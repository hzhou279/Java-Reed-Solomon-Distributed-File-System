package edu.cmu.reedsolomonfs.server.Master;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import edu.cmu.reedsolomonfs.ConfigVariables;
// import edu.cmu.reedsolomonfs.client.RecoveryServiceGrpc;
import edu.cmu.reedsolomonfs.client.ClientMasterServiceGrpc.ClientMasterServiceImplBase;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.GRPCMetadata;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenRequest;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenResponse;
import edu.cmu.reedsolomonfs.datatype.Node;
import edu.cmu.reedsolomonfs.server.RecoveryServiceGrpc;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverDiskRecoveryMachine;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteResponse;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.GRPCNode;
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

import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter.Magenta;

import com.google.protobuf.ByteString;

import java.util.HashMap;

public class Master extends ClientMasterServiceImplBase {

    private static String secretKey;
    private static Map<String, List<Node>> metadata;
    private static MasterImpl masterImpl;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setErr(new PrintStream("/dev/null"));
        masterImpl = new MasterImpl();
        Server server = ServerBuilder.forPort(8080)
                .addService(masterImpl)
                .build()
                .start();

        metadata = new HashMap<>();

        // Generate secret key for master signing JWTs
        generateSecretKey();

        // FileInputStream fileInputStream = new FileInputStream("./as");
        server.awaitTermination();
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
        List<Node> nodes = masterImpl.getMetadata(request.getFilePath());
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
