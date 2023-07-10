package edu.cmu.reedsolomonfs.server.Chunkserver.rpc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.protobuf.ByteString;

import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteResponse;
import edu.cmu.reedsolomonfs.server.RecoveryServiceGrpc.RecoveryServiceImplBase;
// import edu.cmu.reedsolomonfs.client.RecoveryServiceGrpc.RecoveryServiceImplBase;
// import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadRequest;
// import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadResponse;
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
        String chunkFilePath = request.getChunkFilePath();

        try {
            Path path = Paths.get(diskPath, chunkFilePath);
            byte[] fileContent = Files.readAllBytes(path);

            // Prepare the recovery response with the disk path
            RecoveryReadResponse response = RecoveryReadResponse.newBuilder()
                    .setChunkFileData(ByteString.copyFrom(fileContent))
                    .build();

            // Send the response back to the Master
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
        }

    }

    @Override
    public void recoveryWrite(RecoveryWriteRequest request,
            StreamObserver<RecoveryWriteResponse> responseObserver) {
        
        String recoveredChunkFilePath = diskPath + request.getChunkFilePath();
        byte[] recoveredChunkFileData = request.getChunkFileData().toByteArray();
        try (FileOutputStream fos = new FileOutputStream(recoveredChunkFilePath)) {
            fos.write(recoveredChunkFilePath.getBytes()); // Write the recovered data to the recovered file path
            } catch (IOException e) {
            System.err.println("An error occurred while writing the file: " + e.getMessage());
        }

        // Prepare the recovery response with the disk path
        RecoveryWriteResponse response = RecoveryWriteResponse.newBuilder()
                .setRecoveryWriteSuccess(false)
                .build();

        try (FileOutputStream fos = new FileOutputStream(recoveredChunkFilePath)) {
            fos.write(recoveredChunkFileData); // Write the recovered data to the recovered file path
            response = RecoveryWriteResponse.newBuilder()
                    .setRecoveryWriteSuccess(true)
                    .build();
        } catch (IOException e) {
            System.err.println("An error occurred while writing the file: " + e.getMessage());
        }

        // Send the response back to the Master
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
