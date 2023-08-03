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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.alipay.sofa.jraft.Status;

import edu.cmu.reedsolomonfs.client.Reedsolomonfs.WriteRequest;
import edu.cmu.reedsolomonfs.datatype.FileMetadata;
import edu.cmu.reedsolomonfs.datatype.FileMetadataHelper;
import edu.cmu.reedsolomonfs.server.MasterServiceGrpc;
import edu.cmu.reedsolomonfs.server.MasterserverOutter;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverClosure;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverService;
import edu.cmu.reedsolomonfs.server.ChunkserverOutter.UpdateSecretKeyRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;

public class UpdateSecretKeyProcessor implements RpcProcessor<UpdateSecretKeyRequest> {

    private final ChunkserverService counterService;
    private int appendAt;
    private String writeFlag;
    private String filePath;
    private int fileSize;
    private ManagedChannel masterChannel;

    public UpdateSecretKeyProcessor(ChunkserverService counterService, ManagedChannel masterChannel) {
        super();
        this.counterService = counterService;
        this.masterChannel = masterChannel;
        // redirectSystemOutToFile();
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final UpdateSecretKeyRequest request) {
        System.out.println("UpdateSecretKeyProcessor: handleRequest");
        String secretKey = request.getSecretKey();
        System.out.printf("secretKey: %s \n", secretKey);

        final ChunkserverClosure closure = new ChunkserverClosure() {
            @Override
            public void run(Status status) {
                System.out.printf("UpdateSecretKeyRequest: run \n");
                // redirect the print log to file
                // PrintStream out = null;
                // try {
                // out = new PrintStream(new FileOutputStream("./output.txt"));
                // } catch (FileNotFoundException e) {
                // e.printStackTrace();
                // }
                // System.setOut(out);
                // send success to master
                MasterServiceGrpc.MasterServiceBlockingStub stub = MasterServiceGrpc.newBlockingStub(masterChannel);
                ackMasterWriteSuccessRequest ack = ackMasterWriteSuccessRequest.newBuilder()
                        .setAppendAt(appendAt)
                        .setWriteFlag(writeFlag)
                        .setFileName(filePath)
                        .setFileSize(fileSize).build();

                stub.writeSuccess(ack);

                // send reponse back to the client
                rpcCtx.sendResponse(getValueResponse());

            }
        };

        // WriteFlag can be "create", "append", "overwrite", and "delete"
        // switch (writeFlag) {
        // case "create":
        // FileMetadata metadata = FileMetadataHelper.createFileMetadata(filePath,
        // fileSize);
        // byte[][] shards = new byte[request.getPayloadCount()][];
        // for (int i = 0; i < request.getPayloadCount(); i++)
        // shards[i] = request.getPayload(i).toByteArray();
        // this.counterService.write(shards, metadata, closure);
        // break;
        // case "append":
        // break;
        // }
        this.counterService.updateSecretKey(secretKey, closure);

    }

    public void redirectSystemOutToFile() {
        try {
            // Create a new file output stream for the desired file
            FileOutputStream fileOutputStream = new FileOutputStream("chunkserver_output.log");

            // Create a new print stream that writes to the file output stream
            PrintStream printStream = new PrintStream(fileOutputStream);

            // Redirect System.out to the print stream
            System.setOut(printStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String interest() {
        return UpdateSecretKeyRequest.class.getName();
    }
}
