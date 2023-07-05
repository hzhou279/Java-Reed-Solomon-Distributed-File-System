package edu.cmu.reedsolomonfs.server.Master;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import edu.cmu.reedsolomonfs.client.RecoveryServiceGrpc;
import edu.cmu.reedsolomonfs.client.ClientMasterServiceGrpc.ClientMasterServiceImplBase;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.GRPCMetadata;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenRequest;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenResponse;
import edu.cmu.reedsolomonfs.datatype.Node;
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
import java.util.HashMap;

public class Master extends ClientMasterServiceImplBase {

    private static String secretKey;
    private static Map<String, List<Node>> metadata;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(8080)
                .addService(new MasterImpl())
                .build()
                .start();

        metadata = new HashMap<>();

        // Generate secret key for master signing JWTs
        generateSecretKey();

        // Create a gRPC channel to connect to the chunkserver
        ManagedChannel recoveryChannel = ManagedChannelBuilder.forAddress("localhost", 49152)
                .usePlaintext() // For simplicity, using plaintext communication
                .build();

        // Create a client stub using the generated MyServiceGrpc class
        // MyServiceGrpc.MyServiceBlockingStub stub = MyServiceGrpc.newBlockingStub(channel);
        RecoveryServiceGrpc.RecoveryServiceBlockingStub stub = RecoveryServiceGrpc.newBlockingStub(recoveryChannel);

        // send recovery request
        String missingFilePath = "???";
        sendRecoveryRequest(stub, missingFilePath);
        

        // FileInputStream fileInputStream = new FileInputStream("./as");
        server.awaitTermination();

        // Shutdown the recovery channel
        recoveryChannel.shutdown();
    }

    // Implement the client functionality
    private static void sendRecoveryRequest(RecoveryServiceGrpc.RecoveryServiceBlockingStub stub, String filePath) {
        // Perform RPC calls using the stub
        RecoveryReadRequest request = RecoveryReadRequest.newBuilder().setFilePath(filePath).build();
        RecoveryReadResponse response = stub.recoveryRead(request);
        System.out.println("Response from server: " + response.getChunkFileData());
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
