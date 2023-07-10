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
        System.out.println(dirs.length);
        for (String dir : dirs) {
            if (!dir.equals("")) {
                if (!node.children.containsKey(dir)) {
                    node.children.put(dir, new Node(dir));
                    System.out.println(dir);
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
}