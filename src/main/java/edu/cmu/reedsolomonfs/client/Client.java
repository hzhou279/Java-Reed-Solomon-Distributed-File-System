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

import edu.cmu.reedsolomonfs.server.MasterServiceGrpc;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.ChunkserverGrpcHelper;
import edu.cmu.reedsolomonfs.server.ChunkserverOutter.ValueResponse;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.InvokeCallback;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InaccessibleObjectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.WriteRequest;
import edu.cmu.reedsolomonfs.datatype.FileMetadata;
import edu.cmu.reedsolomonfs.cli.ClientCLI;
import edu.cmu.reedsolomonfs.ConfigVariables;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.ReadRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.TokenRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.TokenResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {

    static ClientCLI cli = new ClientCLI();
    public ManagedChannel channel;
    public CliClientServiceImpl cliClientService;
    public String groupId;
    public String confStr;

    public Client(final String[] args) throws Exception {
        /**
         * Reference
         * Line 67 to line 72 follows jRaft documentation to set up raft group client
         * https://www.sofastack.tech/projects/sofa-jraft/counter-example/
         */
        if (args.length != 2) {
            System.out.println("Usage : java com.alipay.sofa.jraft.example.counter.CounterClient {groupId} {conf}");
            System.out
                    .println(
                            "Example: java com.alipay.sofa.jraft.example.counter.CounterClient counter 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083");
            System.exit(1);
        }
        groupId = args[0];
        confStr = args[1];
        ChunkserverGrpcHelper.initGRpc();

        final Configuration conf = new Configuration();
        if (!conf.parse(confStr)) {
            throw new IllegalArgumentException("Fail to parse conf:" + confStr);
        }
        System.out.println(groupId + "????");
        RouteTable.getInstance().updateConfiguration(groupId, conf);

        cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());

        if (!RouteTable.getInstance().refreshLeader(cliClientService, groupId,
                1000).isOk()) {
            throw new IllegalStateException("Refresh leader failed");
        }

        final PeerId leader = RouteTable.getInstance().selectLeader(groupId);
        System.out.println("Leader is " + leader + "\n\n");
        System.out.println("RouteTable is " + RouteTable.getInstance() + "\n\n");
        System.out.println("Configuration is " +
                RouteTable.getInstance().getConfiguration(groupId) + "\n\n");

        // Create a channel to connect to the master
        channel = ManagedChannelBuilder.forAddress("master", 8080)
                .usePlaintext() // Use insecure connection, for testing only
                .build();
    }

    public void test(final String[] args) throws Exception {
        // Cache file metadata
        Map<String, FileMetadata> cache = new HashMap<>();

        TokenResponse tResponse = requestToken("read", "/A/B/C");
        System.out.println("Token received: " + tResponse.getToken());

        // Make a create request
        String filePath = "./ClientClusterCommTestFiles/Files/test.txt";
        byte[] fileData = Files.readAllBytes(Path.of(filePath));
        create(cliClientService, "test.txt", fileData, groupId, tResponse.getToken());
        System.out.println(filePath + " created successfully!!!!");
        // // sleep for 3s to wait for the data to be replicated to the follower
        Thread.sleep(3000);

        System.out.println("Going to read the file!!!!");

        byte[] fileDataRead = read(cliClientService, "read", "test.txt", fileData.length, groupId);

        System.out.println("File read successfully!!!!");

        // write fileDataRead to a file
        Files.write(Path.of("./ClientClusterCommTestFiles/Files/testRead.txt"), fileDataRead);

        // check data read correctly
        if (fileDataRead != null && Arrays.equals(fileData, fileDataRead)) {
            System.out.println("[Client-Cluster] Create and Read a new file succeeded!!!!");
        } else {
            System.out.println("Client-Cluster Create and Read a new file failed?????");
        }
        String clientDiskPath = "./ClientClusterCommTestFiles/FilesRead/test.txt";
        try (FileOutputStream fos = new FileOutputStream(clientDiskPath)) {
            fos.write(fileDataRead); // Write the byte data to the file
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Shutdown the channel to the master
        channel.shutdown();

        // Exit the client
        System.exit(0);
    }

    public static byte[] read(final CliClientServiceImpl cliClientService, String operationType, String filePath,
            int fileSize,
            final String groupId) throws RemotingException, InterruptedException {

        if (operationType.equals("read"))
            return readRequest(cliClientService, filePath, fileSize, groupId);
        else
            return null;
    }

    private static byte[] readRequest(final CliClientServiceImpl cliClientService, String filePath,
            int fileSize,
            final String groupId)
            throws RemotingException,
            InterruptedException {
        ReadRequest request = ReadRequest.newBuilder()
                .setOperationType("read")
                .setFilePath(filePath)
                .build();

        // send the request to all the peers in the cluster
        // get the ip addresses of the cluster
        final Configuration conf = RouteTable.getInstance().getConfiguration(groupId);

        int serverCnt = 0;
        int byteCntInShards = 0;
        byte[][] shards = new byte[ConfigVariables.TOTAL_SHARD_COUNT][];
        boolean[] shardsPresent = new boolean[ConfigVariables.TOTAL_SHARD_COUNT];
        
        for (PeerId peer : conf) {

            ValueResponse response = null;
            try {
                // invokeSync and print the result
                response = (ValueResponse) cliClientService.getRpcClient().invokeSync(peer.getEndpoint(),
                        request, 1500);
            } catch (com.alipay.sofa.jraft.error.RemotingException e) {
                // Handle the exception. For now, just print the error.
                System.err.println("Failed to invoke RPC on " + peer.getEndpoint() + ": " + e.getMessage());
                serverCnt++;
                // Skip to the next iteration since this peer failed
                continue;
            }
            // System.out.println("peer:" + peer.getEndpoint());
            // invokeSync and print the result
            // ValueResponse response = (ValueResponse)
            // cliClientService.getRpcClient().invokeSync(peer.getEndpoint(),
            // request, 15000);

            // System.out.println("Chunk Data:" + response.getChunkDataMapMap());
            // System.out.println("Chunk Data Size:" +
            // response.getChunkDataMapMap().size());

            // save the chunk data in a map
            Map<String, ByteString> chunkDataMap = new HashMap<>();
            chunkDataMap.putAll(response.getChunkDataMapMap());

            // get the sorted map key
            Object[] sortedKeys = chunkDataMap.keySet().toArray();
            // sort the map key by the int of substring from last index of ('-')
            Arrays.sort(sortedKeys, (o1, o2) -> {
                int o1Idx = ((String) o1).lastIndexOf('-');
                int o2Idx = ((String) o2).lastIndexOf('-');
                return Integer.valueOf(((String) o1).substring(o1Idx + 1)).compareTo(
                        Integer.valueOf(((String) o2).substring(o2Idx + 1)));
            });

            // concate the sorted map value by key to byte[]
            byte[] shardBytes = new byte[0];
            for (Object key : sortedKeys) {
                // System.out.println("key:" + key);
                shardBytes = Bytes.concat(shardBytes, chunkDataMap.get(key).toByteArray());
            }

            System.out.println("shardBytes Size:" + shardBytes.length);

            if (response.getChunkDataMapMap() != null && response.getChunkDataMapMap().size() != 0) {
                shardsPresent[serverCnt] = true;
                shards[serverCnt] = shardBytes;
                byteCntInShards = shards[serverCnt].length;
            }
            serverCnt++;
        }
        if (byteCntInShards == 0) {
            System.out.println("File does not exist");
            return null;
        }
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
            if (shards[i] == null)
                shards[i] = new byte[byteCntInShards];
        }

        ReedSolomonDecoder decoder = new ReedSolomonDecoder(shards, shardsPresent, byteCntInShards, fileSize);
        return decoder.getFileData();
    }

    public TokenResponse requestToken(String requestType, String filePath) {
        // Create a stub for the service
        // System.out.println("Creating stub for master service");
        // print out channel
        // System.out.println("Channel is " + channel);
        MasterServiceGrpc.MasterServiceBlockingStub stub = MasterServiceGrpc.newBlockingStub(channel);

        // System.out.println("stub is " + stub);
        // System.out.println("Requesting JWT token from master for " + requestType + "
        // operation on file " + filePath);
        TokenRequest request = TokenRequest.newBuilder()
                .setRequestType(requestType)
                .setFilePath(filePath)
                .build();

        // System.out.println("Sending request to master for JWT token request" +
        // request);
        // Make the RPC call and receive the response
        TokenResponse response = stub.getToken(request);

        // System.out.println("JWT token received at client is: " +
        // response.getToken());

        return response;
    }

    public void delete(final CliClientServiceImpl cliClientService, String filePath,
            final String groupId, String token) throws RemotingException, InterruptedException {
        final int n = 10000;
        final CountDownLatch latch = new CountDownLatch(n);

        // System.out.println("Client delete: " + filePath);
        WriteRequest request = packWriteRequest("delete", filePath, -1, 0, null, "delete",
                -1, -1, token);
        final PeerId leader = RouteTable.getInstance().selectLeader(groupId);
        writeRequest(cliClientService, leader, request, latch);
    }

    public static void overwrite() {

    }

    public static void append() {

    }

    public void create(final CliClientServiceImpl cliClientService, String filePath, byte[] fileData,
            final String groupId, String token) throws RemotingException, InterruptedException {
        final int n = 10000;
        final CountDownLatch latch = new CountDownLatch(n);

        ReedSolomonEncoder encoder = new ReedSolomonEncoder(fileData);
        encoder.encode();
        byte[][] shards = encoder.getShards();

        // Pass padded file size
        // System.out.println("Client create: " + filePath);
        WriteRequest request = packWriteRequest("touch", filePath, encoder.getPaddedFileSize(), 0, shards, "create",
                encoder.getLastChunkIdx(), encoder.getFileSize(), token);
        final PeerId leader = RouteTable.getInstance().selectLeader(groupId);
        writeRequest(cliClientService, leader, request, latch);
    }

    private static WriteRequest packWriteRequest(String operationType, String filePath, int fileSize, int appendAt,
            byte[][] shards, String writeFlag, int lastChunkIdx, int originalFileSize, String token) {
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();

        if (writeFlag.equals("create")) {
            for (byte[] shard : shards) {
                requestBuilder.addPayload(ByteString.copyFrom(shard));
            }

            requestBuilder.setOperationType(operationType);
            // System.out.println("Client packWriteRequest: " + filePath);
            requestBuilder.setFilePath(filePath);
            requestBuilder.setFileSize(fileSize);
            requestBuilder.setAppendAt(appendAt);
            requestBuilder.setWriteFlag(writeFlag);
            requestBuilder.setLastChunkIdx(lastChunkIdx);
            requestBuilder.setOriginalFileSize(originalFileSize);
            requestBuilder.setToken(token);

        } else if (writeFlag.equals("delete")) {
            requestBuilder.setWriteFlag(writeFlag);
            requestBuilder.setFilePath(filePath);
            requestBuilder.setToken(token);
        }
        return requestBuilder.build();
    }

    private static void writeRequest(final CliClientServiceImpl cliClientService, final PeerId leader,
            WriteRequest request, CountDownLatch latch) throws RemotingException,
            InterruptedException {

        try {
            // System.out.println("write request to leader:" + leader);
            cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, new InvokeCallback() {

                @Override
                public void complete(Object result, Throwable err) {
                    if (err == null) {
                        latch.countDown();
                        // System.out.println("write request result:" + result);
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
        } catch (InaccessibleObjectException e) {
            System.err.println("Caught InaccessibleObjectException: " + e.getMessage());
        }
    }
}

