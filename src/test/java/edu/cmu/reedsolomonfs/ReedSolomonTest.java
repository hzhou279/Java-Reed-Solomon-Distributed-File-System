package edu.cmu.reedsolomonfs;

import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
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

    private final static String TEST_FILES_DIR = "./ReedSolomonTestFiles";

    private final static String[] DIR_PATHS = { TEST_FILES_DIR + "/Disks", TEST_FILES_DIR + "/Files",
            TEST_FILES_DIR + "/FilesRead" };

    private final static String[] ALL_DISK_PATHS = { TEST_FILES_DIR + "/Disks/DataDiskOne.txt",
            TEST_FILES_DIR + "/Disks/DataDiskTwo.txt", TEST_FILES_DIR + "/Disks/DataDiskThree.txt",
            TEST_FILES_DIR + "/Disks/DataDiskFour.txt", TEST_FILES_DIR + "/Disks/ParityDiskOne.txt",
            TEST_FILES_DIR + "/Disks/ParityDiskTwo.txt" };
    private final static String FILE_PATH = TEST_FILES_DIR + "/Files/test.txt";
    private final static String FILE_READ_PATH = TEST_FILES_DIR + "/FilesRead/test.txt";
    private final static int MB = 1000000;
    private final static int KB = 1000;
    private final static int FILE_SIZE = 200 * MB;
    private static ReedSolomonEncoder encoder;
    private static ReedSolomonDecoder decoder;

    @BeforeClass
    public static void setUpFileData() {
        for (String directoryPath : DIR_PATHS) {
            Path directory = Paths.get(directoryPath);
            try {
                Files.createDirectories(directory); // Create the directory and any nonexistent parent directories
                // System.out.println("Directory created successfully.");
            } catch (IOException e) {
                System.out.println("Failed to create the directory: " + e.getMessage());
            }
        }
        byte[] fileData = generateRandomFileData(FILE_SIZE);
        writeRandomFileData(fileData);
    }

    @Before
    public void setUp() {
        try {
            encoder = new ReedSolomonEncoder(FILE_PATH, ALL_DISK_PATHS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // decoder = new ReedSolomonDecoder(FILE_READ_PATH, ALL_DISK_PATHS, FILE_SIZE);
        encoder.encode();
        encoder.store();
    }

    @Test
    public void testBasicEncodingAndDecoding() {
        decoder = new ReedSolomonDecoder(FILE_READ_PATH, ALL_DISK_PATHS, FILE_SIZE);
        decoder.decode();
        assertTrue(Arrays.equals(encoder.getFileData(), decoder.getFileData()));
    }

    @Test
    public void testDecodeMissingShards() {
        String[] filePathsMissing = new String[] { TEST_FILES_DIR + "/Disks/ParityDiskTwo.txt",
                TEST_FILES_DIR + "/Disks/DataDiskOne.txt" };
        for (String filePath : filePathsMissing) {
            Path path = Paths.get(filePath);
            try {
                Files.delete(path);
                // System.out.println("File deleted permanently.");
            } catch (IOException e) {
                System.out.println("Failed to delete the file: " + e.getMessage());
            }
        }
        decoder = new ReedSolomonDecoder(FILE_READ_PATH, ALL_DISK_PATHS, FILE_SIZE);
        decoder.decode();
        assertTrue(Arrays.equals(encoder.getFileData(), decoder.getFileData()));
    }

    public void compareDecodedAndOriginalFile() {
        try {
            decoder.decode();
            decoder.store();
            byte[] originalFileData = Files.readAllBytes(Path.of(FILE_PATH));
            byte[] decodedFileData = Files.readAllBytes(Path.of(FILE_READ_PATH));
            assertTrue(Arrays.equals(originalFileData, decodedFileData));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            // System.out.println("Byte data stored in read file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
