package edu.cmu.reedsolomonfs.cli;

import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ClientCLI {
    public static String localPath = "/root";
    public static void main(String[] args) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        options.addOption("cd", "cd", false, "he type of ordering to use, default 'random'");
        while (true) {
            System.out.print(localPath + " % ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("quit")) {
                break;
            }

            String[] words = input.split(" ");
            
            CommandLine line = parser.parse(options, words);

            for (String m : words)
            System.out.println(m);

            if (line.hasOption("cd")) {
                System.out.println("change directory");
            }


            // print the input.
            System.out.println("You entered: " + input);
        }

        scanner.close();
        System.out.println("CLI application exited.");
    }
}
