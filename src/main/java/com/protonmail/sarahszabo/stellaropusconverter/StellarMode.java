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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
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
     * Used when we are interested in converting to the 120k format for our
     * mobile phones.
     */
    SPACE_BRIDGE {
        @Override
        public void start(String... args) throws IOException {
            SpaceBridge.SPACE_BRIDGE.initBridge();
            try {
                StellarHyperspace.initiateFalseVacuum();
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    },
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
    LINK_TIMESTAMPS {
        @Override
        public void start(String... args) throws IOException {
            Path path = Paths.get(args[0]);
            //Get Timestamps
            List<StellarFFMPEGTimeStamp> timestamps = Arrays.asList(Arrays.copyOfRange(args, 1, args.length))
                    .stream().map(StellarFFMPEGTimeStamp::fromString).collect(Collectors.toList());
            List<Callable<Path>> taskList = new ArrayList<>(timestamps.size() / 2);
            StellarFFMPEGTimeStamp start = null, end = null;
            //Every even is start timestamp, every odd is end timestamp
            for (int i = 0; i < timestamps.size(); i++) {
                if ((i + 1) % 2 == 0) {
                    end = timestamps.get(i);
                    //Final proxies for lambda
                    final StellarFFMPEGTimeStamp s = start, e = end;
                    //Add conversion tasks to list for conversion
                    taskList.add(() -> {
                        StellarOPUSConverter converter = new StellarOPUSConverter(path);
                        return converter.convertToOPUS(s, e);
                    });
                } else {
                    start = timestamps.get(i);
                }
            }
            String text = "";
            for (Future<Path> future : StellarHyperspace.runGeneralConversionTasks(taskList)) {
                try {
                    text += future.get() + "\n";
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
            }
            Logger.getLogger(StellarMode.class.getName()).info("\n\nFinished Files: " + text);
        }
    },
    /**
     * Creates the opus file with the specified metadata.
     */
    LINK_AUTHOR_TITLE {
        @Override
        public void start(String... args) throws IOException {
            StellarOPUSConverter converter = new StellarOPUSConverter(Paths.get(args[0]));
            converter.convertToOPUS(args[1], args[2]);
        }
    },
    /**
     * Gets the files from the clipboard.
     */
    GET_FROM_CLIPBOARD {
        @Override
        public void start(String... args) {
            doMultipleConversion(Optional.of(StellarUI.getFilesFromClipboard()));
        }

    },
    /**
     * Used when using graphics mode to directly convert links.
     */
    GRAPHICAL_FILE_CHOICE_DIRECT_LINKS {
        @Override
        public void start(String... args) {
            doMultipleConversion(StellarUI.getFiles());
        }

    },
    /**
     * A shortcut for when all files on the clipboard are made by the same
     * artist. Uses the provided artist with the filenames.
     */
    CLIPBOARD_SAME_ARTIST {
        @Override
        public void start(String... args) throws IOException {
            printFileList(StellarHyperspace.runGeneralConversionTasks(StellarUI.getFilesFromClipboard().stream()
                    .map(path -> toTaskFormat(path, args[1])).collect(Collectors.toList())));
        }

        /**
         * Helper subroutine for task creation.
         *
         * @return The task to run in hyperspace
         */
        private Callable<Path> toTaskFormat(Path filePath, String artist) {
            return () -> {
                StellarOPUSConverter converter = new StellarOPUSConverter(filePath);
                return converter.convertToOPUS(artist, StellarOPUSConverter.FileExtension.stripFileExtension(filePath));
            };
        }
    };

    private static Logger logger = Logger.getLogger(StellarMode.class.getName());

    public abstract void start(String... args) throws IOException;

    /**
     * Prints the list from Hyperspace
     *
     * @param paths The future paths
     */
    public void printFileList(List<Future<Path>> paths) {
        String string = paths.stream().map(futurePath -> {
            try {
                return futurePath.get().toString();
            } catch (Exception ex) {
                Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        })
                .collect(Collectors.joining("\n"));
        Logger.getLogger(StellarMode.class.getName()).info("\n\nCompleted Files:\n\n" + string);
    }

    /**
     * Does multiple conversions by sending them all to Hyperspace.
     *
     * @param paths The paths of the files to convert
     */
    private static void doMultipleConversion(Optional<List<Path>> paths) {
        //Check if path is not a directory & exists
        if (paths.isPresent() && paths.get().stream().allMatch(path -> !Files.isDirectory(path) && Files.exists(path))) {
            //Sends the paths to Hyperspace and lists the completed file paths
            List<Path> finalPaths = StellarHyperspace.runConversionTasks(paths.get()).stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IllegalStateException("Stellar Mode Interrupted While Waiting for Futures", ex);
                }
            }).collect(Collectors.toList());
            Logger.getLogger(StellarMode.class.getName()).info("\n\n\nCompleted Output Files:");
            finalPaths.stream().forEach(path -> Logger.getLogger(StellarMode.class.getName()).info(path.toString()));
        } else {
            Logger.getLogger(StellarMode.class.getName()).info("All Paths Do Not Exist or Are Directories");
        }
        try {
            StellarHyperspace.initiateFalseVacuum();
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
        }
        Platform.exit();
    }

    private static void printIOExceptionMessage() {
        Logger.getLogger(StellarMode.class.getName()).info("We've encountered an I/O error. Check your disk capacity.");
    }
}
