package edu.cmu.reedsolomonfs;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import edu.cmu.reedsolomonfs.client.ReedSolomonDecoder;
import edu.cmu.reedsolomonfs.client.ReedSolomonEncoder;

/**
 * Unit test for simple App.
 */
public class ReedSolomonTest {

    private final String[] ALL_DISK_PATHS = {"./Disks/DataDiskOne.txt", "./Disks/DataDiskTwo.txt", "./Disks/DataDiskThree.txt"
,"./Disks/DataDiskFour.txt", "./Disks/ParityDiskOne.txt", "./Disks/ParityDiskTwo.txt"};
    private final String FILE_PATH = "./Files/test.txt";
    private final String FILE_READ_PATH = "./FilesRead/test.txt";

    @Test
    public void testEncoding() {
        try {
            ReedSolomonEncoder encoder = new ReedSolomonEncoder(FILE_PATH, ALL_DISK_PATHS);
            encoder.encode();
            encoder.store();
        } catch (IOException e) {
            e.printStackTrace();;
        } 
    }

    @Test
    public void testDecoding() {
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(FILE_READ_PATH, ALL_DISK_PATHS);
        decoder.decode();
        decoder.store();
    }
}
