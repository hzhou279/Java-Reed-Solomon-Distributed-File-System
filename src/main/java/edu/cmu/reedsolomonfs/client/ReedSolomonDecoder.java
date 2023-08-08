package edu.cmu.reedsolomonfs.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import edu.cmu.reedsolomon.ReedSolomon;
import edu.cmu.reedsolomonfs.ConfigVariables;

// read data from disks
// do recovery if any disks are missing
public class ReedSolomonDecoder {

    private static final ReedSolomon REED_SOLOMON = ReedSolomon.create(ConfigVariables.DATA_SHARD_COUNT,
            ConfigVariables.PARITY_SHARD_COUNT);

    private String[] diskPaths;
    private byte[][] shards;
    private boolean[] shardPresent;
    private byte[] fileData;
    private String filePath; // destination file path
    private int byteCntInShard;
    private int fileSize;

    public byte[] getFileData() {
        return fileData;
    }

    public ReedSolomonDecoder(byte[][] shards, boolean[] shardPresent, int byteCntInShard, int fileSize) {
        // this.shards = shards;
        // this.shardPresent = shardPresent;
        // this.byteCntInShard = byteCntInShard;
        this.fileSize = fileSize;
        // System.out.println("shardPresent: " + shardPresent);
        REED_SOLOMON.decodeMissing(shards, shardPresent, 0, byteCntInShard);
        fileData = mergeShardsToFile(shards);
        trimPadding();
    }

    public ReedSolomonDecoder(String filePath, String[] diskPaths, int fileSize) {
        this.diskPaths = diskPaths;
        shards = new byte[ConfigVariables.TOTAL_SHARD_COUNT][];
        shardPresent = new boolean[ConfigVariables.TOTAL_SHARD_COUNT];
        byteCntInShard = 0;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public void retrieveShards() {
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
            try {
                shards[i] = Files.readAllBytes(Path.of(diskPaths[i]));
                shardPresent[i] = true;
                byteCntInShard = shards[i].length;
                // System.out.println(shards[i].length);
            } catch (IOException e) {
                continue;
            }
        }
    }

    public void trimPadding() {
        byte[] trimmedFileData = new byte[fileSize];
        System.arraycopy(fileData, 0, trimmedFileData, 0, fileSize);
        fileData = trimmedFileData;
    }

    public void decode() {
        retrieveShards();
        if (byteCntInShard == 0)
            throw new IllegalArgumentException("There is not enough data to decode");
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
            if (shards[i] == null)
                shards[i] = new byte[byteCntInShard];
        }
        REED_SOLOMON.decodeMissing(shards, shardPresent, 0, byteCntInShard);
        fileData = mergeShardsToFile(shards);
        trimPadding();
    }

    public void store() {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(fileData); // Write the byte data to the file
            System.out.println("Byte data stored in read file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] mergeShardsToFile(byte[][] shards) {
        byte[] fileData = new byte[shards[0].length * ConfigVariables.DATA_SHARD_COUNT];
        int blockCnt = fileData.length / ConfigVariables.BLOCK_SIZE;
        for (int blockIdx = 0; blockIdx < blockCnt; blockIdx++) {
            int byteIdxInFile = blockIdx * ConfigVariables.BLOCK_SIZE;
            int shardIdx = blockIdx % ConfigVariables.DATA_SHARD_COUNT;
            // int byteIdxInShard = blockIdx / ConfigurationVariables.BLOCK_SIZE *
            // ConfigurationVariables.BLOCK_SIZE;
            int byteIdxInShard = blockIdx / ConfigVariables.DATA_SHARD_COUNT * ConfigVariables.BLOCK_SIZE;
            for (int i = 0; i < ConfigVariables.BLOCK_SIZE; i++, byteIdxInFile++, byteIdxInShard++)
                fileData[byteIdxInFile] = shards[shardIdx][byteIdxInShard];
        }
        return fileData;
    }
}