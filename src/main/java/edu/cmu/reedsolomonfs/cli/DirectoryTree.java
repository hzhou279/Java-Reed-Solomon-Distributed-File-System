package edu.cmu.reedsolomonfs.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectoryTree {

    private Node root;

    public DirectoryTree() {
        this.root = new Node("/");
    }

    public void addPath(String path) {
        String[] dirs = path.split("/");
        Node node = root;
        for (String dir : dirs) {
            if (!dir.equals("")) {
                if (!node.children.containsKey(dir)) {
                    node.children.put(dir, new Node(dir));
                }
                node = node.children.get(dir);
            }
        }
    }

    public Node getRoot() {
        return root;
    }

    public class Node {
        private String name;
        Map<String, Node> children;
    
        public Node(String name) {
            this.name = name;
            this.children = new HashMap<>();
        }

        public String getName() {
            return name;
        }
    }

    public List<String> listDirectory(String path) {
        String[] dirs = path.split("/");
        Node node = root;
        for (int i = 0; i < dirs.length; i++) {
            if (dirs[i].equals("")) {
                i = i + 1;
            }
            if (!node.children.containsKey(dirs[i])) {
                throw new IllegalArgumentException("Path does not exist in the tree");
            }
            node = node.children.get(dirs[i]);
        }
        
        // Return the names of all children of the final node
        return new ArrayList<>(node.children.keySet());
    }

    public boolean delete(String path) {
        String[] dirs = path.split("/");
        Node node = root;
        for (int i = 0; i < dirs.length - 1; i++) { // stop at the parent node
            if (dirs[i].equals("")) {
                continue;
            }
            if (!node.children.containsKey(dirs[i])) {
                throw new IllegalArgumentException("Path does not exist in the tree");
            }
            node = node.children.get(dirs[i]);
        }
        // At this point, 'node' is the parent of the node to be deleted
        String lastDir = dirs[dirs.length - 1]; // the name of the node to be deleted
        if (!node.children.containsKey(lastDir)) {
            throw new IllegalArgumentException("Path does not exist in the tree");
        }
        node.children.remove(lastDir);
        return true;
    }
}