package edu.cmu.reedsolomonfs.datatype;
import java.io.Serializable;
import java.util.List;

public class FileMetadata implements Serializable {
    
    private String filePath;
    private int fileSize;
    private int fileVersion;
    private int lastChunkIdx;
    private List<Node> nodes;

    public FileMetadata(String filePath, int fileSize, int fileVersion, List<Node> nodes, int lastChunkIdx) {
        this.filePath = filePath;
        this.fileVersion = fileVersion;
        this.fileSize = fileSize;
        this.nodes = nodes;
        this.lastChunkIdx = lastChunkIdx;
    }

    public int getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(int fileVersion) {
        this.fileVersion = fileVersion;
    }

    public int getLastChunkIdx() {
        return lastChunkIdx;
    }

    public void setLastChunkIdx(int lastChunkIdx) {
        this.lastChunkIdx = lastChunkIdx;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
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
