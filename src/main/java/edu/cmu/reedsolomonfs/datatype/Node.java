package edu.cmu.reedsolomonfs.datatype;

import java.io.Serializable;

public class Node implements Serializable {

    private int chunkIdx; // sequence number of current node in the file
    private int serverId; // where the node is stored
    private boolean isData; // flag to indicate whether current node is a data block or a parity block
    private int dataSize; // it is possible there exists padding data in this node, dataSize stands for
                          // non-padding data in this node

    public Node(int chunkIdx, int serverId, boolean isData, int dataSize) {
        this.chunkIdx = chunkIdx;
        this.serverId = serverId;
        this.isData = isData;
        this.dataSize = dataSize;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
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

    public boolean getIsData() {
        return isData;
    }

    public void setIsData(boolean isData) {
        this.isData = isData;
    }
}
