package edu.cmu.reedsolomonfs.server.Chunkserver.rpc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            Path path = Paths.get(diskPath, filePath);
            byte[] fileContent = Files.readAllBytes(path);
            String content = new String(fileContent);
            System.out.println(content);
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
        }
            
    }
}
