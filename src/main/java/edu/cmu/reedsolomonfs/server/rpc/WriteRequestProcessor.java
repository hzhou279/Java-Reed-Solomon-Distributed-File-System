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
package edu.cmu.reedsolomonfs.server.rpc;

import com.alipay.sofa.jraft.Status;

import edu.cmu.reedsolomonfs.client.Reedsolomonfs.WriteRequest;
import edu.cmu.reedsolomonfs.server.ChunkserverOutter.SetBytesRequest;
import edu.cmu.reedsolomonfs.server.ChunkserverClosure;
import edu.cmu.reedsolomonfs.server.ChunkserverService;

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;

/**
 * SetBytesValueRequest processor.
 *
 * @author boyan (boyan@alibaba-inc.com)
 *
 * 2018-Apr-09 5:43:57 PM
 */
public class WriteRequestProcessor implements RpcProcessor<WriteRequest> {

    private final ChunkserverService counterService;

    public WriteRequestProcessor(ChunkserverService counterService) {
        super();
        this.counterService = counterService;
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final WriteRequest request) {
        final ChunkserverClosure closure = new ChunkserverClosure() {
            @Override
            public void run(Status status) {
                rpcCtx.sendResponse(getValueResponse());
            }
        };

        byte[][] shards = new byte[request.getPayloadCount()][];
        for (int i = 0; i < request.getPayloadCount(); i++)
            shards[i] = request.getPayload(i).toByteArray();
        this.counterService.write(shards, closure);
    }

    @Override
    public String interest() {
        return WriteRequest.class.getName();
    }
}
