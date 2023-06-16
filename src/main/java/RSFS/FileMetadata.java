package RSFS;

import java.util.List;

public class FileMetadata {

    private int fileSize;
    private List<Integer> blocks;
    
    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public List<Integer> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Integer> blocks) {
        this.blocks = blocks;
    }

    public FileMetadata(int fileSize, List<Integer> blocks) {
        this.fileSize = fileSize;
        this.blocks = blocks;
    }
}
