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
package edu.cmu.reedsolomonfs.client;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RemotingException;
import edu.cmu.reedsolomonfs.server.ChunkserverOutter.IncrementAndGetRequest;
import edu.cmu.reedsolomonfs.server.ChunkserverOutter.SetBytesRequest;
import edu.cmu.reedsolomonfs.server.rpc.ChunkserverGrpcHelper;

import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.InvokeCallback;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.google.protobuf.ByteString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import edu.cmu.reedsolomonfs.client.Reedsolomonfs.WriteRequest;

public class Client {

    public static void main(final String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage : java com.alipay.sofa.jraft.example.counter.CounterClient {groupId} {conf}");
            System.out
                    .println(
                            "Example: java com.alipay.sofa.jraft.example.counter.CounterClient counter 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083");
            System.exit(1);
        }
        final String groupId = args[0];
        final String confStr = args[1];
        ChunkserverGrpcHelper.initGRpc();

        final Configuration conf = new Configuration();
        if (!conf.parse(confStr)) {
            throw new IllegalArgumentException("Fail to parse conf:" + confStr);
        }

        RouteTable.getInstance().updateConfiguration(groupId, conf);

        final CliClientServiceImpl cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());

        if (!RouteTable.getInstance().refreshLeader(cliClientService, groupId, 1000).isOk()) {
            throw new IllegalStateException("Refresh leader failed");
        }

        final PeerId leader = RouteTable.getInstance().selectLeader(groupId);
        System.out.println("Leader is " + leader + "\n\n");
        System.out.println("RouteTable is " + RouteTable.getInstance() + "\n\n");
        System.out.println("Configuration is " + RouteTable.getInstance().getConfiguration(groupId) + "\n\n");
        final int n = 1000;
        final CountDownLatch latch = new CountDownLatch(n);
        final long start = System.currentTimeMillis();
        // for (int i = 0; i < n; i++) {
        // incrementAndGet(cliClientService, leader, i, latch);
        // }
        // setBytesValue(cliClientService, leader, "hello".getBytes(), latch);

        String filePath = "./Files/test.txt";
        byte[] fileData = Files.readAllBytes(Path.of(filePath));

        ReedSolomonEncoder encoder = new ReedSolomonEncoder(fileData);
        encoder.encode();
        byte[][] shards = encoder.getShards();

        WriteRequest request = packWriteRequest("touch", "./Files/test.txt", encoder.getFileSize(), 0, shards, "create",
                encoder.getLastChunkIdx());
        writeRequest(cliClientService, leader, request, latch);
        latch.await();
        System.out.println(n + " ops, cost : " + (System.currentTimeMillis() - start) + " mssssssss.");
        System.exit(0);
    }

    private static WriteRequest packWriteRequest(String operationType, String filePath, int fileSize, int appendAt,
            byte[][] shards, String writeFlag, int lastChunkIdx) {
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();

        for (byte[] shard : shards) {
            // System.out.println("current shard is: " + new String(shard));
            requestBuilder.addPayload(ByteString.copyFrom(shard));
        }

        requestBuilder.setOperationType(operationType);
        requestBuilder.setFilePath(filePath);
        requestBuilder.setFileSize(fileSize);
        requestBuilder.setAppendAt(appendAt);
        requestBuilder.setWriteFlag(writeFlag);
        requestBuilder.setLastChunkIdx(lastChunkIdx);

        return requestBuilder.build();
    }

    private static void writeRequest(final CliClientServiceImpl cliClientService, final PeerId leader,
            WriteRequest request, CountDownLatch latch) throws RemotingException,
            InterruptedException {

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, new InvokeCallback() {

            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    latch.countDown();
                    System.out.println("write request result:" + result);
                } else {
                    err.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        }, 5000);
    }

    private static void setBytesValue(final CliClientServiceImpl cliClientService, final PeerId leader,
            final byte[] bytes, CountDownLatch latch) throws RemotingException,
            InterruptedException {
        SetBytesRequest request = SetBytesRequest.newBuilder().setValue(com.google.protobuf.ByteString.copyFrom(bytes))
                .build();

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, new InvokeCallback() {

            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    latch.countDown();
                    System.out.println("setBytesValue result:" + result);
                } else {
                    err.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        }, 5000);
    }

}


// package edu.cmu.reedsolomonfs.client;

// import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenRequest;
// import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenResponse;
// import io.grpc.ManagedChannel;
// import io.grpc.ManagedChannelBuilder;

// public class Client {
//     public static void main(String[] args) {
//         // Create a channel to connect to the server
//         ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
//                 .usePlaintext() // Use insecure connection, for testing only
//                 .build();

//         // Create a stub for the service
//         ClientMasterServiceGrpc.ClientMasterServiceBlockingStub stub = ClientMasterServiceGrpc.newBlockingStub(channel);

//         TokenRequest request = TokenRequest.newBuilder()
//                 .setRequestType("R")
//                 .setFilePath("./Files/test.txt")
//                 .build();

//         // Make the RPC call and receive the response
//         TokenResponse response = stub.getToken(request);

//         System.out.println("JWT token received at client is: " + response.getToken());

//         // Shutdown the channel
//         channel.shutdown();
//     }
// }

