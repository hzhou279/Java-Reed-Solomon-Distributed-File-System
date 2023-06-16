package edu.cmu.rsfs;

import com.google.protobuf.ByteString;
import edu.cmu.rsfs.Chunkserver.ReadResponse;
import edu.cmu.rsfs.Chunkserver.WriteResponse;
import edu.cmu.rsfs.Chunkserver.WriteRequest;
import edu.cmu.rsfs.Chunkserver.ReadRequest;
import io.grpc.stub.StreamObserver;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class ChunkServiceImpl extends edu.cmu.rsfs.ChunkServiceGrpc.ChunkServiceImplBase {

    private static final String STORAGE_DIR = "path/to/chunks/";

    @Override
    public void writeChunk(WriteRequest request, StreamObserver<WriteResponse> responseObserver) {
        try {
            Path filePath = Paths.get(STORAGE_DIR, request.getChunkId());
            Files.write(filePath, request.getData().toByteArray());

            WriteResponse response = WriteResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void readChunk(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        try {
            Path filePath = Paths.get(STORAGE_DIR, request.getChunkId());
            byte[] data = Files.readAllBytes(filePath);

            ReadResponse response = ReadResponse.newBuilder()
                    .setData(ByteString.copyFrom(data))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
