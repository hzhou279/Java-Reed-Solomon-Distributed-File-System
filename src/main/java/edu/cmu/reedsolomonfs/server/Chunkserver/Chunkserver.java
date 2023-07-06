/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.cmu.reedsolomonfs.server.Chunkserver;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;

import edu.cmu.reedsolomonfs.server.ChunkserverOutter;
import edu.cmu.reedsolomonfs.server.MasterServiceGrpc;
import edu.cmu.reedsolomonfs.server.MasterserverOutter;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.ChunkserverGrpcHelper;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.GetValueRequestProcessor;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.IncrementAndGetRequestProcessor;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.ReadRequestProcessor;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.RecoveryServiceImpl;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.SetBytesValueRequestProcessor;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.WriteRequestProcessor;
import edu.cmu.reedsolomonfs.server.ChunkserverOutter.ValueResponse;
import edu.cmu.reedsolomonfs.server.ChunkserverOutter.ValueResponse.Builder;
import edu.cmu.reedsolomonfs.server.MasterServiceGrpc.MasterServiceBlockingStub;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.HeartbeatRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.ackMasterWriteSuccessRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Counter server that keeps a counter value in a raft group.
 *
 * @author boyan (boyan@alibaba-inc.com)
 *         <p>
 *         2018-Apr-09 4:51:02 PM
 */
public class Chunkserver {

    private RaftGroupService raftGroupService;
    private Node node;
    private ChunkserverStateMachine fsm;
    private int serverIdx;
    private ManagedChannel channel;

