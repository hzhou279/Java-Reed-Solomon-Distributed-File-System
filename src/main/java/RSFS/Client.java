package RSFS;

import java.io.IOException;

import RSFSProto.ClientMasterServiceGrpc;
import RSFSProto.Systemproto.TokenAndMetadataRequest;
import RSFSProto.Systemproto.TokenAndMetadataResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {

    private static final String[] ALL_DISK_PATHS = {"./Disks/DataDiskOne.txt", "./Disks/DataDiskTwo.txt", "./Disks/DataDiskThree.txt"
,"./Disks/DataDiskFour.txt", "./Disks/ParityDiskOne.txt", "./Disks/ParityDiskTwo.txt"};

    public static void main(String[] args) throws IOException {

        // Create a channel to connect to the server
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Use insecure connection, for testing only
                .build();

        // Create a stub for the service
        ClientMasterServiceGrpc.ClientMasterServiceBlockingStub stub = ClientMasterServiceGrpc.newBlockingStub(channel);

        Encoder encoder = new Encoder("./Files/test.txt", ALL_DISK_PATHS);
        encoder.encode();
        // encoder.store();
        // Decoder decoder = new Decoder("./ReadFiles/test.txt", ALL_DISK_PATHS);
        // decoder.decode();
        // decoder.store();
        // System.out.println(encoder.getFileSize());
        // System.out.println(decoder.getFileData().length);
        // if (encoder.getFileSize() != decoder.getFileData().length) System.out.println("decode failed");
        TokenAndMetadataRequest request = TokenAndMetadataRequest.newBuilder()
                .setRequestType("R")
                .setFilePath("./Files/test.txt")
                .build();

        // // Make the RPC call and receive the response
        TokenAndMetadataResponse response = stub.retrieveTokenAndMetadata(request);

        // Shutdown the channel
        channel.shutdown();
    }
}
