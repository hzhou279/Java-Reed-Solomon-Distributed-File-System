package edu.cmu.reedsolomonfs.client;

import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenRequest;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {
    public static void main(String[] args) {
        // Create a channel to connect to the server
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext() // Use insecure connection, for testing only
                .build();

        // Create a stub for the service
        ClientMasterServiceGrpc.ClientMasterServiceBlockingStub stub = ClientMasterServiceGrpc.newBlockingStub(channel);

        TokenRequest request = TokenRequest.newBuilder()
                .setRequestType("R")
                .setFilePath("./Files/test.txt")
                .build();

        // Make the RPC call and receive the response
        TokenResponse response = stub.getToken(request);

        System.out.println("JWT token received at client is: " + response.getToken());

        // Shutdown the channel
        channel.shutdown();
    }
}
