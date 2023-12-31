package edu.cmu.reedsolomonfs.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import edu.cmu.reedsolomon.ReedSolomon;
import edu.cmu.reedsolomonfs.ConfigVariables;

// read data from a file and calculate its parity
public class ReedSolomonEncoder {
    
    // line 17 calls function create at line 29 in ReedSolomon.java
    // https://github.com/Backblaze/JavaReedSolomon/blob/master/src/main/java/com/backblaze/erasure/ReedSolomon.java
    private static final ReedSolomon REED_SOLOMON = ReedSolomon.create(ConfigVariables.DATA_SHARD_COUNT, ConfigVariables.PARITY_SHARD_COUNT);

    private String filePath;
    private byte[] fileData;
    private byte[] paddedFileData;
    private byte[][] shards;
    private int fileSize;
    private String[] diskPaths;
    private int lastChunkIdx;

    public ReedSolomonEncoder(byte[] fileData) {
        this.fileData = fileData;
        fileSize = fileData.length;
    }

    public ReedSolomonEncoder(String filePath, String[] diskPaths) throws IOException {
        this.filePath = filePath;
        fileData = Files.readAllBytes(Path.of(filePath));
        fileSize = fileData.length;
        this.diskPaths = diskPaths;
    }

    public int getPaddedFileSize() {
        return paddedFileData.length;
    }

    public void store() {
        for (int i = 0; i < ConfigVariables.TOTAL_SHARD_COUNT; i++) {
            try (FileOutputStream fos = new FileOutputStream(diskPaths[i])) {
                fos.write(shards[i]); // Write the byte data to the file
                System.out.println("Byte data stored in disk successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // line 59 calls function encodeParity at line 89 in ReedSolomon.java
    // https://github.com/Backblaze/JavaReedSolomon/blob/master/src/main/java/com/backblaze/erasure/ReedSolomon.java
    public void encode()  {
        paddedFileData = pad(fileData);
        shards = splitFileToShards(paddedFileData);
        REED_SOLOMON.encodeParity(shards, 0, shards[0].length);
    }

    private byte[][] splitFileToShards(byte[] fileData) {
        byte[][] shards = new byte[ConfigVariables.TOTAL_SHARD_COUNT][fileData.length / ConfigVariables.DATA_SHARD_COUNT];
        int blockCnt = fileData.length / ConfigVariables.BLOCK_SIZE;
        for (int blockIdx = 0; blockIdx < blockCnt; blockIdx++) {
            int byteIdxInFile = blockIdx * ConfigVariables.BLOCK_SIZE;
            int shardIdx = blockIdx % ConfigVariables.DATA_SHARD_COUNT;
            int byteIdxInShard = blockIdx / ConfigVariables.DATA_SHARD_COUNT * ConfigVariables.BLOCK_SIZE;
            for (int i = 0; i < ConfigVariables.BLOCK_SIZE; i++, byteIdxInFile++, byteIdxInShard++)
                shards[shardIdx][byteIdxInShard] = fileData[byteIdxInFile];
        }
        lastChunkIdx = blockCnt - 1;
        return shards;
    }

    private byte[] pad(byte[] fileData) {
        if (fileData.length % ConfigVariables.FILE_SIZE_MULTIPLE == 0) return fileData;
        int paddedfileSize = fileData.length / ConfigVariables.FILE_SIZE_MULTIPLE * ConfigVariables.FILE_SIZE_MULTIPLE + ConfigVariables.FILE_SIZE_MULTIPLE;
        byte[] paddedFileData = new byte[paddedfileSize];
        Arrays.fill(paddedFileData, (byte) 0);
        int startIndex = 0; // Starting index in destinationArray where sourceArray will be copied
        int length = fileData.length; // Number of elements to be copied
        System.arraycopy(fileData, 0, paddedFileData, startIndex, length);
        return paddedFileData;
    }

    public String getFilePath() {
        return filePath;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public byte[] getPaddedFileData() {
        return paddedFileData;
    }

    public byte[][] getShards() {
        return shards;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getLastChunkIdx() {
        return lastChunkIdx;
    }
}