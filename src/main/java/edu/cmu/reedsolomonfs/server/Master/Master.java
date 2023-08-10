package edu.cmu.reedsolomonfs.server.Master;

import java.io.IOException;
import java.util.List;
import com.alipay.sofa.jraft.conf.Configuration;
import edu.cmu.reedsolomonfs.datatype.Node;
import edu.cmu.reedsolomonfs.server.Chunkserver.rpc.ChunkserverGrpcHelper;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.util.Map;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;

import java.util.HashMap;


public class Master {

    private static Map<String, List<Node>> metadata;
    private static MasterImpl masterImpl;
    public static CliClientServiceImpl cliClientService;
    public static String groupId;
    public static String confStr;
    
    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        
        // Reference: line 28 to line 35 follows jRaft documentation to set up raft group client
        // https://www.sofastack.tech/projects/sofa-jraft/counter-example/
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

        // Reference: line 52 to line 56 follows gRPC documentation to set up gRPC server
        // https://grpc.io/docs/languages/java/basics/#server
        masterImpl = new MasterImpl(groupId, conf);
        Server server = ServerBuilder.forPort(8080)
                .addService(masterImpl)
                .build()
                .start();

        metadata = new HashMap<>();
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
