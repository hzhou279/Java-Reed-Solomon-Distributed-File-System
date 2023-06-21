package edu.cmu.reedsolomonfs.datatype;

public class Node {
    
    private int chunkIdx;
    private int serverId;
    private boolean isData;

    public Node(int chunkIdx, int serverId, boolean isData) {
        this.chunkIdx = chunkIdx;
        this.serverId = serverId;
        this.isData = isData;
    }

    public int getChunkIdx() {
        return chunkIdx;
    }

    public void setChunkIdx(int chunkIdx) {
        this.chunkIdx = chunkIdx;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public boolean isData() {
        return isData;
    }

    public void setData(boolean isData) {
        this.isData = isData;
    }
}
