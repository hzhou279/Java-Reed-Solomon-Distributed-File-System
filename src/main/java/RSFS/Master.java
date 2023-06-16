package RSFS;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import RSFSProto.Systemproto.TokenAndMetadataRequest;
import RSFSProto.Systemproto.TokenAndMetadataResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class Master extends RSFSProto.ClientMasterServiceGrpc.ClientMasterServiceImplBase {

    private static Map<String, FileMetadata> metadata = new HashMap<>();

    @Override
    public void retrieveTokenAndMetadata(TokenAndMetadataRequest request, StreamObserver<TokenAndMetadataResponse> responseObserver) {
        // Server-side implementation logic
        // boolean isHealthy = ...;  // Compute the value of isHealthy
        // String token = ...;  // Compute the value of token
        // List<String> ips = ...;  // Compute the value of ips
        // Map<String, LinkedList<Integer>> data = ...;  // Compute the value of data

        // Prepare the response
        // Response response = Response.newBuilder()
        //     .setIsHealthy(isHealthy)
        //     .setToken(token)
        //     .addAllIps(ips)
        //     .putAllData(data)
        //     .build();

        // Send the response to the client
        // responseObserver.onNext(response);
        // responseObserver.onCompleted();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Create the server
        Server server = ServerBuilder.forPort(50051)
                .addService(new Master())
                .build();

        // Start the server
        server.start();

        // Await termination
        server.awaitTermination();
    }


    public void createFileMetadata() {

    }


    public void addFileMetadata(String filePath, int fileSize, List<Integer> blocks) {
        metadata.put(filePath, new FileMetadata(fileSize, blocks));
    }

    public FileMetadata retrieveFileMetaData(String filePath) {
        return metadata.get(filePath);
    }
    
}
