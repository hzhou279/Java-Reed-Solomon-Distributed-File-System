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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.closure.ReadIndexClosure;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.rhea.StoreEngineHelper;
import com.alipay.sofa.jraft.rhea.options.StoreEngineOptions;
import com.alipay.sofa.jraft.util.BytesUtil;

import edu.cmu.reedsolomonfs.datatype.FileMetadata;

// The ChunkserverServiceImpl is referenced from
// https://github.com/sofastack/sofa-jraft/blob/19ed179e02ee9108adc0bbf66badb47f62c62af8/jraft-example/src/main/java/com/alipay/sofa/jraft/example/counter/CounterServiceImpl.java
public class ChunkserverServiceImpl implements ChunkserverService {
    private static final Logger LOG = LoggerFactory.getLogger(ChunkserverServiceImpl.class);

    private final Chunkserver counterServer;
    private final Executor      readIndexExecutor;

    public ChunkserverServiceImpl(Chunkserver counterServer) {
        this.counterServer = counterServer;
        this.readIndexExecutor = createReadIndexExecutor();
    }

    private Executor createReadIndexExecutor() {
        final StoreEngineOptions opts = new StoreEngineOptions();
        return StoreEngineHelper.createReadIndexExecutor(opts.getReadIndexCoreThreads());
    }

    @Override
    public void get(final boolean readOnlySafe, final ChunkserverClosure closure) {
        if(!readOnlySafe){
            closure.success(getValue());
            closure.run(Status.OK());
            return;
        }

        this.counterServer.getNode().readIndex(BytesUtil.EMPTY_BYTES, new ReadIndexClosure() {
            @Override
            public void run(Status status, long index, byte[] reqCtx) {
                if(status.isOk()){
                    closure.success(getValue());
                    closure.run(Status.OK());
                    return;
                }
                ChunkserverServiceImpl.this.readIndexExecutor.execute(() -> {
                    if(isLeader()){
                        LOG.debug("Fail to get value with 'ReadIndex': {}, try to applying to the state machine.", status);
                        applyOperation(ChunkserverOperation.createGet(), closure);
                    }else {
                        handlerNotLeaderError(closure);
                    }
                });
            }
        });
    }

    private boolean isLeader() {
        return this.counterServer.getFsm().isLeader();
    }

    private long getValue() {
        return this.counterServer.getFsm().getValue();
    }

    private String getRedirect() {
        return this.counterServer.redirect().getRedirect();
    }

    @Override
    public void incrementAndGet(final long delta, final ChunkserverClosure closure) {
        applyOperation(ChunkserverOperation.createIncrement(delta), closure);
    }

    @Override
    public void setBytesValue(final byte[] value, final ChunkserverClosure closure) {
        applyOperation(ChunkserverOperation.createSetBytesValue(value), closure);
    }

    @Override
    public void write(final byte[][] shards, final FileMetadata metadata, final ChunkserverClosure closure) {
        applyOperation(ChunkserverOperation.createWrite(shards, metadata), closure);
    }

    @Override
    public void delete(final String filePath, final ChunkserverClosure closure) {
        applyOperation(ChunkserverOperation.createDelete(filePath), closure);
    }

    @Override
    public void read(String filePath, final ChunkserverClosure closure) {
        System.out.println("read filePath: " + filePath);
        Map<String, byte[]> fileData = this.counterServer.getFsm().readFromServerDisk(filePath);
        System.out.println("fileData: " + fileData);
        closure.successWithRead(fileData);
        closure.run(Status.OK());
        System.out.println("read filePath: " + filePath + "completes");
        return;
    }

    @Override
    public void updateSecretKey(final String secretKey, final ChunkserverClosure closure) {
        System.out.println("updateSecretKey: ChunkserverServiceImpl " + secretKey);
        applyOperation(ChunkserverOperation.updateSecretKey(secretKey), closure);
    }

    private void applyOperation(final ChunkserverOperation op, final ChunkserverClosure closure) {
        if (!isLeader()) {
            System.out.print("Not leader.");
            handlerNotLeaderError(closure);
            return;
        }

        try {
            closure.setCounterOperation(op);
            final Task task = new Task();
            task.setData(ByteBuffer.wrap(SerializerManager.getSerializer(SerializerManager.Hessian2).serialize(op)));
            task.setDone(closure);
            this.counterServer.getNode().apply(task);
        } catch (CodecException e) {
            System.out.println("CodecException");
            String errorMsg = "Fail to encode CounterOperation";
            LOG.error(errorMsg, e);
            closure.failure(errorMsg, StringUtils.EMPTY);
            closure.run(new Status(RaftError.EINTERNAL, errorMsg));
        }
    }

    private void handlerNotLeaderError(final ChunkserverClosure closure) {
        closure.failure("Not leader.", getRedirect());
        closure.run(new Status(RaftError.EPERM, "Not leader"));
    }
}
