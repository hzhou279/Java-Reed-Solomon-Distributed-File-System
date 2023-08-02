package edu.cmu.reedsolomonfs.server.Master;

import edu.cmu.reedsolomonfs.ConfigVariables;
import edu.cmu.reedsolomonfs.client.ReedSolomonEncoder;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.WriteRequest;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverDiskRecoveryMachine;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatRequest.ChunkFileNames;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequestResponse;
import edu.cmu.reedsolomonfs.server.ChunkserverOutter.UpdateSecretKeyRequest;
import edu.cmu.reedsolomonfs.datatype.Node;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.InvokeCallback;

import io.grpc.stub.StreamObserver;

import java.io.FileInputStream;
import com.alipay.sofa.jraft.conf.Configuration;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InaccessibleObjectException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.impl.LogKitLogger;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.google.common.collect.Sets;
// import io.grpc.ManagedChannel;
// import io.grpc.ManagedChannelBuilder;
// import edu.cmu.reedsolomonfs.server.RecoveryServiceGrpc;
// import java.util.concurrent.CountDownLatch;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import edu.cmu.reedsolomonfs.ConfigVariables;
// import edu.cmu.reedsolomonfs.client.RecoveryServiceGrpc;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.GRPCMetadata;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.TokenRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.TokenResponse;
import edu.cmu.reedsolomonfs.datatype.Node;
import edu.cmu.reedsolomonfs.server.RecoveryServiceGrpc;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverDiskRecoveryMachine;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.GRPCNode;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.SecretKeyRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.SecretKeyResponse;
// import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadRequest;
// import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadResponse;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

public class MasterImpl extends edu.cmu.reedsolomonfs.server.MasterServiceGrpc.MasterServiceImplBase {

    private static String secretKey;
    boolean storageActivated;
    Map<Integer, Long> currHeartbeat;
    Map<Integer, Long> oldHeartbeat;
    Map<Integer, Boolean> serverStatus;
    final long checkInterval = 7000;
    boolean[] chunkserversPresent;
    boolean needToRecover;
    private Map<String, Map<Integer, List<String>>> fileVersions;
    private Map<String, Integer> latestFileVersion;
    private Map<String, Long> latestChunkIndex;
    private Map<Integer, Map<String, Set<String>>> chunkServerChunkFileNames;
    private Map<String, List<Node>> metadata;
    public CliClientServiceImpl cliClientService;

    String fileVersionsFileName = "fileVersions";
    String outputLogFile = "master_output.log";

    private ManagedChannel[] recoveryChannels;
    private RecoveryServiceGrpc.RecoveryServiceBlockingStub[] stubs;
    private boolean[] recoveryConnectionEstablished;
    private int[] recoveryPorts = { 18000, 18001, 18002, 18003, 18004, 18005 };
    private String groupId;
    private Configuration conf;

    public MasterImpl(String groupId, Configuration conf) {
        // generateSecretKey();
        secretKey = "secretKey";
        cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());
        storageActivated = false;
        currHeartbeat = new ConcurrentHashMap<Integer, Long>();
        oldHeartbeat = new ConcurrentHashMap<Integer, Long>();
        serverStatus = new ConcurrentHashMap<Integer, Boolean>();
        chunkserversPresent = new boolean[ConfigVariables.TOTAL_SHARD_COUNT];
        needToRecover = false;
        this.groupId = groupId;
        this.conf = conf;

        // Initialize recovery client variables
        recoveryChannels = new ManagedChannel[ConfigVariables.TOTAL_SHARD_COUNT];
        stubs = new RecoveryServiceGrpc.RecoveryServiceBlockingStub[ConfigVariables.TOTAL_SHARD_COUNT];
        recoveryConnectionEstablished = new boolean[ConfigVariables.TOTAL_SHARD_COUNT];

        Thread hbc = new heartbeatChecker();
        fileVersions = new ConcurrentHashMap<>();
        latestFileVersion = new ConcurrentHashMap<>();
        latestChunkIndex = new ConcurrentHashMap<>();
        chunkServerChunkFileNames = new ConcurrentHashMap<>();
        metadata = new HashMap<>();
        redirectSystemOutToFile();

