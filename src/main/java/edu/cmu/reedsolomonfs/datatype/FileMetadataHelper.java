package edu.cmu.reedsolomonfs.datatype;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.reedsolomonfs.ConfigVariables;

/**
 * Helper class to create and update FileMetadata
 * 
 * Some facts about file and metadata:
 * Each time we edit a file, we create a new copy of it with a new metadata
 */
public class FileMetadataHelper {

    /**
     * Create a FileMetaData for a newly created file
     * 
     * @param filePath path including name of the newly created file
     * @param fileSize number of bytes of data in the newly created file (exclude
     *                 padding and parity bytes)
     * @return a packed FileMetadata
     */
    public static FileMetadata createFileMetadata(String filePath, int fileSize) {
        // Create a list of nodes for the new file
        List<Node> nodes = new ArrayList<>();
        createNodes(nodes, 0, fileSize);

        // actual filePath of the newly create file should be "/path/file.txt.0" (just
        // an example)
        // Last chunk(node) index should be the number of nodes minus one
        return new FileMetadata(filePath, fileSize, 0, nodes, nodes.size() - 1);
    }

    /**
     * Update the FileMetadata of the appended file
     * 
     * @param metadata             obsolete FileMetadata of the appended file
     * @param appendedFileDataSize
     * @return the updated FileMetadata
     */
    public static FileMetadata appendFileMetadata(FileMetadata metadata, int appendedFileDataSize) {
        // Get path and size of the updated file
        String filePath = metadata.getFilePath();
        int fileSize = metadata.getFileSize() + appendedFileDataSize;
        int fileVersion = metadata.getFileVersion() + 1;

        List<Node> nodes = metadata.getNodes();
        int lastChunkIdx = metadata.getLastChunkIdx();

        // Deep copy old nodes into updatedNodes
        List<Node> updatedNodes = new ArrayList<>();
        for (Node node : nodes) {
            Node updatedNode = new Node(node.getChunkIdx(), node.getServerId(), node.getIsData(), node.getDataSize());
            updatedNodes.add(updatedNode);
        }

        // Create new nodes for appended data
        lastChunkIdx = createNodes(updatedNodes, lastChunkIdx + 1, appendedFileDataSize);

        // Create the new FileMetadata
        return new FileMetadata(filePath, fileSize, fileVersion, updatedNodes, lastChunkIdx);
    }

    /**
     * Create a list of nodes when appending fileSize of data to a given file
     * 
     * @param startChunkIdx the first index of the next node created
     * @param fileSize total number of data bytes being written as nodes
     * @return lastChunkIdx
     */
    private static int createNodes(List<Node> nodes, int startChunkIdx, int fileSize) {
        // Calculate number of bytes of data and padding for the file
        // (exclude parity bytes)
        int paddedFileSize = fileSize / ConfigVariables.FILE_SIZE_MULTIPLE
                * ConfigVariables.FILE_SIZE_MULTIPLE + ConfigVariables.FILE_SIZE_MULTIPLE;

        // Calculate the total number of nodes(chunks stored in chunkservers) of the new
        // file (include nodes for parity bytes)
        int nodeCnt = paddedFileSize / ConfigVariables.BLOCK_SIZE / ConfigVariables.DATA_SHARD_COUNT
                * ConfigVariables.TOTAL_SHARD_COUNT;

        // Calculate the number of nodes that are full of file data (with no padding
        // bytes)
        int fullDataNodeCnt = fileSize / ConfigVariables.BLOCK_SIZE;

        // Create each node
        int chunkIdx = startChunkIdx;
        for (int serverIdx = 0; chunkIdx < nodeCnt; chunkIdx++, serverIdx++) {

            // Nodes stored in parity disks should also be considered
            serverIdx %= ConfigVariables.TOTAL_SHARD_COUNT;

            // Parity disks are indexed after data disks
            boolean isData = serverIdx >= ConfigVariables.DATA_SHARD_COUNT ? false : true;

            // Calculate the number of data bytes written in this node (chunk)
            // If current node index is chunkIdx, we know there are chunkIdx number of node
            // being written. Thus, any remaining bytes of data (not padding bytes) should
            // be
            // fileSize - chunkIdx * ConfigurationVariables.BLOCK_SIZE. In case that if
            // current
            // node is a node full of padding bytes, fileSize will be smaller than
            // chunkIdx * ConfigurationVariables.BLOCK_SIZE, which results in a negative
            // number.
            int dataSize = ConfigVariables.BLOCK_SIZE;
            if (chunkIdx >= fullDataNodeCnt)
                dataSize = Math.max(fileSize - chunkIdx * ConfigVariables.BLOCK_SIZE, 0);

            Node node = new Node(chunkIdx, serverIdx, isData, dataSize);
            nodes.add(node);
        }
        return chunkIdx - 1;
    }

    /**
     * Parse a given FileMetadata and retrieve the file chunk names (paths)
     * corresponding to
     * current server id
     * 
     * @param metadata  file metadata to be parsed
     * @param serverIdx server index or id
     * @return fileChunkPaths a list of file chunk name used to retrieve from
     *         current
     *         chunk server disk
     */
    public static List<String> retrieveFileChunkPaths(FileMetadata metadata, int serverIdx) {

        List<String> fileChunkPaths = new ArrayList<>();
        String filePath = metadata.getFilePath();
        int fileVersion = metadata.getFileVersion();
        List<Node> nodes = metadata.getNodes();

        // The format of file chunk name: filePath.fileVersion-chunkIdx
        for (Node node : nodes) {
            if (node.getServerId() != serverIdx)
                continue;
            StringBuilder sb = new StringBuilder(filePath);
            sb.append('.');
            sb.append(fileVersion);
            sb.append('-');
            sb.append(node.getChunkIdx());
            fileChunkPaths.add(sb.toString());
        }

        return fileChunkPaths;
    }

}
