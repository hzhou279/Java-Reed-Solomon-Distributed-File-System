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
package edu.cmu.reedsolomonfs.server.Chunkserver.rpc;

import com.alipay.sofa.jraft.Status;

import edu.cmu.reedsolomonfs.client.Reedsolomonfs.ReadRequest;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverClosure;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverService;

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;

/**
 * GetBytesValueRequest processor.
 *
 * @author boyan (boyan@alibaba-inc.com)
 *
 *         2018-Apr-09 5:48:33 PM
 */
public class ReadRequestProcessor implements RpcProcessor<ReadRequest> {

    private final ChunkserverService counterService;

    public ReadRequestProcessor(ChunkserverService counterService) {
        super();
        this.counterService = counterService;
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final ReadRequest request) {
        // log to see if the request is handled in the same thread
        System.out.println("ReadRequestProcessor thread: " + Thread.currentThread().getName());

        final ChunkserverClosure closure = new ChunkserverClosure() {
            @Override
            public void run(Status status) {
                rpcCtx.sendResponse(getValueResponse());
            }
        };
        // log to see if the request is handled in the same thread
        System.out.println("ReadRequestProcessor thread2: " + Thread.currentThread().getName());
        String filePath = request.getFilePath();
        this.counterService.read(filePath, closure);
    }

    @Override
    public String interest() {
        return ReadRequest.class.getName();
    }
}
