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
package edu.cmu.reedsolomonfs.server.Chunkserver;

import static edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverOperation.GET;
import static edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverOperation.INCREMENT;
import static edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverOperation.READ_BYTES;
import static edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverOperation.WRITE_BYTES;
import static edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverOperation.DELETE_BYTES;
import static edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverOperation.UPDATE_SECRETKEY;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.alipay.sofa.jraft.util.NamedThreadFactory;
import com.alipay.sofa.jraft.util.ThreadPoolUtil;

import edu.cmu.reedsolomonfs.datatype.FileMetadata;
import edu.cmu.reedsolomonfs.datatype.FileMetadataHelper;
import edu.cmu.reedsolomonfs.datatype.NodeHelper;

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


// The ChunkserverStateMachine class is referenced from 
// https://github.com/sofastack/sofa-jraft/blob/19ed179e02ee9108adc0bbf66badb47f62c62af8/jraft-example/src/main/java/com/alipay/sofa/jraft/example/counter/CounterStateMachine.java
public class ChunkserverStateMachine extends StateMachineAdapter {

    private final int serverIdx;
    private final String serverDiskPath;
    // Create Map of stored chunks in memory: filename -> list of chunk filepaths
    private Map<String, List<String>> storedFileNameToChunks;
    private String secretKey;

