/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        /*
        Current formats:
        URL
        URL TimeStart TimeEnd (Supports HH:MM:SS, & MM:SS)

        TO IMPLEMENT:
        -G Select By Graphics (MultiThread)
        Select From Clipboard & MultiThread (-CVC: Convert Via Clipboard)
         */
        if (args.length == 0) {
            messageThenExit("There is no file specified!");
        } else if (args.length == 1 && firstArgIsLink(args[0])) {
            StellarMode.DIRECT_LINK.start(args);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("-g")) {
            //Enable Graphics & Multithread
            printHelp();
        } else if (args.length == 2) {
            messageThenExit("We don't currently support 2 arguments");
            printHelp();
        } else if (args.length == 3 && firstArgIsLink(args[0])) {
            StellarMode.LINK_AUTHOR_TIMESTAMPS.start(args);
        } else {
            printHelp();
        }
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
    private static void printHelp() {
        messageThenExit("OPTIONS: \n\n ? = Help "
                + "\n/home/MyHardDriveFile.mp4 = Convert Selected File to .opus"
                + "\n/home/MyHardDriveFile.mp4 2:5:47 5:0:0 Converts file from 2 hours 5 minutes and 47 seconds to 5 hours to .opus,"
                + "NOTE: this also works with just the minues/seconds format as well.\n");
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
    private static void stellarConversion(StellarMode mode, String... args) {
        mode.start(args);
    }
}
