package edu.cmu.reedsolomonfs.server.Chunkserver;

import edu.cmu.reedsolomon.ReedSolomon;
import edu.cmu.reedsolomonfs.ConfigVariables;

public class ChunkserverDiskRecoveryMachine {

    private byte[][] chunkserverDisksData;
    private boolean[] chunkserverDiskPresent;
    private int chunkserverDiskPresentCnt;
    private int chunkserverDiskByteCnt;
    private ReedSolomon reedSolomon;

    public ChunkserverDiskRecoveryMachine() {
        chunkserverDisksData = new byte[ConfigVariables.TOTAL_SHARD_COUNT][];
        chunkserverDiskPresent = new boolean[ConfigVariables.TOTAL_SHARD_COUNT];
        chunkserverDiskPresentCnt = 0;
        chunkserverDiskByteCnt = 0;
        reedSolomon = ReedSolomon.create(ConfigVariables.DATA_SHARD_COUNT, ConfigVariables.PARITY_SHARD_COUNT);
    }

    public void addChunkserverDisksData(int serverIdx, byte[] chunkserverDiskData) throws IllegalArgumentException {
        if (serverIdx < 0 || serverIdx >= ConfigVariables.TOTAL_SHARD_COUNT)
            throw new IllegalArgumentException("Given server index does not exist");
        if (chunkserverDiskByteCnt != 0 && chunkserverDiskData.length != chunkserverDiskByteCnt)
            throw new IllegalArgumentException("Number of bytes in different chunkserver disks mismatch");
        chunkserverDiskByteCnt = chunkserverDiskData.length;
        chunkserverDisksData[serverIdx] = new byte[chunkserverDiskByteCnt];
        System.arraycopy(chunkserverDiskData, 0, chunkserverDisksData[serverIdx], 0, chunkserverDiskByteCnt);
        chunkserverDiskPresent[serverIdx] = true;
        chunkserverDiskPresentCnt++;
    }

    public void recoverChunkserverDiskData() throws IllegalArgumentException {
        if (chunkserverDiskPresentCnt < ConfigVariables.DATA_SHARD_COUNT)
            throw new IllegalArgumentException("There is not enough disk data to perform the recovery");
        else if (chunkserverDiskByteCnt == 0)
            throw new IllegalArgumentException("There is no data to recover");
        else if (chunkserverDiskPresentCnt == ConfigVariables.TOTAL_SHARD_COUNT)
            return;
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++)
            if (!chunkserverDiskPresent[i])
                chunkserverDisksData[i] = new byte[chunkserverDiskByteCnt];
        reedSolomon.decodeMissing(chunkserverDisksData, chunkserverDiskPresent, 0, chunkserverDiskByteCnt);
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++)
            if (!chunkserverDiskPresent[i])
                chunkserverDiskPresent[i] = true;
    }

    public byte[] retrieveRecoveredDiskData(int serverIdx) throws IllegalArgumentException {
        if (serverIdx < 0 || serverIdx >= ConfigVariables.TOTAL_SHARD_COUNT)
            throw new IllegalArgumentException("Given server index does not exist");
        if (chunkserverDiskPresent[serverIdx])
            return chunkserverDisksData[serverIdx];
        recoverChunkserverDiskData();
        return chunkserverDisksData[serverIdx];
    }
}
