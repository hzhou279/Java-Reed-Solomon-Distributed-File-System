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
import java.nio.charset.StandardCharsets;
import com.alipay.sofa.jraft.Status;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.WriteRequest;
import edu.cmu.reedsolomonfs.datatype.FileMetadata;
import edu.cmu.reedsolomonfs.datatype.FileMetadataHelper;
import edu.cmu.reedsolomonfs.server.MasterServiceGrpc;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverClosure;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverService;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverStateMachine;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequest;
import io.grpc.ManagedChannel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;

/**
 * @author Matt (chenweiy@andrew.cmu.edu) and Tommy (hungchic@andrew.cmu.edu)
 */
public class WriteRequestProcessor implements RpcProcessor<WriteRequest> {

    private final ChunkserverService counterService;
    private int appendAt;
    private String writeFlag;
    private String filePath;
    private int fileSize;
    private ManagedChannel masterChannel;
    private ChunkserverStateMachine fsm;

    public WriteRequestProcessor(ChunkserverService counterService, ManagedChannel masterChannel, 
            ChunkserverStateMachine fsm) {
        super();
        this.counterService = counterService;
        this.masterChannel = masterChannel;
        this.fsm = fsm;
    }

    public boolean validateJWT(String token) {
        try {
            System.out.println("fsm.getSecretKey(): " + fsm.getSecretKey());
            byte[] secretKeyBytes = fsm.getSecretKey().getBytes(StandardCharsets.UTF_8);
            System.out.println("secretKeyBytes: " + secretKeyBytes);
            // Parse the JWT token
            Jws<Claims> jwsClaims = Jwts.parser()
                    .setSigningKey(secretKeyBytes)
                    .parseClaimsJws(token);
            System.out.println("jwsClaims: " + jwsClaims);

            // Check the permission and filePath in the token (optional)
            Claims claims = jwsClaims.getBody();
            System.out.println("claims: " + claims);
            String permission = claims.get("permission", String.class);
            String filePath = claims.get("filePath", String.class);

            // If we reach this point, the token was valid
            return true;
        } catch (JwtException ex) {
            // An error occurred while validating the JWT token
            System.out.println("Invalid JWT token: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final WriteRequest request) {

        String token = request.getToken();
        System.out.println("token: " + token);
        if (!validateJWT(token)) {
            System.out.println("Invalid JWT token");
            return;
        }
        writeFlag = request.getWriteFlag();
        appendAt = request.getAppendAt();
        filePath = request.getFilePath();
        fileSize = request.getFileSize();
        System.out.printf("writeFlag: %s \n", writeFlag);
        System.out.printf("appendAt: %d \n", appendAt);
        System.out.printf("filePath: %s \n", filePath);
        System.out.printf("fileSize: %d \n", fileSize);

        final ChunkserverClosure closure = new ChunkserverClosure() {
            @Override
            public void run(Status status) {
                System.out.printf("WriteRequestProcessor: run \n");
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

        if (writeFlag.equals("create")) {
            FileMetadata metadata = FileMetadataHelper.createFileMetadata(filePath, fileSize);
            byte[][] shards = new byte[request.getPayloadCount()][];
            for (int i = 0; i < request.getPayloadCount(); i++)
                shards[i] = request.getPayload(i).toByteArray();
            this.counterService.write(shards, metadata, closure);
        } else if (writeFlag.equals("delete")) {
            this.counterService.delete(filePath, closure);
        }

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
        return WriteRequest.class.getName();
    }
}