    // line 88 to line 99 follows server setting example in SOFAJaft documentation
    // https://www.sofastack.tech/en/projects/sofa-jraft/jraft-user-guide/
    public Chunkserver(final String dataPath, final String groupId, final PeerId serverId,
            final NodeOptions nodeOptions, int serverIdx) throws IOException {
        // init raft data path, it contains log,meta,snapshot
        FileUtils.forceMkdir(new File(dataPath));

        // here use same RPC server for raft and business. It also can be seperated
        // generally
        final RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
        // GrpcServer need init marshaller
        ChunkserverGrpcHelper.initGRpc();
        ChunkserverGrpcHelper.setRpcServer(rpcServer);

        // register business processor
        channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext() // Use insecure connection, for testing only
                .build();
        heartbeatThread hbt = new heartbeatThread(channel, rpcServer, serverIdx);
        hbt.start();
        ChunkserverService counterService = new ChunkserverServiceImpl(this);
        rpcServer.registerProcessor(new GetValueRequestProcessor(counterService));
        rpcServer.registerProcessor(new IncrementAndGetRequestProcessor(counterService));
        rpcServer.registerProcessor(new SetBytesValueRequestProcessor(counterService));
        rpcServer.registerProcessor(new WriteRequestProcessor(counterService, channel));
        rpcServer.registerProcessor(new ReadRequestProcessor(counterService));

        // record the server index
        this.serverIdx = serverIdx;
        // init state machine
        this.fsm = new ChunkserverStateMachine(serverIdx);
        // set fsm to nodeOptions
        nodeOptions.setFsm(this.fsm);
        // set storage path (log,meta,snapshot)
        // log, must
        nodeOptions.setLogUri(dataPath + File.separator + "log");
        // meta, must
        nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta");
        // snapshot, optional, generally recommended
        nodeOptions.setSnapshotUri(dataPath + File.separator + "snapshot");
        // init raft group service framework
        this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer);
        // start raft node
        this.node = this.raftGroupService.start();
    }

    // heartbeat routine
    private class heartbeatThread extends Thread {
        // TODO Add Retry Mechanism
        private ManagedChannel channel;
        private RpcServer rpcServer;
        private int serverIdx;

        public heartbeatThread(ManagedChannel channel, RpcServer rpcServer, int serverIdx) {
            this.channel = channel;
            this.rpcServer = rpcServer;
            this.serverIdx = serverIdx;
        }

        public void run() {
            while (true) {
                MasterServiceGrpc.MasterServiceBlockingStub stub = MasterServiceGrpc.newBlockingStub(channel);
                HeartbeatRequest hb = HeartbeatRequest.newBuilder().setServerTag(String.valueOf(serverIdx)).build();
                stub.heartBeat(hb);
                try {
                    sleep(5000); // heartbeat interval
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private static void startRecoveryServer(int serverIdx, String diskPath) throws Exception {
        // Create a new thread for running the server
        Thread recoveryServerThread = new Thread(() -> {
            try {
                // Create a gRPC server using ServerBuilder
                Server server = ServerBuilder.forPort(18000 + serverIdx)
                        .addService(new RecoveryServiceImpl(serverIdx, diskPath)) // Add your service implementation
                        .build();

                // Start the server
                server.start();
                System.out.println("Server starteds on port " + (18000 + serverIdx));

                // Block the server thread to keep the server running
                server.awaitTermination();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Start the server thread
        recoveryServerThread.start();
    }

    public int getServerIdx() {
        return this.serverIdx;
    }

    public ChunkserverStateMachine getFsm() {
        return this.fsm;
    }

    public Node getNode() {
        return this.node;
    }

    public RaftGroupService RaftGroupService() {
        return this.raftGroupService;
    }

    /**
     * Redirect request to new leader
     */
    public ValueResponse redirect() {
        final ValueResponse.Builder builder = ValueResponse.newBuilder().setSuccess(false);
        if (this.node != null) {
            final PeerId leader = this.node.getLeaderId();
            if (leader != null) {
                builder.setRedirect(leader.toString());
            }
        }
        return builder.build();
    }

    // the following main function used the raft server example code provided in
    // SOFAJRaft documentation
    // https://www.sofastack.tech/en/projects/sofa-jraft/jraft-user-guide/
    public static void main(final String[] args) throws NumberFormatException, Exception {
        if (args.length != 5) {
            System.out
                    .println(
                            "Usage : java com.alipay.sofa.jraft.example.counter.CounterServer {dataPath} {groupId} {serverId} {initConf} {serverIdx}");
            System.out
                    .println(
                            "Example: java com.alipay.sofa.jraft.example.counter.CounterServer /tmp/server1 counter 127.0.0.1:8081 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083 0");
            System.exit(1);
        }
        final String dataPath = args[0];
        final String groupId = args[1];
        final String serverIdStr = args[2];
        final String initConfStr = args[3];
        final String serverIdx = args[4];

        final NodeOptions nodeOptions = new NodeOptions();
        // for test, modify some params
        // set election timeout to 1s
        nodeOptions.setElectionTimeoutMs(1000);
        // disable CLI service。
        nodeOptions.setDisableCli(false);
        // do snapshot every 30s
        nodeOptions.setSnapshotIntervalSecs(30);
        // parse server address
        final PeerId serverId = new PeerId();
        if (!serverId.parse(serverIdStr)) {
            throw new IllegalArgumentException("Fail to parse serverId:" + serverIdStr);
        }
        final Configuration initConf = new Configuration();
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("Fail to parse initConf:" + initConfStr);
        }
        // set cluster configuration
        nodeOptions.setInitialConf(initConf);

        // start raft server
        final Chunkserver counterServer = new Chunkserver(dataPath, groupId, serverId, nodeOptions,
                Integer.parseInt(serverIdx));
        System.out.println("Started counter server at port:"
                + counterServer.getNode().getNodeId().getPeerId().getPort());

        // start the recovery thread
        String diskPath = "./ClientClusterCommTestFiles/Disks/chunkserver-" + serverIdx + "/";
        startRecoveryServer(Integer.parseInt(serverIdx), diskPath);

        // GrpcServer need block to prevent process exit
        ChunkserverGrpcHelper.blockUntilShutdown();
    }
}
