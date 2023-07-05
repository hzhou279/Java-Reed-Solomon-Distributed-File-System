package edu.cmu.reedsolomonfs.datatype;

import edu.cmu.reedsolomonfs.ConfigVariables;

public class NodeHelper {

    /**
     * Given a data shard, split it into multiple data blocks (chunks or nodes)
     * @param shard
     * @return
     */
    public static byte[][] splitShardToChunks(byte[] shard) {
        int chunkCnt = shard.length / ConfigVariables.BLOCK_SIZE;
        byte[][] chunks = new byte[chunkCnt][ConfigVariables.BLOCK_SIZE];
        int idxInShard = 0;
        for (int i = 0; i < chunkCnt; i++) {
            for (int j = 0; j < ConfigVariables.BLOCK_SIZE; j++)
                chunks[i][j] = shard[idxInShard++];
        }
        return chunks;
    }
}
