package edu.cmu.reedsolomonfs.server.Master;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import edu.cmu.reedsolomonfs.ConfigVariables;
import com.alipay.sofa.jraft.conf.Configuration;
// import edu.cmu.reedsolomonfs.client.RecoveryServiceGrpc;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.GRPCMetadata;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.TokenRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.TokenResponse;
import edu.cmu.reedsolomonfs.datatype.Node;
import edu.cmu.reedsolomonfs.server.RecoveryServiceGrpc;
import edu.cmu.reedsolomonfs.server.Chunkserver.ChunkserverDiskRecoveryMachine;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.ChunkserverGrpcHelper;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryReadResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.RecoveryWriteResponse;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.GRPCNode;
// import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadRequest;
// import edu.cmu.reedsolomonfs.client.Reedsolomonfs.RecoveryReadResponse;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter.Magenta;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.google.protobuf.ByteString;

import java.util.HashMap;

public class Master {

    private static Map<String, List<Node>> metadata;
    private static MasterImpl masterImpl;
    public static CliClientServiceImpl cliClientService;
    public static String groupId;
    public static String confStr;

    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        if (args.length != 2) {
            System.out.println("Usage : java com.alipay.sofa.jraft.example.counter.CounterClient {groupId} {conf}");
            System.out
                    .println(
                            "Example: java com.alipay.sofa.jraft.example.counter.CounterClient counter 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083");
            System.exit(1);
        }
        groupId = args[0];
        confStr = args[1];
        ChunkserverGrpcHelper.initGRpc();

        final Configuration conf = new Configuration();
        if (!conf.parse(confStr)) {
            throw new IllegalArgumentException("Fail to parse conf:" + confStr);
        }
        System.out.println(groupId + "????");
        RouteTable.getInstance().updateConfiguration(groupId, conf);

        cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());

        System.setErr(new PrintStream("/dev/null"));
        masterImpl = new MasterImpl(groupId, conf);
        Server server = ServerBuilder.forPort(8080)
                .addService(masterImpl)
                .build()
                .start();

        metadata = new HashMap<>();

        // FileInputStream fileInputStream = new FileInputStream("./as");
        server.awaitTermination();
    }

    public List<Node> getMetadata(String filePath) {
        return metadata.get(filePath);
    }

    public void addMetadata(String filePath, List<Node> nodes) {
        metadata.put(filePath, nodes);
    }

    public void deleteMetadata(String filePath) {
        metadata.remove(filePath);
    }

}
