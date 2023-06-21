package edu.cmu.reedsolomonfs.datatype;
import java.util.List;

public class Metadata {
    
    private String filePath;
    private List<Node> nodes;

    public Metadata(String filePath, List<Node> nodes) {
        this.filePath = filePath;
        this.nodes = nodes;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
}
