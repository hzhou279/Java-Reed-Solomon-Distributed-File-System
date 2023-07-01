package edu.cmu.reedsolomonfs.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import edu.cmu.reedsolomon.ReedSolomon;
import edu.cmu.reedsolomonfs.ConfigurationVariables;

// read data from a file and calculate its parity
public class ReedSolomonEncoder {
    
    private static final ReedSolomon REED_SOLOMON = ReedSolomon.create(ConfigurationVariables.DATA_SHARD_COUNT, ConfigurationVariables.PARITY_SHARD_COUNT);

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

    public void store() {
        for (int i = 0; i < ConfigurationVariables.TOTAL_SHARD_COUNT; i++) {
            try (FileOutputStream fos = new FileOutputStream(diskPaths[i])) {
                fos.write(shards[i]); // Write the byte data to the file
                System.out.println("Byte data stored in disk successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void encode()  {
        paddedFileData = pad(fileData);
        shards = splitFileToShards(paddedFileData);
        REED_SOLOMON.encodeParity(shards, 0, shards[0].length);
    }

    private byte[][] splitFileToShards(byte[] fileData) {
        byte[][] shards = new byte[ConfigurationVariables.TOTAL_SHARD_COUNT][fileData.length / ConfigurationVariables.DATA_SHARD_COUNT];
        int blockCnt = fileData.length / ConfigurationVariables.BLOCK_SIZE;
        System.out.println("file size is: " + this.fileSize);
        System.out.println("padded file size is: " + fileData.length);
        System.out.println("blockCnt: " + blockCnt);
        for (int blockIdx = 0; blockIdx < blockCnt; blockIdx++) {
            int byteIdxInFile = blockIdx * ConfigurationVariables.BLOCK_SIZE;
            int shardIdx = blockIdx % ConfigurationVariables.DATA_SHARD_COUNT;
            // int byteIdxInShard = blockIdx / ConfigurationVariables.BLOCK_SIZE * ConfigurationVariables.BLOCK_SIZE;
            int byteIdxInShard = blockIdx / ConfigurationVariables.DATA_SHARD_COUNT * ConfigurationVariables.BLOCK_SIZE;
            for (int i = 0; i < ConfigurationVariables.BLOCK_SIZE; i++, byteIdxInFile++, byteIdxInShard++)
                shards[shardIdx][byteIdxInShard] = fileData[byteIdxInFile];
        }
        lastChunkIdx = blockCnt - 1;
        return shards;
    }

    private byte[] pad(byte[] fileData) {
        if (fileData.length % ConfigurationVariables.FILE_SIZE_MULTIPLE == 0) return fileData;
        int paddedfileSize = fileData.length / ConfigurationVariables.FILE_SIZE_MULTIPLE * ConfigurationVariables.FILE_SIZE_MULTIPLE + ConfigurationVariables.FILE_SIZE_MULTIPLE;
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