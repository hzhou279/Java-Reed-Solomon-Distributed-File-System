package edu.cmu.reedsolomonfs.server.Chunkserver.rpc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.protobuf.ByteString;

import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteResponse;
import edu.cmu.reedsolomonfs.server.RecoveryServiceGrpc.RecoveryServiceImplBase;
import edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver;
import io.grpc.stub.StreamObserver;


/**
 * @author Hongru (hongruz@andrew.cmu.edu) 
 */
public class RecoveryServiceImpl extends RecoveryServiceImplBase {

    private int serverIdx;
    private final Chunkserver chunkServer;
    private String diskPath;
    String outputLogFile = "recovery_output.log";

    public RecoveryServiceImpl(int serverIdx, String diskPath, Chunkserver chunkServer) {
        this.chunkServer = chunkServer;
        this.serverIdx = serverIdx;
        this.diskPath = diskPath;
        redirectSystemOutToFile();
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
        
        System.out.println("RecoveryWriteRequest received from Master");
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
            // print recoveredChunkFileData
            System.out.println("request.getChunkFilePath(): " + new String(request.getChunkFilePath()));
            this.chunkServer.getFsm().updateStoredFileNameToChunks(request.getChunkFilePath());
            System.out.println("recoveredChunkFilePath: " + recoveredChunkFilePath);
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

    public void redirectSystemOutToFile() {
        try {
            // Create a new file output stream for the desired file
            FileOutputStream fileOutputStream = new FileOutputStream(outputLogFile);

            // Create a new print stream that writes to the file output stream
            PrintStream printStream = new PrintStream(fileOutputStream);

            // Redirect System.out to the print stream
            System.setOut(printStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
