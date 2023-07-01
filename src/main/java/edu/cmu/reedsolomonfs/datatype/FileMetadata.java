package edu.cmu.reedsolomonfs.datatype;
import java.util.List;

public class FileMetadata {
    
    private String filePath;
    private int fileSize;
    private int lastChunkIdx;
    private List<Node> nodes;

    public FileMetadata(String filePath, List<Node> nodes) {
        this.filePath = filePath;
        this.nodes = nodes;
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