    public ChunkserverStateMachine(int serverIdx) {
        secretKey = "secretKey";
        this.serverIdx = serverIdx;
        serverDiskPath = "./ClientClusterCommTestFiles/Disks/chunkserver-" + serverIdx + "/";
        storedFileNameToChunks = new HashMap<String, List<String>>();
        System.out.println("ChunkserverStateMachine created for serverIdx " + serverIdx);
        System.out.println("serverDiskPath is " + serverDiskPath);
        // iterate through all files in serverDiskPath
        // TBD: the file directory data structure can be improved
        Path start = Paths.get(serverDiskPath);
        try (Stream<Path> stream = Files.walk(start)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String fileNameWithSubDir = start.relativize(path).toString();
                int separatorIndex = fileNameWithSubDir.lastIndexOf('.');
                if (separatorIndex > 0) {
                    String originalFileName = fileNameWithSubDir.substring(0, separatorIndex);
                    System.out.println("originalFileName is " + originalFileName);
                    System.out.println("fileNameWithSubDir is " + fileNameWithSubDir);
                    storedFileNameToChunks.computeIfAbsent("/" + originalFileName, k -> new ArrayList<>())
                            .add(fileNameWithSubDir);
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("server " + serverIdx + " initialize printStoredFileNameToChunks:");
        printStoredFileNameToChunks();
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

    public void updateStoredFileNameToChunks(String fileName) {
        System.out.println("server " + serverIdx + " updateStoredFileNameToChunks:");
        System.out.println("updateStoredFileNameToChunks with fileName " + fileName);
        // update storedFileNameToChunks map
        int separatorIndex = fileName.lastIndexOf('.');
        if (separatorIndex > 0) {
            String originalFileName = fileName.substring(0, separatorIndex);
            storedFileNameToChunks.computeIfAbsent(originalFileName, k -> new ArrayList<>()).add(fileName);
        }
        System.out.println("storedFileNameToChunks size is " + storedFileNameToChunks.size());
        for (Map.Entry<String, List<String>> entry : storedFileNameToChunks.entrySet()) {
            System.out.println("File: " + entry.getKey() + ", Chunks: " + entry.getValue());
        }
    }

    private void printStoredFileNameToChunks() {
        System.out.println("server " + serverIdx + " storedFileNameToChunks:");
        System.out.println("storedFileNameToChunks size is " + storedFileNameToChunks.size());
        for (Map.Entry<String, List<String>> entry : storedFileNameToChunks.entrySet()) {
            System.out.println("File: " + entry.getKey() + ", Chunks: " + entry.getValue());
        }
    }

    public Map<String, List<String>> getStoredFileNameToChunks() {
        return storedFileNameToChunks;
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

    public Map<String, byte[]> readFromServerDisk(String filePath) {
        System.out.println("readFromServerDisk line 181");
        // get chunks file path from storedFileNameToChunks
        List<String> chunkFilePaths = storedFileNameToChunks.get(filePath);
        if (chunkFilePaths == null) {
            System.out.println("chunk file paths does not exist.");
            return new HashMap<String, byte[]>();
        }    
        // read chunks from disk
        Map<String, byte[]> chunks = new HashMap<String, byte[]>();
        for (int i = 0; i < chunkFilePaths.size(); i++) {
            String chunkFilePath = serverDiskPath + chunkFilePaths.get(i);
            try {
                System.out.println("To read " + chunkFilePath);
                byte[] chunk = Files.readAllBytes(Paths.get(chunkFilePath));
                chunks.put(chunkFilePaths.get(i), chunk);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("readFromServerDisk line 198");
        return chunks;
    }

    public void deleteFile(String filePath) {
        // get chunks file path from storedFileNameToChunks
        System.out.println("filePath is " + filePath);
        // print out storedFileNameToChunks map
        for (Map.Entry<String, List<String>> entry : storedFileNameToChunks.entrySet()) {
            System.out.println("File: " + entry.getKey() + ", Chunks: " + entry.getValue());
        }
        System.out.println("reach delete file 209");
        List<String> chunkFilePaths = storedFileNameToChunks.get(filePath);
        if (chunkFilePaths == null) {
            System.out.println("chunk file paths to delete does not exist.");
            return;
        }
        // delete filepath from storedFileNameToChunks
        storedFileNameToChunks.remove(filePath);
        for (int i = 0; i < chunkFilePaths.size(); i++) {
            String chunkFilePath = serverDiskPath + chunkFilePaths.get(i);
            try {
                System.out.println("To delete " + chunkFilePath);
                Path path = Paths.get(chunkFilePath);
                // Delete the file using Files.delete() method
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("deleteFromServerDisk line 227");
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
            // print out closure
            if (closure != null) {
                LOG.info("closure={} at logIndex={}", closure, iter.getIndex());
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
                    case UPDATE_SECRETKEY:
                        secretKey = counterOperation.getFilePath();
                        System.out.println("secretKey is updated in state machine " + secretKey);
                        break;
                    case WRITE_BYTES:
                        System.out.println("Enter write byte: ");
                        final byte[][] shards = counterOperation.getShards();
                        final FileMetadata metadata = counterOperation.getMetadata();
                        final byte[][] chunks = NodeHelper.splitShardToChunks(shards[serverIdx]);
                        List<String> chunkFilePaths = FileMetadataHelper.retrieveFileChunkPaths(metadata, serverIdx);
                        for (String s : chunkFilePaths)
                            System.out.println("chunkFIlePath: " + s);
                        Path directory = Paths.get(serverDiskPath);
                        try {
                            Files.createDirectories(directory); // Create the directory and any nonexistent parent
                                                                // directories
                        } catch (IOException e) {
                            System.out.println("Failed to create the directory: " + e.getMessage());
                        }

                        for (int i = 0; i < chunks.length; i++) {
                            String chunkFilePath = serverDiskPath + chunkFilePaths.get(i);
                            // Create parent directories if they do not exist
                            File file = new File(chunkFilePath);
                            File parentDirectory = file.getParentFile();
                            System.out.println("parentDirectory is " + parentDirectory);
                            if (!parentDirectory.exists()) {
                                parentDirectory.mkdirs();
                            }
                            try (FileOutputStream fos = new FileOutputStream(chunkFilePath)) {
                                fos.write(chunks[i]); // Write the byte data to the file
                                String fileNameWithSubDir = chunkFilePaths.get(i);
                                updateStoredFileNameToChunks(fileNameWithSubDir);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case READ_BYTES:
                        final Byte[] byteValue2 = this.byteValue;
                        LOG.info("Get byte value={} at logIndex={}", byteValue2, iter.getIndex());
                        break;
                    case DELETE_BYTES:
                        deleteFile(counterOperation.getFilePath());
                        break;
                }

                if (closure != null) {
                    closure.success(current);
                    closure.run(Status.OK());
                    System.out.println("Write Success");
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

    public String getSecretKey() {
        return secretKey;
    }

}
