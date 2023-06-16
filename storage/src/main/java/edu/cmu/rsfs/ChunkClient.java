package edu.cmu.rsfs;

import com.google.protobuf.ByteString;
import edu.cmu.rsfs.Chunkserver.ReadResponse;
import edu.cmu.rsfs.Chunkserver.WriteRequest;
import edu.cmu.rsfs.Chunkserver.ReadRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ChunkClient {
    public static void main(String[] args) throws Exception {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        ChunkServiceGrpc.ChunkServiceBlockingStub stub = ChunkServiceGrpc.newBlockingStub(channel);

        // Write chunk
        WriteRequest writeRequest = WriteRequest.newBuilder()
                .setChunkId("exampleChunk")
                .setData(ByteString.copyFromUtf8("exampleData"))
                .build();
        stub.writeChunk(writeRequest);

        // Read chunk
        ReadRequest readRequest = ReadRequest.newBuilder()
                .setChunkId("exampleChunk")
                .build();
        ReadResponse readResponse = stub.readChunk(readRequest);
        System.out.println(readResponse.getData().toStringUtf8());

        channel.shutdown();
    }
}
