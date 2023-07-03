/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.cmu.reedsolomonfs.server;

import static edu.cmu.reedsolomonfs.server.ChunkserverOperation.GET;
import static edu.cmu.reedsolomonfs.server.ChunkserverOperation.INCREMENT;
import static edu.cmu.reedsolomonfs.server.ChunkserverOperation.READ_BYTES;
import static edu.cmu.reedsolomonfs.server.ChunkserverOperation.WRITE_BYTES;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import com.alipay.sofa.jraft.util.NamedThreadFactory;
import com.alipay.sofa.jraft.util.ThreadPoolUtil;

import edu.cmu.reedsolomonfs.datatype.FileMetadata;
import edu.cmu.reedsolomonfs.datatype.FileMetadataHelper;
import edu.cmu.reedsolomonfs.datatype.NodeHelper;
import edu.cmu.reedsolomonfs.server.snapshot.ChunkserverSnapshotFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;

/**
 * Counter state machine.
 *
 * @author boyan (boyan@alibaba-inc.com)
 *
 *         2018-Apr-09 4:52:31 PM
 */
public class ChunkserverStateMachine extends StateMachineAdapter {

    private final int serverIdx;
    private final String serverDiskPath;

    public ChunkserverStateMachine(int serverIdx) {
        this.serverIdx = serverIdx;
        serverDiskPath = "./ClientClusterCommTestFiles/Disks/chunkserver-" + serverIdx + "/";
    }

    private static final Logger LOG = LoggerFactory.getLogger(ChunkserverStateMachine.class);
    private static ThreadPoolExecutor executor = ThreadPoolUtil
            .newBuilder()
            .poolName("JRAFT_TEST_EXECUTOR")
            .enableMetric(true)
            .coreThreads(3)
            .maximumThreads(5)
            .keepAliveSeconds(60L)
            .workQueue(new SynchronousQueue<>())
            .threadFactory(
                    new NamedThreadFactory("JRaft-Test-Executor-", true))
            .build();
    /**
     * Counter value
     */
    private final AtomicLong value = new AtomicLong(0);
    /**
     * Byte Array Value
     */
    private final Byte[] byteValue = new Byte[100];
    /**
     * Leader term
     */
    private final AtomicLong leaderTerm = new AtomicLong(-1);

    public int getServerIdx() {
        return serverIdx;
    }

    public boolean isLeader() {
        return this.leaderTerm.get() > 0;
    }

    /**
     * Returns current value.
     */
    public long getValue() {
        return this.value.get();
    }

    /**
     * Returns byte value.
     */
    public Byte[] getByteValue() {
        return this.byteValue;
    }

