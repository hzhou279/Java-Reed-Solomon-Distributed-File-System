package edu.cmu.reedsolomonfs.server.Master;

import edu.cmu.reedsolomonfs.ConfigVariables;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverDiskRecoveryMachine;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequestResponse;
import io.grpc.stub.StreamObserver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.impl.LogKitLogger;

public class MasterImpl extends edu.cmu.reedsolomonfs.server.MasterServiceGrpc.MasterServiceImplBase {

    boolean storageActivated;
    Map<Integer, Long> currHeartbeat;
    Map<Integer, Long> oldHeartbeat;
    static final long checkInterval = 7000;
    static boolean[] chunkserversPresent;
    static boolean needToRecover;
    private Map<String, Map<Integer, List<String>>> fileVersions;
    private Map<String, Integer> latestFileVersion;
    private Map<String, Long> latestChunkIndex;
    private Map<Integer, Map<String, List<String>>> chunkServerChunkFileNames;

    static String fileVersionsFileName = "fileVersions";

    public MasterImpl() {
        storageActivated = false;
        currHeartbeat = new ConcurrentHashMap<Integer, Long>();
        oldHeartbeat = new ConcurrentHashMap<Integer, Long>();
        chunkserversPresent = new boolean[ConfigVariables.TOTAL_SHARD_COUNT];
        needToRecover = false;

        Thread hbc = new heartbeatChecker();
        fileVersions = new ConcurrentHashMap<>();
        latestFileVersion = new ConcurrentHashMap<>();
        latestChunkIndex = new ConcurrentHashMap<>();
        chunkServerChunkFileNames = new ConcurrentHashMap<>();
        // load from file if exists
        try {
            loadFromFile(fileVersionsFileName);
            System.out.println("fileVersions: " + fileVersions);
            System.out.println("latestFileVersion: " + latestFileVersion);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        hbc.start();
    }

    public void addFileVersion(String filename, long fileSize, long appendAt, String writeFlag) {
        // retrieve the latest file version from the map, if not found, start from 0
        int latestVersion = latestFileVersion.getOrDefault(filename, 0);
        int newVersion = latestVersion + 1;

        // construct the new chunk file names linked list from the file size, starting from the appendAt position
        // concate the new chunk file names linked list after the appendAt node in the latest version of the chunk file names linked list
        List<String> originalChunkFileNames;
        // 1. get the original chunk file names linked list from the file versions map
        // if the file version is not found, create a new linked list
        originalChunkFileNames = fileVersions.getOrDefault(filename, new ConcurrentHashMap<>()).getOrDefault(latestVersion, new java.util.LinkedList<>());
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


        // 4. Update the file versions map and the latest file version map, latest chunk index map, and chunk server chunk file names map
        // Retrieve the map for the given filename, creating and inserting an empty one if none exists
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
            Map<String, List<String>> shardMap = chunkServerChunkFileNames.computeIfAbsent(shardIdx, k -> new ConcurrentHashMap<>());
            List<String> chunkFileNames = shardMap.computeIfAbsent(filename, k -> new java.util.LinkedList<>());
            chunkFileNames.add(chunkFileName);
            chunkServerChunkFileNames.put(shardIdx, shardMap);
        }

        // print out the file versions map and the latest file version map
        System.out.println("fileVersions: " + fileVersions);
        System.out.println("latestFileVersion: " + latestFileVersion);
        System.out.println("latestChunkIndex: " + latestChunkIndex);
        System.out.println("storedChunkFileNames: " + chunkServerChunkFileNames);
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
        }
    }

    // Load from disk
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            fileVersions = (Map<String, Map<Integer, List<String>>>) ois.readObject();
            latestFileVersion = (Map<String, Integer>) ois.readObject();
            latestChunkIndex = (Map<String, Long>) ois.readObject();
            chunkServerChunkFileNames = (Map<Integer, Map<String, List<String>>>) ois.readObject();
        }
    }

    // heartbeat routine
    private class heartbeatChecker extends Thread {

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
                    for (Map.Entry<Integer, Long> entry : oldHeartbeat.entrySet()) {
                        if (!currHeartbeat.containsKey(entry.getKey())
                                || entry.getValue() == currHeartbeat.get(entry.getKey())) {
                            // timeout
                            System.out.println("Chunkserver " + entry.getKey() + " heartbeat timeout");
                            chunkserversPresent[entry.getKey()] = false;
                            needToRecover = true;
                        } else {
                            chunkserversPresent[entry.getKey()] = true;
                            oldHeartbeat.put(entry.getKey(), entry.getValue());
                            System.out.println("Chunkserver " + entry.getKey() + " pass timeout check");
                        }
                    }
                    if (needToRecover) {
                        System.out.println("line 74 in MasterImpl");
                        Master.recoverOfflineChunkserver(chunkserversPresent);
                        try {
                            Thread.sleep(checkInterval);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        needToRecover = false;
                    }
                    break;
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

    @Override
    public void heartBeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String serverTag = request.getServerTag();

            // System.out.println("request getChunkFileNamesMap: ");
            // System.out.println(request.getChunkFileNamesMap());
            // storage activated
            if (!storageActivated) {
                storageActivated = true;
                for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
                    oldHeartbeat.put(i, (long) 0);
                }
            }

            System.out.println("Received Heatbeat in: " + timestamp);
            System.out.println(serverTag);

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
            addFileVersion(request.getFileName(), request.getFileSize(), request.getAppendAt(), request.getWriteFlag());

            ackMasterWriteSuccessRequestResponse response = ackMasterWriteSuccessRequestResponse.newBuilder()
                    .setSuccess(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

}
