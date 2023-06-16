package RSFS;
import RSFS.ReedSolomon.*;
import java.util.Arrays;
import java.util.Random;

/**
 * Hello world!
 *
 */
public class SimpleEncoderAndDecoder {

    private static final int BLOCK_SIZE = 4; // number of bytes in one block
    private static final int DATA_SHARD_COUNT = 4; // number of data disks in RSFS
    private static final int PARITY_SHARD_COUNT = 2; // number of parity disks in RSFS
    private static final int TOTAL_SHARD_COUNT = DATA_SHARD_COUNT + PARITY_SHARD_COUNT; // total number of disks in RSFS
    private static final int RANDOM_DATA_SIZE = 21; // number of bytes in randomly generated file

    public static void main(String[] args) {
        ReedSolomon reedSolomon = ReedSolomon.create(DATA_SHARD_COUNT, PARITY_SHARD_COUNT);
        byte[] file = generateData(RANDOM_DATA_SIZE);
        byte[][] shards = splitFileToShards(file);
        reedSolomon.encodeParity(shards, 0, shards[0].length);
        String originalData = new String(file);

        // check parity calculated is correct
        byte[] tempBuffer = new byte[shards[0].length];
        if (!reedSolomon.isParityCorrect(shards, 0, BLOCK_SIZE, tempBuffer)) System.out.println("Parity not correct." + "\n");

        // drop some shards
        byte[][] incompleteShards = new byte[shards.length][shards[0].length];
        boolean[] shardPresent = generateRandomMissingShardsIndex();
        for (int i = 0; i < shards.length; i++) {
            if (shardPresent[i]) incompleteShards[i] = Arrays.copyOf(shards[i], shards[i].length);
            else Arrays.fill(incompleteShards[i], (byte)1);
        }
        byte[] incompleteFile = mergeShardsToFile(incompleteShards);
        if (Arrays.equals(file, incompleteFile)) System.out.println("Incomplete file generation failed.." + "\n");
        String incompleteData = new String(incompleteFile);
        System.out.println("Original file is: " + originalData + "\n");
        System.out.println("Incomplete file is: " + incompleteData + "\n");

        // decode incomplete shards to recover the original shards
        reedSolomon.decodeMissing(incompleteShards, shardPresent, 0, incompleteShards[0].length);
        byte[] recoveredFile = mergeShardsToFile(incompleteShards);
        if (!Arrays.equals(file, recoveredFile)) System.out.println("Incomplete file recovery failed." + "\n");
        String recoveredData = new String(originalData);
        System.out.println("Recovered file is: " + recoveredData + "\n");
    }

    public static byte[] mergeShardsToFile(byte[][] shards) {
        byte[] file = new byte[RANDOM_DATA_SIZE];
        int idx = 0; // pointer to current read byte in file
        for (int i = 0; i < DATA_SHARD_COUNT; i++) {
            for (int j = 0; j < shards[i].length; j++)
                 file[idx++] = shards[i][j];
        }
        return file;
    }
    
    public static byte[][] splitFileToShards(byte[] file) {
        byte[][] shards = new byte[TOTAL_SHARD_COUNT][file.length / 4];
        int idx = 0; // pointer to current read byte in file
        for (int i = 0; i < DATA_SHARD_COUNT; i++) {
            for (int j = 0; j < file.length / 4; j++)
                shards[i][j] = file[idx++];
        }
        return shards;
    }

    public static byte[] generateData(int arraySize) {
        // Create an instance of Random
        Random random = new Random();

        // Create a byte array with the specified size
        byte[] byteArray = new byte[arraySize];

        // Fill the byte array with random bytes
        random.nextBytes(byteArray);

        return byteArray;
    }

    public static boolean[] generateRandomMissingShardsIndex() {
        boolean[] shardPresent = new boolean[TOTAL_SHARD_COUNT];
        Arrays.fill(shardPresent, true);
        int maxValue = TOTAL_SHARD_COUNT - 1;
        int minValue = 0;
        Random random = new Random();
        for (int i = 0; i < PARITY_SHARD_COUNT; i++) {
            int idx = random.nextInt(maxValue - minValue + 1) + minValue;
            while (!shardPresent[idx])
                idx = random.nextInt(maxValue - minValue + 1) + minValue;
            shardPresent[idx] = false;
        }
        return shardPresent;
    }
}
