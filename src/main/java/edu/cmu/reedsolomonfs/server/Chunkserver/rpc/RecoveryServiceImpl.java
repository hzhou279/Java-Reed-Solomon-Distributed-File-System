package edu.cmu.reedsolomonfs.server.Chunkserver.rpc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.protobuf.ByteString;

import edu.cmu.reedsolomonfs.client.RecoveryServiceGrpc.RecoveryServiceImplBase;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadRequest;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadResponse;
import io.grpc.stub.StreamObserver;

public class RecoveryServiceImpl extends RecoveryServiceImplBase {

    private int serverIdx;
    private String diskPath;

    public RecoveryServiceImpl(int serverIdx, String diskPath) {
        this.serverIdx = serverIdx;
        this.diskPath = diskPath;
    }

    @Override
    public void recoveryRead(RecoveryReadRequest request,
            StreamObserver<RecoveryReadResponse> responseObserver) {
        String filePath = request.getFilePath();

        try {
            System.out.println("reach 29 in recoveryservice impl");
            Path path = Paths.get(diskPath, filePath);
            byte[] fileContent = Files.readAllBytes(path);
            // byte[] fileContent = new byte[]{1,1,1};
            // String content = new String(fileContent);
            // System.out.println(content);

            // Prepare the recovery response with the disk path
            RecoveryReadResponse response = RecoveryReadResponse.newBuilder()
                .setChunkFileData(ByteString.copyFrom(fileContent))
                .build();

            // Send the response back to the mask
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
        }

    }
}
