package edu.cmu.reedsolomonfs;

import java.io.File;

public class TempTestHelper {
    public static void main(String[] args) {
        String directoryPath = "./ClientClusterCommTestFiles/Disks";
        deleteDirectory(new File(directoryPath));
    }

    public static void deleteDirectory(File directory) {
        if (!directory.exists()) {
            System.out.println("Directory does not exist.");
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }

        // Delete the empty directory
        directory.delete();
        System.out.println("Directory deleted successfully.");
    }
}
