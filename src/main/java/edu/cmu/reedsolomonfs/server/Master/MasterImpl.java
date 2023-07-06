package edu.cmu.reedsolomonfs.server.Master;

import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverDiskRecoveryMachine;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequestResponse;
import io.grpc.stub.StreamObserver;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.impl.LogKitLogger;

public class MasterImpl extends edu.cmu.reedsolomonfs.server.MasterServiceGrpc.MasterServiceImplBase{
    
    boolean storageActivated;
    Map<Integer, Long> currHeartbeat;
    Map<Integer, Long> oldHeartbeat;
    static final long checkInterval = 7000;


    public MasterImpl(){
        storageActivated = false;
        currHeartbeat = new ConcurrentHashMap<Integer, Long>();
        oldHeartbeat = new ConcurrentHashMap<Integer, Long>();
        
        Thread hbc = new heartbeatChecker();
        hbc.start();
    }

    // heartbeat routine 
    private class heartbeatChecker extends Thread {
        
        public void run() {
            while(true) {
                System.out.println("Checking last heartbeat");
                if (storageActivated) {
                    for (Map.Entry<Integer, Long> entry : currHeartbeat.entrySet()) {
                        if (entry.getValue() == oldHeartbeat.get(entry.getKey())) {
                            // timeout
                            System.out.println("heartbeat timeout");
                            break;
                        } 
                        oldHeartbeat.put(entry.getKey(), entry.getValue());
                        System.out.println("Pass timeout check");
                    }
                }
                try {
                    Thread.sleep(checkInterval);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }



    @Override
    public void heartBeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String serverTag = request.getServerTag();

            // storage activated 
            if (!storageActivated) {
                storageActivated = true;
                int chunkservers = 6;
                for (int i = 0; i < chunkservers; i++) {
                    oldHeartbeat.put(i, (long)0);
                }
            } 


            

            System.out.println("Received Heatbeat in: " + timestamp);
            System.out.println(serverTag);
            
            // update last heartbeat timestamp
            currHeartbeat.put(Integer.parseInt(serverTag), System.currentTimeMillis());



            HeartbeatResponse response = HeartbeatResponse.newBuilder().setReceive(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void writeSuccess(ackMasterWriteSuccessRequest request, StreamObserver<ackMasterWriteSuccessRequestResponse> responseObserver) {
        try {

            // log
            System.out.println(request.getFileName());
            System.out.println(request.getFileSize());
            System.out.println(request.getAppendAt());
            System.out.println(request.getWriteFlag());


            ackMasterWriteSuccessRequestResponse response = ackMasterWriteSuccessRequestResponse.newBuilder().setSuccess(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

}
