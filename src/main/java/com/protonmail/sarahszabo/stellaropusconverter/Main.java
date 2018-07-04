/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * The main class.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("There is no file specified!\n");
            printHelp();
        } //Direct Link Conversion
        else if (args.length == 1) {
            if (firstArgIsLink(args[0])) {
                StellarMode.DIRECT_LINK.start(args);
            } //Init Graphical File Chooser
            else if (args[0].equalsIgnoreCase("-G")) {
                StellarMode.GRAPHICAL_FILE_CHOICE_DIRECT_LINKS.start(args);
            } //Get From Clipboard
            else if (args[0].equalsIgnoreCase("-CL")) {
                StellarMode.GET_FROM_CLIPBOARD.start(args);
            } else if (args[0].equalsIgnoreCase("Status")) {
                System.out.println(StellarDiskManager.DISKMANAGER.getState());
            } else if (args[0].equalsIgnoreCase("Space-Bridge")) {
                stellarConversion(StellarMode.SPACE_BRIDGE, args);
            } else {
                printHelp();
            }
        } else if (args.length == 2) {
            //-CL With All Same Artist
            if (args[0].equalsIgnoreCase("-CL")) {
                stellarConversion(StellarMode.CLIPBOARD_SAME_ARTIST, args);
            } //Change Settings
            else if (args[0].equalsIgnoreCase("Set")) {
                if (args[1].equalsIgnoreCase("Pictures-Folder")) {
                    Path path = StellarUI.getFolderFor("Picture Folder")
                            .orElse(StellarDiskManager.USER_DIR);
                    StellarDiskManager.setPictureOutputFolder(path);
                    messageThenExit("Output Folder Changed To:" + path);
                } else if (args[1].equalsIgnoreCase("Output-Folder")) {
                    Path path = StellarUI.getFolderFor("Output Folder")
                            .orElse(StellarDiskManager.USER_DIR);
                    StellarDiskManager.setOutputFolder(path);
                    messageThenExit("Picture Folder Changed To: " + path);
                } else if (args[1].equalsIgnoreCase("Space-Bridge-Folder")) {
                    Path path = StellarUI.getFolderFor("Space-Bridge Folder")
                            .orElse(StellarDiskManager.USER_DIR);
                    StellarDiskManager.setSpaceBridgeDirectory(path);
                    messageThenExit("Space-Bridge Folder Changed To: " + path);
                }
            } else {
                printHelp();
            }
        } else if (args.length == 3) {
            //Manually Specify Author/Title with Timestamps
            if (firstArgIsLink(args[0]) && !allStringsAreTimestamps(args)) {
                stellarConversion(StellarMode.LINK_AUTHOR_TITLE, args);
            } else {
                printHelp();
            }
        }//Odd Number of Entries >= 3 (URL, timestamp0 timestamp1 timestamp2 timestamp3
        else if (args.length >= 3 && firstArgIsLink(args[0]) && (args.length + 1) % 2 == 0
                && allStringsAreTimestamps(args)) {
            StellarMode.LINK_TIMESTAMPS.start(args);
        } else {
            printHelp();
        }
        StellarUI.shutdownUI();
        System.exit(0);
    }

    /**
     * Checks to ensure that all timestamps are in timestamp format. You may
     * pass the URL/Path into this method, it will ignore it and send the
     * substring instead of just the timestamps.
     *
     * @param timestamps The string of timestamps, or suspected timestamps
     * @return If they all match timstamp format
     */
    private static boolean allStringsAreTimestamps(String[] args) {
        if (firstArgIsLink(args[0])) {
            return allStringsAreTimestampsActual(Arrays.copyOfRange(args, 1, args.length));
        } else {
            return allStringsAreTimestampsActual(args);
        }
    }

    /**
     * Checks to ensure that all timestamps are in timestamp format.
     *
     * @param timestamps The string of timestamps, or suspected timestamps
     * @return If they all match timstamp format
     */
    private static boolean allStringsAreTimestampsActual(String[] timestamps) {
        return Arrays.asList(timestamps).stream().allMatch(StellarFFMPEGTimeStamp::matchesFFmpegTimestampFormat);
    }

    /**
     * Checks if the argument is a link or not.
     *
     * @param arg The argument
     * @return If it is a link
     */
    private static boolean firstArgIsLink(String arg) {
        return Files.exists(Paths.get(arg));
    }

    /**
     * Prints the help doc.
     */
    private static void printHelp() throws IOException {
        messageThenExit(StellarDiskManager.readHelpText());
    }

    /**
     * Helper method, prints the message then System.exit().
     */
    private static void messageThenExit(String message) {
        System.out.println(message);
        System.exit(0);
    }

    /**
     * Convert using the specified conversion mode.
     *
     * @param mode The mode to use
     */
    private static void stellarConversion(StellarMode mode, String... args) throws IOException {
        mode.start(args);
    }
}
