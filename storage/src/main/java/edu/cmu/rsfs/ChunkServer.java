package edu.cmu.rsfs;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ChunkServer {
    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(8080)
                .addService(new ChunkServiceImpl())
                .build()
                .start();

        server.awaitTermination();
    }
}