        // load from file if exists
        try {
            loadFromFile(fileVersionsFileName);
            System.out.println("fileVersions: " + fileVersions);
            System.out.println("latestFileVersion: " + latestFileVersion);
            System.out.println("latestChunkIndex: " + latestChunkIndex);
            System.out.println("storedChunkFileNames: " + chunkServerChunkFileNames);
            System.out.println("metadata: " + metadata);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        hbc.start();

        // Shutdown the recovery channel
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++)
            if (recoveryConnectionEstablished[i])
                recoveryChannels[i].shutdown();
    }

    // print fileVersions, latestFileVersion, latestChunkIndex,
    // chunkServerChunkFileNames, metadata
    public void printFileVersion() {
        // print out the map for debugging
        for (String filename : fileVersions.keySet()) {
            System.out.println("filename: " + filename);
            Map<Integer, List<String>> fileVersion = fileVersions.get(filename);
            for (int version : fileVersion.keySet()) {
                System.out.println("version: " + version);
                List<String> chunkFileNames = fileVersion.get(version);
                for (String chunkFileName : chunkFileNames) {
                    System.out.println("chunkFileName: " + chunkFileName);
                }
            }
        }
        for (String filename : latestFileVersion.keySet()) {
            System.out.println("filename: " + filename);
            System.out.println("latestFileVersion: " + latestFileVersion.get(filename));
        }
        for (String filename : latestChunkIndex.keySet()) {
            System.out.println("filename: " + filename);
            System.out.println("latestChunkIndex: " + latestChunkIndex.get(filename));
        }
        for (int sharedIdx : chunkServerChunkFileNames.keySet()) {
            System.out.println("sharedIdx: " + sharedIdx);
            Map<String, Set<String>> chunkFileNames = chunkServerChunkFileNames.get(sharedIdx);
            for (String chunkFileName : chunkFileNames.keySet()) {
                System.out.println("chunkFileName: " + chunkFileName);
                Set<String> chunkServerNames = chunkFileNames.get(chunkFileName);
                for (String chunkServerName : chunkServerNames) {
                    System.out.println("chunkServerName: " + chunkServerName);
                }
            }
        }
        for (String filename : metadata.keySet()) {
            System.out.println("filename: " + filename);
            List<Node> nodes = metadata.get(filename);
            for (Node node : nodes) {
                System.out.println("node: " + node);
            }
        }
    }

    public void deleteFileVersion(String filename) {
        fileVersions.remove(filename);
        latestFileVersion.remove(filename);
        latestChunkIndex.remove(filename);
        // iterate all sharedIdx and remove the filename from the
        // chunkServerChunkFileNames
        for (int sharedIdx = 0; sharedIdx < ConfigVariables.TOTAL_SHARD_COUNT; sharedIdx++) {
            if (chunkServerChunkFileNames.containsKey(sharedIdx)) {
                Map<String, Set<String>> chunkFileNames = chunkServerChunkFileNames.get(sharedIdx);
                // remove filenae from the chunkFileNames
                chunkFileNames.remove(filename);
            }
        }
        metadata.remove(filename);
        try {
            saveToFile(fileVersionsFileName);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void addFileVersion(String filename, long fileSize, long appendAt, String writeFlag) {
        // retrieve the latest file version from the map, if not found, start from 0
        int latestVersion = latestFileVersion.getOrDefault(filename, 0);
        int newVersion = latestVersion + 1;

        // construct the new chunk file names linked list from the file size, starting
        // from the appendAt position
        // concate the new chunk file names linked list after the appendAt node in the
        // latest version of the chunk file names linked list
        List<String> originalChunkFileNames;
        // 1. get the original chunk file names linked list from the file versions map
        // if the file version is not found, create a new linked list
        originalChunkFileNames = fileVersions.getOrDefault(filename, new ConcurrentHashMap<>())
                .getOrDefault(latestVersion, new java.util.LinkedList<>());
        originalChunkFileNames = new java.util.LinkedList<>(originalChunkFileNames);
        // 2. construct the new chunk file names linked list from the file size
        List<String> newChunkFileNames = new java.util.LinkedList<>();
        int chunkCnt = (int) Math.ceil((double) fileSize / ConfigVariables.BLOCK_SIZE);
        long latestChunkIdx = latestChunkIndex.getOrDefault(filename, 0L);
        // get the latest chunk file idx from the originalChunkFileNames linked list
        for (long i = latestChunkIdx; i < chunkCnt; i++) {
            newChunkFileNames.add(filename + "." + newVersion + "." + i);
        }

        // find the appendAt node in the originalChunkFileNames linked list
        int i = 0;
        for (String chunkFileName : originalChunkFileNames) {
            if (i == appendAt) {
                break;
            }
            i++;
        }
        // 3. concatenate the new chunk file names after the appendAt node
        originalChunkFileNames.addAll(i, newChunkFileNames);

        // 4. Update the file versions map and the latest file version map, latest chunk
        // index map, and chunk server chunk file names map
        // Retrieve the map for the given filename, creating and inserting an empty one
        // if none exists
        Map<Integer, List<String>> versionMap = fileVersions.computeIfAbsent(filename, k -> new ConcurrentHashMap<>());
        // Add the version and its chunk file names to the map
        versionMap.put(newVersion, originalChunkFileNames);
        latestFileVersion.put(filename, newVersion);
        latestChunkIdx += chunkCnt;
        latestChunkIndex.put(filename, latestChunkIdx);

        // Split the newChunkFileNames linked list into 6 shards
        // and store them separately in the chunkServerChunkFileNames map
        chunkCnt = newChunkFileNames.size();
        System.out.println("chunkCnt: " + chunkCnt);
        for (int idx = 0; idx < chunkCnt; idx++) {
            String chunkFileName = newChunkFileNames.get(idx);
            int shardIdx = idx % ConfigVariables.DATA_SHARD_COUNT;
            Map<String, Set<String>> shardMap = chunkServerChunkFileNames.computeIfAbsent(shardIdx,
                    k -> new ConcurrentHashMap<>());
            Set<String> chunkFileNames = shardMap.computeIfAbsent(filename, k -> Sets.newConcurrentHashSet());
            chunkFileNames.add(chunkFileName);
            chunkServerChunkFileNames.put(shardIdx, shardMap);
            // update metadata
            List<Node> nodes = metadata.getOrDefault(filename, new java.util.LinkedList<>());
            // extract chunkIdx from chunkFileName, ex: 9 from test.txt.0-9
            int chunkIdx = Integer.parseInt(chunkFileName.substring(chunkFileName.lastIndexOf('.') + 1));
            Node n = new Node(chunkIdx, shardIdx, false, ConfigVariables.BLOCK_SIZE);
            nodes.add(n);
            metadata.put(filename, nodes);
        }

        // print out the file versions map and the latest file version map
        System.out.println("fileVersions: " + fileVersions);
        System.out.println("latestFileVersion: " + latestFileVersion);
        System.out.println("latestChunkIndex: " + latestChunkIndex);
        System.out.println("storedChunkFileNames: " + chunkServerChunkFileNames);
        System.out.println("metadata: " + metadata);
        try {
            saveToFile(fileVersionsFileName);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // Save to disk
    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(fileVersions);
            oos.writeObject(latestFileVersion);
            oos.writeObject(latestChunkIndex);
            oos.writeObject(chunkServerChunkFileNames);
            oos.writeObject(metadata);
        }
        printFileVersion();
    }

    // Load from disk
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            fileVersions = (Map<String, Map<Integer, List<String>>>) ois.readObject();
            latestFileVersion = (Map<String, Integer>) ois.readObject();
            latestChunkIndex = (Map<String, Long>) ois.readObject();
            chunkServerChunkFileNames = (Map<Integer, Map<String, Set<String>>>) ois.readObject();
            metadata = (Map<String, List<Node>>) ois.readObject();
        }
    }

    // heartbeat routine
    private class heartbeatChecker extends Thread {

        boolean getLeader = false;
        int passCnt = 0;

        public void run() {
            while (true) {
                System.out.println("Checking last heartbeat");
                if (storageActivated) {
                    // for (Map.Entry<Integer, Long> entry : currHeartbeat.entrySet()) {
                    // if (entry.getValue() == oldHeartbeat.get(entry.getKey())) {
                    // // timeout
                    // System.out.println("Chunkserver " + entry.getKey() + " heartbeat timeout");
                    // chunkserversPresent[entry.getKey()] = false;
                    // needToRecover = true;
                    // } else
                    // chunkserversPresent[entry.getKey()] = true;
                    // if (needToRecover) {
                    // Master.recoverOfflineChunkserver(chunkserversPresent);
                    // needToRecover = false;
                    // break;
                    // }
                    // oldHeartbeat.put(entry.getKey(), entry.getValue());
                    // System.out.println("Pass timeout check");
                    // }
                    passCnt = 0;
                    for (Map.Entry<Integer, Long> entry : oldHeartbeat.entrySet()) {
                        if (!currHeartbeat.containsKey(entry.getKey())
                                || entry.getValue() == currHeartbeat.get(entry.getKey())) {
                            // timeout
                            System.out.println("Chunkserver " + entry.getKey() + " heartbeat timeout");
                            chunkserversPresent[entry.getKey()] = false;
                            serverStatus.put(entry.getKey(), false);
                            needToRecover = true;
                        } else {
                            passCnt++;
                            chunkserversPresent[entry.getKey()] = true;
                            oldHeartbeat.put(entry.getKey(), currHeartbeat.get(entry.getKey()));
                            System.out.println("Chunkserver " + entry.getKey() + " pass timeout check");
                        }
                    }
                    if (needToRecover) {
                        System.out.println("line 74 in MasterImpl");
                        recoverOfflineChunkserver(chunkserversPresent);
                        try {
                            Thread.sleep(checkInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        needToRecover = false;
                        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
                            oldHeartbeat.put(i, (long) 0);
                        }
                        Arrays.fill(chunkserversPresent, true);
                        // break;
                    }
                    if (!getLeader && passCnt == 6) {
                        getLeader = true;
                        System.out.println("storageActivated, start to refresh leader");
                        System.out.println("groupId: " + groupId);
                        System.out.println("conf: " + conf);
                        System.out.println("RouteTable.getInstance1: " + RouteTable.getInstance());
                        RouteTable.getInstance().updateConfiguration(groupId, conf);
                        System.out.println("RouteTable.getInstance2: " + RouteTable.getInstance());
                        try {
                            if (!RouteTable.getInstance().refreshLeader(cliClientService, groupId,
                                    1000).isOk()) {
                                throw new IllegalStateException("Refresh leader failed");
                            }
                        } catch (InterruptedException | TimeoutException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        System.out.println("RouteTable.getInstance3: " + RouteTable.getInstance());

                        final PeerId leader = RouteTable.getInstance().selectLeader(groupId);
                        System.out.println("Leader is " + leader + "\n\n");
                        System.out.println("RouteTable is " + RouteTable.getInstance() + "\n\n");
                        System.out.println("Configuration is " +
                                RouteTable.getInstance().getConfiguration(groupId) + "\n\n");
                    }

                }
                try {
                    Thread.sleep(checkInterval);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public String generateJWT(String requestType, String filePath) {
        // return "RANDOM_TOKEN";
        // Set the token expiration time (e.g., 1 hour from now)
        System.out.println("generateJWT");
        long expirationTimeMillis = System.currentTimeMillis() + 3600000; // 1 hour
        System.out.println("expirationTimeMillis: " + expirationTimeMillis);
        Date expirationDate = new Date(expirationTimeMillis);
        System.out.println("expirationDate: " + expirationDate);

        // Set the JWT claims (e.g., subject and issuer)
        Claims claims = Jwts.claims();
        System.out.println("claims: " + claims);
        // claims.setSubject("example_subject"); // this can be the name of the
        // client
        // claims.setIssuer("example_issuer"); // this can be the name of the master
        claims.put("permission", requestType);
        System.out.println("requestType: " + requestType);
        claims.put("filePath", filePath);
        System.out.println("filePath: " + filePath);
        System.out.println("secretKey: " + secretKey);

        // Build the JWT
        JwtBuilder jwtBuilder = Jwts.builder();
        System.out.println("jwtBuilder: " + jwtBuilder);
        jwtBuilder.setClaims(claims);
        System.out.println("jwtBuilder: " + jwtBuilder);
        jwtBuilder.setExpiration(expirationDate);
        System.out.println("jwtBuilder: " + jwtBuilder);
        byte[] secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        jwtBuilder.signWith(SignatureAlgorithm.HS256, secretKeyBytes);
        System.out.println("jwtBuilder: " + jwtBuilder);

        String token = jwtBuilder.compact();
        System.out.println("token: " + token);

        return token;
    }

    public static void generateSecretKey() {
        // Generate a secure random key
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256 bits key length
        secureRandom.nextBytes(keyBytes);
        secretKey = Base64.getEncoder().encodeToString(keyBytes);
    }

    @Override
    public void getToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {
        try {
            // Server-side implementation logic
            boolean isHealthy = true; // Compute the value of isHealthy
            System.out.println("getToken");
            String token = generateJWT(request.getRequestType(), request.getFilePath()); // Compute the value of token
            System.out.println("token: " + token);
            List<String> ips = new ArrayList<>(); // Compute the value of ips

            // Get nodes of current file
            // List<GRPCNode> grpcNodes = new ArrayList<>();
            // List<Node> nodes = getMetadata(request.getFilePath());
            // for (Node node : nodes) {
            // GRPCNode grpcNode = GRPCNode.newBuilder()
            // .setChunkIdx(node.getChunkIdx())
            // .setServerId(node.getServerId())
            // .setIsData(node.getIsData())
            // .build();
            // grpcNodes.add(grpcNode);
            // }
            // // print out grpcNodes
            // System.out.println("grpcNodes");
            // for (GRPCNode grpcNode : grpcNodes) {
            // System.out.println(grpcNode.getChunkIdx() + " " + grpcNode.getServerId() + "
            // " + grpcNode.getIsData());
            // }

            List<GRPCMetadata> grpcMetadatas = new ArrayList<>();
            metadata = getMetadata();
            System.out.println("metadata: " + metadata);
            for (String filePath : metadata.keySet()) {
                List<Node> nodes1 = metadata.get(filePath);
                List<GRPCNode> grpcNodes1 = new ArrayList<>();
                for (Node node : nodes1) {
                    GRPCNode grpcNode = GRPCNode.newBuilder()
                            .setChunkIdx(node.getChunkIdx())
                            .setServerId(node.getServerId())
                            .setIsData(node.getIsData())
                            .build();
                    grpcNodes1.add(grpcNode);
                }
                GRPCMetadata grpcMetadata = GRPCMetadata.newBuilder()
                        .setFilePath(filePath)
                        .addAllNodes(grpcNodes1)
                        .build();
                grpcMetadatas.add(grpcMetadata);
            }
            System.out.println("grpcMetadatas");
            // print out grpcMetadatas
            for (GRPCMetadata grpcMetadata : grpcMetadatas) {
                System.out.println(grpcMetadata.getFilePath());
                for (GRPCNode grpcNode : grpcMetadata.getNodesList()) {
                    System.out.println(
                            grpcNode.getChunkIdx() + " " + grpcNode.getServerId() + " " + grpcNode.getIsData());
                }
            }

            // Prepare the response
            TokenResponse.Builder tokenResponseBuilder = TokenResponse.newBuilder();
            System.out.print("response: " + tokenResponseBuilder);
            tokenResponseBuilder.setIsHealthy(isHealthy);
            System.out.print("tokenResponseBuilder: " + tokenResponseBuilder);
            tokenResponseBuilder.setToken(token);
            System.out.print("tokenResponseBuilder: " + tokenResponseBuilder);
            // tokenResponseBuilder.addAllIps(ips);
            // System.out.print("tokenResponseBuilder: " + tokenResponseBuilder);
            tokenResponseBuilder.addAllMetadata(grpcMetadatas);
            System.out.print("tokenResponseBuilder: " + tokenResponseBuilder);
            TokenResponse response = tokenResponseBuilder.build();
            System.out.println("response: " + response);

            // Send the response to the client
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            System.out.println("Exception in getToken");
            e.printStackTrace();
        }
    }

    @Override
    public void heartBeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String serverTag = request.getServerTag();

            // System.out.println("request getChunkFileNamesMap: ");
            // System.out.println(request.getChunkFileNamesMap());
            Map<String, ChunkFileNames> chunkFilesMap = request.getChunkFileNamesMap();
            // TODO: recovery/delete chunk file if not exists
            // TODO: check if the difference is expected or not
            // compare and print out the difference of chunk file names map with
            // chunkServerChunkFileNames
            for (Map.Entry<String, ChunkFileNames> entry : chunkFilesMap.entrySet()) {
                String filename = entry.getKey();
                ChunkFileNames chunkFileNames = entry.getValue();
                // convert chunkFileNames to a set
                Set<String> chunkFileNamesSet = Sets.newConcurrentHashSet();
                for (int i = 0; i < chunkFileNames.getFileNameCount(); i++) {
                    chunkFileNamesSet.add(chunkFileNames.getFileName(i));
                }
                // print the difference between chunkFileNamesSet and chunkServerChunkFileNames
                Set<String> diff = Sets.difference(chunkFileNamesSet,
                        chunkServerChunkFileNames.getOrDefault(Integer.parseInt(serverTag), new ConcurrentHashMap<>())
                                .getOrDefault(filename, Sets.newConcurrentHashSet()));
                System.out.println("servertag: " + serverTag + " filename: " + filename + " diff: " + diff);
            }

            // storage activated
            if (!storageActivated) {
                storageActivated = true;

                for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
                    oldHeartbeat.put(i, (long) 0);
                    serverStatus.put(i, false);
                }
            }

            System.out.println("Received Heatbeat in: " + timestamp);
            System.out.println(serverTag);

            serverStatus.put(Integer.parseInt(serverTag), true);

            // update last heartbeat timestamp
            currHeartbeat.put(Integer.parseInt(serverTag), System.currentTimeMillis());

            HeartbeatResponse response = HeartbeatResponse.newBuilder().setReceive(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void writeSuccess(ackMasterWriteSuccessRequest request,
            StreamObserver<ackMasterWriteSuccessRequestResponse> responseObserver) {
        try {
            // log
            System.out.println(request.getFileName());
            System.out.println(request.getFileSize());
            System.out.println(request.getAppendAt());
            System.out.println(request.getWriteFlag());
            // addFileVersion if writeFlag is "write"
            if (request.getWriteFlag().equals("create"))
                addFileVersion(request.getFileName(), request.getFileSize(), request.getAppendAt(),
                        request.getWriteFlag());

            // deleteFileVersion if writeFlag is "delete"
            if (request.getWriteFlag().equals("delete"))
                deleteFileVersion(request.getFileName());

            ackMasterWriteSuccessRequestResponse response = ackMasterWriteSuccessRequestResponse.newBuilder()
                    .setSuccess(true).build();

            generateSecretKey();
            // notify storage cluster to update secret key
            updateSecretKey(cliClientService, secretKey);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    public List<Node> getMetadata(String filePath) {
        return metadata.get(filePath);
    }

    public void addMetadata(String filePath, List<Node> nodes) {
        metadata.put(filePath, nodes);
    }

    public void deleteMetadata(String filePath) {
        metadata.remove(filePath);
    }

    public Map<String, List<Node>> getMetadata() {
        return metadata;
    }

    private void initRecoveryChannelsAndStubs(int serverIdx) {
        // Create a gRPC channel to connect to the chunkserver
        recoveryChannels[serverIdx] = ManagedChannelBuilder.forAddress("localhost", recoveryPorts[serverIdx])
                .usePlaintext() // For simplicity, using plaintext communication
                .build();

        // Create a client stub using the generated MyServiceGrpc class
        stubs[serverIdx] = RecoveryServiceGrpc.newBlockingStub(recoveryChannels[serverIdx]);
        recoveryConnectionEstablished[serverIdx] = true;
    }

    // Master requests chunk file data from cluster
    public byte[] makeRecoveryReadRequest(String filePath, int serverIdx) {
        if (!recoveryConnectionEstablished[serverIdx])
            initRecoveryChannelsAndStubs(serverIdx);
        // Perform RPC calls using the stub
        RecoveryReadRequest request = RecoveryReadRequest.newBuilder().setChunkFilePath(filePath).build();
        RecoveryReadResponse response = stubs[serverIdx].recoveryRead(request);
        // System.out.println("Response from server: " + response.getChunkFileData());
        return response.getChunkFileData().toByteArray();
    }

    // Master writes recovered chunk file data to recovered chunkservers
    public boolean makeRecoveryWriteRequest(String filePath, byte[] chunkFileData, int serverIdx) {
        if (!recoveryConnectionEstablished[serverIdx])
            initRecoveryChannelsAndStubs(serverIdx);
        // Perform RPC calls using the stub
        System.out.println("makeRecoveryWriteRequest: " + filePath);
        RecoveryWriteRequest request = RecoveryWriteRequest.newBuilder().setChunkFilePath(filePath)
                .setChunkFileData(ByteString.copyFrom(chunkFileData)).build();
        RecoveryWriteResponse response = stubs[serverIdx].recoveryWrite(request);
        System.out.println("Response from server: " + response.getRecoveryWriteSuccess());
        return response.getRecoveryWriteSuccess();
    }

    private void relaunchOfflineChunkserver(int serverIdx, CountDownLatch latch) {
        // Create a new thread to re launch one offline chunkserver
        Thread launchThread = new Thread(() -> {
            try {
                // Specify the Maven command
                String[] mvnCommand = {
                        "mvn",
                        "exec:java",
                        "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver",
                        "-Dexec.args=chunkserver" + (serverIdx + 1) + " cluster 127.0.0.1:808" + (serverIdx + 1)
                                + " 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 "
                                + serverIdx
                };

                // Build the process
                ProcessBuilder pb = new ProcessBuilder(mvnCommand);
                pb.redirectErrorStream(true);

                // Start the process
                Process process = pb.start();

                // latch.countDown();

                // Wait for the process to complete
                int exitCode = process.waitFor();

                System.out.println("Relaunched chunkserver completes with exit code: " + exitCode);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Start the thread to launch the additional process
        launchThread.start();
    }

    public void recoverOfflineChunkserver(boolean[] chunkserverPresent) throws IllegalArgumentException {
        System.out.println("line 97 Master");
        String[] chunkFilePathsInOneServer = null;
        List<Integer> offlineServerIndices = new ArrayList<>();
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
            System.out.println("chunkserverpresent[" + i + "] = " + chunkserverPresent[i]);
            if (!chunkserverPresent[i]) {
                offlineServerIndices.add(i);
                if (offlineServerIndices.size() > ConfigVariables.PARITY_SHARD_COUNT) {
                    System.out.println("The number of offline chunkservers exceed the maximum number to recover");
                    throw new IllegalArgumentException(
                            "The number of offline chunkservers exceed the maximum number to recover");
                }
                continue;
            }
            // For temperary test only, retrieve all chunk file paths from one server
            // In normal case, Master should know all chunk file paths in any existing
            // chunkserver
            if (chunkFilePathsInOneServer == null) {
                chunkFilePathsInOneServer = getChunkserverChunkFilePaths(i);
                System.out.println("chunkFilePathsInOneServer: " + Arrays.toString(chunkFilePathsInOneServer));
            }
        }

        System.out.println("line 115 Master");
        // For local test only, create disk directory of offline chunkserver
        for (Integer offlineServerIdx : offlineServerIndices) {
            System.out.println("Current offline server index is: " + offlineServerIdx);
            Path directory = Paths.get("./ClientClusterCommTestFiles/Disks/chunkserver-" + offlineServerIdx);
            try {
                Files.createDirectories(directory); // Create the directory and any nonexistent parent directories
            } catch (IOException e) {
                System.out.println("Failed to create the directory: " + e.getMessage());
            }
        }

        if (chunkFilePathsInOneServer == null)
            throw new IllegalArgumentException("There are no files in any chunkserver disks");

        // Relaunch offline chunkservers
        CountDownLatch latch = new CountDownLatch(offlineServerIndices.size());
        for (Integer offlineServerIdx : offlineServerIndices) {
            System.out.println("Relaunch offline chunkserver" + offlineServerIdx);
            // relaunchOfflineChunkserver(offlineServerIdx);
            relaunchOfflineChunkserver(offlineServerIdx, latch);
        }

        // try {
        // latch.await();
        // } catch (InterruptedException e) {
        // }

        // try {
        // // wait for offline chunkservers to relaunch
        // Thread.sleep(10000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }

        for (int offlineServerIdx : offlineServerIndices) {
            while (!serverStatus.get(offlineServerIdx)) {
                try {
                    // wait for offline chunkservers to relaunch
                    Thread.sleep(5000);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
            }
            System.out.print("offline server " + offlineServerIdx + " is relaunched");
            initRecoveryChannelsAndStubs(offlineServerIdx);
        }

        // print the chunkFilePathsInOneServer
        System.out.println("chunkFilePathsInOneServer: " + Arrays.toString(chunkFilePathsInOneServer));
        for (String chunkFilePath : chunkFilePathsInOneServer) {
            int lastIndex = chunkFilePath.lastIndexOf("-"); // Get the index of the last dash sign
            if (lastIndex == -1 || lastIndex >= chunkFilePath.length() - 1)
                throw new IllegalArgumentException("The naming of file " + chunkFilePath + " has some problems");
            String numberString = chunkFilePath.substring(lastIndex + 1);
            int chunkIdx = Integer.parseInt(numberString);
            int chunkGroupStartIdx = chunkIdx / ConfigVariables.TOTAL_SHARD_COUNT * ConfigVariables.TOTAL_SHARD_COUNT;
            String filePathWithDash = chunkFilePath.substring(0, lastIndex + 1);
            String filePath = chunkFilePath.substring(0, lastIndex);
            // i stands for index of chunk file in current group of chunk files across six
            // chunkservers

            ChunkserverDiskRecoveryMachine recoveryMachine = new ChunkserverDiskRecoveryMachine();
            for (int i = 0; i < chunkserverPresent.length; i++) {
                if (!chunkserverPresent[i])
                    continue;
                // Create the new filename with the updated number
                String curChunkFilePath = filePathWithDash + (chunkGroupStartIdx + i);
                byte[] curChunkData = makeRecoveryReadRequest(curChunkFilePath, i);
                recoveryMachine.addChunkserverDisksData(i, curChunkData);
            }

            // recover lost chunk file in current group
            recoveryMachine.recoverChunkserverDiskData();

            for (Integer offlineServerIdx : offlineServerIndices) {
                byte[] recoveredChunkData = recoveryMachine.retrieveRecoveredDiskData(offlineServerIdx);
                String recoveredChunkFilePath = "./ClientClusterCommTestFiles/Disks/chunkserver-" + offlineServerIdx
                        + "/" + filePath + "-" + (chunkGroupStartIdx + offlineServerIdx);
                // String recoveredChunkFilePath = filePath + "-" + (chunkGroupStartIdx +
                // offlineServerIdx);
                System.out.println(
                        "Server Status: " + " ID " + offlineServerIdx + "," + serverStatus.get(offlineServerIdx));
                // while (!serverStatus.get(offlineServerIdx)) {
                // try {
                // // wait for offline chunkservers to relaunch

                // Thread.sleep(5000);
                // } catch (InterruptedException e2) {
                // e2.printStackTrace();
                // }
                // }

                boolean writeSuccess = makeRecoveryWriteRequest(
                        filePath + "-" + (chunkGroupStartIdx + offlineServerIdx), recoveredChunkData, offlineServerIdx);
                System.out.println("writeSuccess ? : " + writeSuccess);
                // while (!writeSuccess) {
                // try {
                // writeSuccess = makeRecoveryWriteRequest(filePath, recoveredChunkData,
                // offlineServerIdx);
                // } catch (StatusRuntimeException e1) {
                // writeSuccess = false;
                // try {
                // // wait for offline chunkservers to relaunch
                // Thread.sleep(5000);
                // } catch (InterruptedException e2) {
                // e2.printStackTrace();
                // }
                // }
                // System.out.println(
                // "Recovered " + recoveredChunkFilePath + " in chunkserver-" + offlineServerIdx
                // + " failed");
                // }
                // try (FileOutputStream fos = new FileOutputStream(recoveredChunkFilePath)) {
                // fos.write(recoveredChunkData); // Write the recovered data to the recovered
                // file path
                // } catch (IOException e) {
                // e.printStackTrace();
                // }
            }

        }
    }

    /**
     * For test only. This method gets all chunk file names in a functional chunk
     * server
     * 
     * @param filePath
     * @return
     */
    private String[] getChunkserverChunkFilePaths(int serverIdx) {
        // Specify the folder path
        String folderPath = "./ClientClusterCommTestFiles/Disks/chunkserver-" + serverIdx + "/";

        // Create a File object for the folder
        File folder = new File(folderPath);

        // Check if the folder exists and is a directory
        if (folder.exists() && folder.isDirectory()) {
            // Get an array of File objects representing the files in the folder
            File[] files = folder.listFiles();

            String[] fileNames = new String[files.length];

            // Iterate over the files and print their names
            for (int i = 0; i < fileNames.length; i++) {
                if (files[i].isFile()) {
                    fileNames[i] = files[i].getName();
                }
            }
            return fileNames;
        } else {
            System.out.println("Invalid folder path or folder does not exist.");
        }
        return null;
    }

    public void redirectSystemOutToFile() {
        try {
            // Create a new file output stream for the desired file
            FileOutputStream fileOutputStream = new FileOutputStream(outputLogFile);

            // Create a new print stream that writes to the file output stream
            PrintStream printStream = new PrintStream(fileOutputStream);

            // Redirect System.out to the print stream
            System.setOut(printStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void updateSecretKey(final CliClientServiceImpl cliClientService, String secretKey)
            throws RemotingException, InterruptedException {
        System.out.println("master send Updating secret key request to chunkserver " + secretKey);
        final int n = 10000;
        final CountDownLatch latch = new CountDownLatch(n);
        // final long start = System.currentTimeMillis();

        // WriteRequest request = packWriteRequest("touch", filePath,
        // encoder.getFileSize(), 0, shards, "create",
        // encoder.getLastChunkIdx());

        // Pass padded file size
        UpdateSecretKeyRequest request = packUpdateSecretKeyRequest();
        System.out.println("updateSecretKey request: " + request);
        final PeerId leader = RouteTable.getInstance().selectLeader(groupId);
        System.out.println("updateSecretKey request leader: " + leader);
        updateSecretKeyRequest(cliClientService, leader, request, latch);
        // latch.await();
        // System.out.println(n + " ops, cost : " + (System.currentTimeMillis() - start)
        // + " mssssssss.");
    }

    private static UpdateSecretKeyRequest packUpdateSecretKeyRequest() {
        UpdateSecretKeyRequest.Builder requestBuilder = UpdateSecretKeyRequest.newBuilder();
        requestBuilder.setSecretKey(secretKey);
        return requestBuilder.build();
    }

    private static void updateSecretKeyRequest(final CliClientServiceImpl cliClientService, final PeerId leader,
            UpdateSecretKeyRequest request, CountDownLatch latch) throws RemotingException,
            InterruptedException {
        try {
            System.out.println("updateSecretKeyRequest sendddddd");
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
        } catch (InaccessibleObjectException e) {
            System.err.println("Caught InaccessibleObjectException: " + e.getMessage());
        }
    }

}
