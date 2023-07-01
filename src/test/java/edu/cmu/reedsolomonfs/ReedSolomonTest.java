package edu.cmu.reedsolomonfs;

import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.cmu.reedsolomonfs.client.ReedSolomonDecoder;
import edu.cmu.reedsolomonfs.client.ReedSolomonEncoder;

import java.util.Arrays;
import java.util.Random;

/**
 * Unit test for simple App.
 */
public class ReedSolomonTest {

    private final static String[] ALL_DISK_PATHS = {"./Disks/DataDiskOne.txt", "./Disks/DataDiskTwo.txt", "./Disks/DataDiskThree.txt"
,"./Disks/DataDiskFour.txt", "./Disks/ParityDiskOne.txt", "./Disks/ParityDiskTwo.txt"};
    private final static String FILE_PATH = "./Files/test.txt";
    private final static String FILE_READ_PATH = "./FilesRead/test.txt";
    private final static int MB = 1000000;
    private final static int FILE_SIZE = 1 * MB;
    private static ReedSolomonEncoder encoder;
    private static ReedSolomonDecoder decoder;

    @BeforeClass
    public static void setUp() {
        byte[] fileData = generateRandomFileData(FILE_SIZE);
        writeRandomFileData(fileData);
        try {
            encoder = new ReedSolomonEncoder(FILE_PATH, ALL_DISK_PATHS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        decoder = new ReedSolomonDecoder(FILE_READ_PATH, ALL_DISK_PATHS, FILE_SIZE);
    }

    @Test
    public void testBasicEncodingAndDecoding() {
        encoder.encode();
        encoder.store();
        decoder.decode();
        decoder.store();
        assertTrue(Arrays.equals(encoder.getFileData(), decoder.getFileData()));
    }

    public static byte[] generateRandomFileData(int fileSize) {
        byte[] fileData = new byte[fileSize];
        // Generate random byte values
        new Random().nextBytes(fileData);
        return fileData;
    }

    public static void writeRandomFileData(byte[] fileData) {
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            fos.write(fileData); // Write the byte data to the file
            System.out.println("Byte data stored in read file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
