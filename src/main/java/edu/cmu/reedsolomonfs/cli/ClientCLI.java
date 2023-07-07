package edu.cmu.reedsolomonfs.cli;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.cmu.reedsolomonfs.cli.DirectoryTree.Node;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ClientCLI implements KeyListener{
    public static String localPath = "/A";
    public static String[] lowerDirectory = {"direcory1", "directory2", "file1", "file2"};
    public static String[] upperDirectory;
    static Map<String, LinkedList<Integer>> metaData;
    static DirectoryTree tree = new DirectoryTree();
    static Node root;

    public void run() throws ParseException {
        Scanner scanner = new Scanner(System.in);
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        metaData = new HashMap<>();
        
        LinkedList<Integer> list1 = new LinkedList<>(Arrays.asList(1, 2, 3, 4));
        LinkedList<Integer> list2 = new LinkedList<>(Arrays.asList(5, 6, 7));
        LinkedList<Integer> list3 = new LinkedList<>(Arrays.asList(8, 9));
        LinkedList<Integer> list4 = new LinkedList<>(Arrays.asList(8, 9, 10));
        
        metaData.put("A/B/C", list1);
        metaData.put("A/E/M", list2);
        metaData.put("A/E/Z", list3);
        metaData.put("A/E/Z/D", list4);

        for (String path : metaData.keySet()) {
            tree.addPath(path);
        }

        root = tree.getRoot();
        
        options.addOption("cd", "cd", false, "he type of ordering to use, default 'random'");

        while (true) {
            System.out.print(localPath + " % ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("quit")) {
                break;
            }

            String[] words = input.split(" ");
            
            CommandLine line = parser.parse(options, words);

            if (line.hasOption("cd")) {
                System.out.println("change directory");
            } else if (words[0].equals("cd") && !words[1].equals("..")) {
                    Path path = Paths.get(words[1]);
                    if (!path.isAbsolute()) {
                        System.out.println("The path is invalid");
                    } else {
                        if (pathExists(words[1])) {
                            localPath = words[1];
                        } else {
                            System.out.println("Path does not exist!");
                        }      
                    }
            } else if (words[0].equals("cd") && words[1].equals("..")) {
                Path path = Paths.get(localPath);
                Path parentPath = path.getParent();
                if (parentPath != null) {
                    String parentDirectory = parentPath.toString();
                    System.out.println(parentDirectory);
                    localPath = parentDirectory;
                } else {
                    System.out.println("No parent directory found.");
                }
            } else if (words[0].equals("pwd")) {
                System.out.println(localPath);
            } else if (words[0].equals("ls")) {
                List<String> lsList = new ArrayList<>();
                lsList = tree.listDirectory(localPath);
                for (String string : lsList) {
                    System.out.print(string + " ");
                }
                System.out.println();
            } else if (words[0].equals("rm")) {
                System.out.println("REMOVE FILE");
            } else if (words[0].equals("mkdir")) {
                System.out.println("MAKE DIRECTORY");
            } else {
                System.out.println("Invalid Command");
            }
        }

        scanner.close();
        System.out.println("CLI application exited.");
    }

    public String[] findStringsStartingWith(String[] array, char[] startChars) {
        List<String> result = new ArrayList<>();
    
        for (String str : array) {
            char firstChar = str.charAt(0);
            for (char startChar : startChars) {
                if (firstChar == startChar) {
                    result.add(str);
                    break;
                }
            }
        }
    
        return result.toArray(new String[0]);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            System.out.println("I love you");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public static boolean pathExists(String path) {
        String[] dirs = path.split("/");
        Node node = root;
        for (int i = 0; i < dirs.length; i++) {
            String dir = dirs[i];
            if (dir.equals("")) {
                dir = dirs[++i];
            }
            if (!node.children.containsKey(dir) && !dir.equals("")) {
                return false;
            }
            node = node.children.get(dir);
        }
        return true;
    }
}