    public byte[] readFromServerDisk() {
        byte[] fileData = new byte[0];
        File file = new File(serverDiskPath);
        if (file.exists())
            try {
                fileData = Files.readAllBytes(Path.of(serverDiskPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        System.out.println("read from disk " + serverDiskPath);
        return fileData;
    }

    @Override
    public void onApply(final Iterator iter) {
        while (iter.hasNext()) {
            long current = 0;
            ChunkserverOperation counterOperation = null;

            ChunkserverClosure closure = null;
            if (iter.done() != null) {
                // This task is applied by this node, get value from closure to avoid additional
                // parsing.
                closure = (ChunkserverClosure) iter.done();
                counterOperation = closure.getCounterOperation();
            } else {
                // Have to parse FetchAddRequest from this user log.
                final ByteBuffer data = iter.getData();
                try {
                    counterOperation = SerializerManager.getSerializer(SerializerManager.Hessian2).deserialize(
                            data.array(), ChunkserverOperation.class.getName());
                } catch (final CodecException e) {
                    LOG.error("Fail to decode IncrementAndGetRequest", e);
                }
                // follower ignore read operation
                if (counterOperation != null && counterOperation.isReadOp()) {
                    iter.next();
                    continue;
                }
            }
            if (counterOperation != null) {
                switch (counterOperation.getOp()) {
                    case GET:
                        current = this.value.get();
                        LOG.info("Get value={} at logIndex={}", current, iter.getIndex());
                        break;
                    case INCREMENT:
                        final long delta = counterOperation.getDelta();
                        final long prev = this.value.get();
                        current = this.value.addAndGet(delta);
                        LOG.info("Added value={} by delta={} at logIndex={}", prev, delta, iter.getIndex());
                        break;
                    case WRITE_BYTES:
                        final byte[][] shards = counterOperation.getShards();
                        final FileMetadata metadata = counterOperation.getMetadata();
                        final byte[][] chunks = NodeHelper.splitShardToChunks(shards[serverIdx]);
                        List<String> chunkFilePaths = FileMetadataHelper.retrieveFileChunkPaths(metadata, serverIdx);

                        Path directory = Paths.get(serverDiskPath);
                        try {
                            Files.createDirectories(directory); // Create the directory and any nonexistent parent directories
                        } catch (IOException e) {
                            System.out.println("Failed to create the directory: " + e.getMessage());
                        }

                        for (int i = 0; i < chunks.length; i++) {
                            String chunkFilePath = serverDiskPath + chunkFilePaths.get(i);
                            try (FileOutputStream fos = new FileOutputStream(chunkFilePath)) {
                                fos.write(chunks[i]); // Write the byte data to the file
                                System.out.println("Byte data to store is " + new String(chunks[i]));
                                System.out.println("Byte data stored in " + chunkFilePath + " successfully.");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // final byte[][] shards = counterOperation.getShards();
                        // try (FileOutputStream fos = new FileOutputStream(serverDiskPath)) {
                        // fos.write(shards[serverIdx]); // Write the byte data to the file
                        // System.out.println("Byte data to store is " + new String(shards[serverIdx]));
                        // System.out.println("Byte data stored in " + serverDiskPath + "
                        // successfully.");
                        // } catch (IOException e) {
                        // e.printStackTrace();
                        // }

                        // for (int i = 0; i < byteValue.length; i++) {
                        // this.byteValue[i] = byteValue[i];
                        // }
                        // LOG.info("Set byte value={} length={} at logIndex={}", byteValue,
                        // byteValue.length,
                        // iter.getIndex());
                        // LOG.info("Get byte value={} length={} at logIndex={}",
                        // this.byteValue[0].toString(),
                        // this.byteValue.length, iter.getIndex());
                        // log this.byteValue using for loop
                        // for (int i = 0; i < this.byteValue.length; i++) {
                        // LOG.info("Get byte value={} at logIndex={}", this.byteValue[i].toString(),
                        // iter.getIndex());
                        // }
                        break;
                    case READ_BYTES:
                        final Byte[] byteValue2 = this.byteValue;
                        LOG.info("Get byte value={} at logIndex={}", byteValue2, iter.getIndex());
                        break;
                }

                if (closure != null) {
                    closure.success(current);
                    closure.run(Status.OK());
                }
            }
            iter.next();
        }
    }

    @Override
    public void onSnapshotSave(final SnapshotWriter writer, final Closure done) {
        final long currVal = this.value.get();
        executor.submit(() -> {
            final ChunkserverSnapshotFile snapshot = new ChunkserverSnapshotFile(
                    writer.getPath() + File.separator + "data");
            if (snapshot.save(currVal)) {
                if (writer.addFile("data")) {
                    done.run(Status.OK());
                } else {
                    done.run(new Status(RaftError.EIO, "Fail to add file to writer"));
                }
            } else {
                done.run(new Status(RaftError.EIO, "Fail to save counter snapshot %s", snapshot.getPath()));
            }
        });
    }

    @Override
    public void onError(final RaftException e) {
        LOG.error("Raft error: {}", e, e);
    }

    @Override
    public boolean onSnapshotLoad(final SnapshotReader reader) {
        if (isLeader()) {
            LOG.warn("Leader is not supposed to load snapshot");
            return false;
        }
        if (reader.getFileMeta("data") == null) {
            LOG.error("Fail to find data file in {}", reader.getPath());
            return false;
        }
        final ChunkserverSnapshotFile snapshot = new ChunkserverSnapshotFile(
                reader.getPath() + File.separator + "data");
        try {
            this.value.set(snapshot.load());
            return true;
        } catch (final IOException e) {
            LOG.error("Fail to load snapshot from {}", snapshot.getPath());
            return false;
        }

    }

    @Override
    public void onLeaderStart(final long term) {
        this.leaderTerm.set(term);
        super.onLeaderStart(term);

    }

    @Override
    public void onLeaderStop(final Status status) {
        this.leaderTerm.set(-1);
        super.onLeaderStop(status);
    }

}
