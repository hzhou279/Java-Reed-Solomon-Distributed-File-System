package edu.cmu.reedsolomonfs.server.Master;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import edu.cmu.reedsolomonfs.ConfigVariables;
import edu.cmu.reedsolomonfs.client.RecoveryServiceGrpc;
import edu.cmu.reedsolomonfs.client.ClientMasterServiceGrpc.ClientMasterServiceImplBase;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.GRPCMetadata;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenRequest;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenResponse;
import edu.cmu.reedsolomonfs.datatype.Node;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverDiskRecoveryMachine;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.GRPCNode;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadRequest;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter.Magenta;

import java.util.HashMap;

public class Master extends ClientMasterServiceImplBase {

    private static String secretKey;
    private static Map<String, List<Node>> metadata;
    private static ManagedChannel[] recoveryChannels;
    private static RecoveryServiceGrpc.RecoveryServiceBlockingStub[] stubs;
    private static boolean[] recoveryConnectionEstablished;
    private static int[] recoveryPorts = { 18000, 18001, 18002, 18003, 18004, 18005 };

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(8080)
                .addService(new MasterImpl())
                .build()
                .start();

        metadata = new HashMap<>();
        recoveryChannels = new ManagedChannel[ConfigVariables.TOTAL_SHARD_COUNT];
        stubs = new RecoveryServiceGrpc.RecoveryServiceBlockingStub[ConfigVariables.TOTAL_SHARD_COUNT];
        recoveryConnectionEstablished = new boolean[ConfigVariables.TOTAL_SHARD_COUNT];

        // Generate secret key for master signing JWTs
        generateSecretKey();

        // FileInputStream fileInputStream = new FileInputStream("./as");
        server.awaitTermination();

        // Shutdown the recovery channel
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++)
            if (recoveryConnectionEstablished[i])
                recoveryChannels[i].shutdown();
    }

    private static void initRecoveryChannelsAndStubs(int serverIdx) {
        // Create a gRPC channel to connect to the chunkserver
        recoveryChannels[serverIdx] = ManagedChannelBuilder.forAddress("localhost", recoveryPorts[serverIdx])
                .usePlaintext() // For simplicity, using plaintext communication
                .build();

        // Create a client stub using the generated MyServiceGrpc class
        stubs[serverIdx] = RecoveryServiceGrpc.newBlockingStub(recoveryChannels[serverIdx]);
        recoveryConnectionEstablished[serverIdx] = true;
    }

    // Master request chunk file data from cluster
    public static byte[] makeRecoveryRequest(String filePath, int serverIdx) {
        if (!recoveryConnectionEstablished[serverIdx])
            initRecoveryChannelsAndStubs(serverIdx);
        // Perform RPC calls using the stub
        RecoveryReadRequest request = RecoveryReadRequest.newBuilder().setFilePath(filePath).build();
        RecoveryReadResponse response = stubs[serverIdx].recoveryRead(request);
        // System.out.println("Response from server: " + response.getChunkFileData());
        return response.getChunkFileData().toByteArray();
    }

    public static void recoverOfflineChunkserver(boolean[] chunkserverPresent) throws IllegalArgumentException {
        System.out.println("line 97 Master");
        String[] chunkFilePathsInOneServer = null;
        List<Integer> offlineServerIndices = new ArrayList<>();
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
            System.out.println("chunkserverpresent[" + i + "] = " + chunkserverPresent[i]);
            if (!chunkserverPresent[i]) {
                offlineServerIndices.add(i);
                if (offlineServerIndices.size() > ConfigVariables.PARITY_SHARD_COUNT)
                    throw new IllegalArgumentException(
                            "The number of offline chunkservers exceed the maximum number to recover");
                continue;
            }
            // For temperary test only, retrieve all chunk file paths from one server
            // In normal case, Master should know all chunk file paths in any existing
            // chunkserver
            if (chunkFilePathsInOneServer == null)
                chunkFilePathsInOneServer = getChunkserverChunkFilePaths(i);
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
                byte[] curChunkData = makeRecoveryRequest(curChunkFilePath, i);
                recoveryMachine.addChunkserverDisksData(i, curChunkData);
            }

            // recover lost chunk file in current group
            recoveryMachine.recoverChunkserverDiskData();

            for (Integer offlineServerIdx : offlineServerIndices) {
                byte[] recoveredChunkData = recoveryMachine.retrieveRecoveredDiskData(offlineServerIdx);
                String recoveredChunkFilePath = "./ClientClusterCommTestFiles/Disks/chunkserver-" + offlineServerIdx + "/" + filePath + "-" + (chunkGroupStartIdx + offlineServerIdx);
                try (FileOutputStream fos = new FileOutputStream(recoveredChunkFilePath)) {
                    fos.write(recoveredChunkData); // Write the recovereed data to the recovered file path
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
    private static String[] getChunkserverChunkFilePaths(int serverIdx) {
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

    public List<Node> getMetadata(String filePath) {
        return metadata.get(filePath);
    }

    public void addMetadata(String filePath, List<Node> nodes) {
        metadata.put(filePath, nodes);
    }

    public void deleteMetadata(String filePath) {
        metadata.remove(filePath);
    }

    @Override
    public void getToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {
        // Server-side implementation logic
        boolean isHealthy = true; // Compute the value of isHealthy
        String token = generateJWT(request.getRequestType(), request.getFilePath()); // Compute the value of token
        List<String> ips = new ArrayList<>(); // Compute the value of ips

        // Get nodes of current file
        List<GRPCNode> grpcNodes = new ArrayList<>();
        List<Node> nodes = getMetadata(request.getFilePath());
        for (Node node : nodes) {
            GRPCNode grpcNode = GRPCNode.newBuilder()
                    .setChunkIdx(node.getChunkIdx())
                    .setServerId(node.getServerId())
                    .setIsData(node.getIsData())
                    .build();
            grpcNodes.add(grpcNode);
        }

        GRPCMetadata data = GRPCMetadata.newBuilder()
                .setFilePath(request.getFilePath())
                .addAllNodes(grpcNodes)
                .build(); // Compute the value of data

        // Prepare the response
        TokenResponse response = TokenResponse.newBuilder()
                .setIsHealthy(isHealthy)
                .setToken(token)
                .addAllIps(ips)
                .setMetadata(data)
                .build();

        // Send the response to the client
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public String generateJWT(String requestType, String filePath) {
        // Set the token expiration time (e.g., 1 hour from now)
        long expirationTimeMillis = System.currentTimeMillis() + 3600000; // 1 hour
        Date expirationDate = new Date(expirationTimeMillis);

        // Set the JWT claims (e.g., subject and issuer)
        Claims claims = Jwts.claims();
        // claims.setSubject("example_subject"); // this can be the name of the client
        // claims.setIssuer("example_issuer"); // this can be the name of the master
        claims.put("permission", requestType);
        claims.put("filePath", filePath);

        // Build the JWT
        JwtBuilder jwtBuilder = Jwts.builder()
                .setClaims(claims)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secretKey);

        // Generate the JWT token
        return jwtBuilder.compact();
    }

    public static void generateSecretKey() {
        // Generate a secure random key
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256 bits key length
        secureRandom.nextBytes(keyBytes);
        secretKey = Base64.getEncoder().encodeToString(keyBytes);
    }

}
