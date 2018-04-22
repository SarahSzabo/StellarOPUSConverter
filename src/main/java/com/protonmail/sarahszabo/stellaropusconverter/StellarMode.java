/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;

/**
 * An interface representing the mode that the converter is in.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public enum StellarMode {
    /**
     * Used when only a direct link to the file is chosen, and nothing else.
     */
    DIRECT_LINK {
        @Override
        public void start(String... args) {
            try {
                StellarOPUSConverter converter = new StellarOPUSConverter(Paths.get(args[0]));
                converter.convertToOPUS();
            } catch (IOException ex) {
                Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
                printIOExceptionMessage();
            }
        }
    },
    /**
     * Used when a URL, author, and start and end times are specified.
     */
    LINK_AUTHOR_TIMESTAMPS {
        @Override
        public void start(String... args) {
            StellarFFMPEGTimeStamp start = StellarFFMPEGTimeStamp.fromString(args[1]);
            StellarFFMPEGTimeStamp end = StellarFFMPEGTimeStamp.fromString(args[2]);
            try {
                StellarOPUSConverter converter = new StellarOPUSConverter(Paths.get(args[0]));
                converter.convertToOPUS(start, end);
            } catch (IOException ex) {
                Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    },
    /**
     * Used when using graphics mode to directly convert links.
     */
    GRAPHICAL_FILE_CHOICE_DIRECT_LINKS {
        @Override
        public void start(String... args) {
            Optional<List<Path>> paths = StellarUI.getFiles();
            if (paths.isPresent()) {
                List<Path> finalPaths = StellarHyperspace.runConversionTasks(paths.get()).stream().map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
                        throw new IllegalStateException("Stellar Mode Interrupted While Waiting for Futures", ex);
                    }
                }).collect(Collectors.toList());
                System.out.println("\n\n\nCompleted Output Files:");
                finalPaths.stream().forEach(System.out::println);
            }
            StellarHyperspace.initiateFalseVacuum();
            Platform.exit();
        }
    };

    public abstract void start(String... args);

    private static void printIOExceptionMessage() {
        System.err.println("We've encountered an I/O error. Check your disk capacity.");
    }
}
