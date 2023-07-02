package edu.cmu.reedsolomonfs;

public class ConfigurationVariables {
    public static final int MB = 1000000; // Mega number of bytes
    public static final int BLOCK_SIZE = 4; // number of bytes in one block
    public static final int DATA_SHARD_COUNT = 4; // number of data disks in RSFS
    public static final int PARITY_SHARD_COUNT = 2; // number of parity disks in RSFS
    public static final int TOTAL_SHARD_COUNT = DATA_SHARD_COUNT + PARITY_SHARD_COUNT; // total number of disks in RSFS
    public static final int FILE_SIZE_MULTIPLE = DATA_SHARD_COUNT * BLOCK_SIZE;
}
