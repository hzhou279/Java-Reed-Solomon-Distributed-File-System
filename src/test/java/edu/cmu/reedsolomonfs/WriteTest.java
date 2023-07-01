package edu.cmu.reedsolomonfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Test;

import edu.cmu.reedsolomonfs.client.ReedSolomonDecoder;

public class WriteTest {

    private final String[] ALL_DISK_PATHS = {"./Disks/chunkserver-0.txt", "./Disks/chunkserver-1.txt", "./Disks/chunkserver-2.txt"};
    private final String FILE_PATH = "./Files/test.txt";
    private final String FILE_READ_PATH = "./MergedFiles/test.txt";

    @Test
    public void testChunkServerWrite() throws IOException {
        byte[] fileData = Files.readAllBytes(Path.of(FILE_PATH));
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(FILE_READ_PATH, ALL_DISK_PATHS, fileData.length);
        decoder.decode();
        decoder.store();
        byte[] fileDataRead = decoder.getFileData();
        assertEquals(fileData.length, fileDataRead.length);
        // for (int i = 0; i < fileData.length; i++) {
        //     if (fileData[i] != fileDataRead[i]) {
        //         System.out.println(i + " characters matched and there are " + fileData.length + " characters in total.");
        //     }
        //     // assertEquals(i + "characters matched", fileData[i], fileDataRead[i]);
        // }
        assertTrue(Arrays.equals(fileData, decoder.getFileData()));
    }
    
}
