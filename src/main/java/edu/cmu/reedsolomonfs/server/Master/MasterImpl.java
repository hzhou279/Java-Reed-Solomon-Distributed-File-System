package edu.cmu.reedsolomonfs.server.Master;

import edu.cmu.reedsolomonfs.server.MasterServiceGrpc;
import edu.cmu.reedsolomonfs.server.MasterserverOutter;
import edu.cmu.reedsolomonfs.server.MasterServiceGrpc.MasterServiceImplBase;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequestResponse;
import io.grpc.stub.StreamObserver;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class MasterImpl extends edu.cmu.reedsolomonfs.server.MasterServiceGrpc.MasterServiceImplBase{
    
    boolean storageActivated;
    Map<Integer, Long> lastHeartbeat;
    static final long TIMEOUT = 6000;


    public MasterImpl(){
        storageActivated = false;
        lastHeartbeat = new HashMap<Integer, Long>();
        Thread hbc = new heartbeatChecker();
        hbc.start();
    }

    // heartbeat routine 
    private class heartbeatChecker extends Thread {
        public void run() {
            while(true) {
                System.out.println("Checking last heartbeat");
                if (storageActivated) {
                    for (Map.Entry<Integer, Long> entry : lastHeartbeat.entrySet()) {
                        System.out.println("Pass timeout check");
                        long current = System.currentTimeMillis();
                        if ((current - entry.getValue()) > TIMEOUT) {
                            // timeout
                            // TODO: Start recovery;
                            break;
                        }
                    }
                }
                try {
                    Thread.sleep(TIMEOUT);
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
            // storage activated 
            if (!storageActivated) {
                storageActivated = true;
            }


            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String serverTag = request.getServerTag();

            System.out.println("Received Heatbeat in: " + timestamp);
            System.out.println(serverTag);
            
            // update last heartbeat timestamp
            lastHeartbeat.put(Integer.parseInt(serverTag), System.currentTimeMillis());


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
