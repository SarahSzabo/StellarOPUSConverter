/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar;

import com.protonmail.sarahszabo.stellar.conversions.SpaceBridge;
import com.protonmail.sarahszabo.stellar.conversions.StellarFFMPEGTimeStamp;
import com.protonmail.sarahszabo.stellar.conversions.StellarHyperspace;
import com.protonmail.sarahszabo.stellar.conversions.converters.StellarOPUSConverter;
import com.protonmail.sarahszabo.stellar.metadata.ConverterMetadata;
import com.protonmail.sarahszabo.stellar.metadata.ConverterMetadataBuilder;
import com.protonmail.sarahszabo.stellar.metadata.MetadataType;
import com.protonmail.sarahszabo.stellar.transmissions.Uplink;
import com.protonmail.sarahszabo.stellar.transmissions.YoutubeUplink;
import com.protonmail.sarahszabo.stellar.util.FileExtension;
import com.protonmail.sarahszabo.stellar.util.StellarCLIUtils;
import com.protonmail.sarahszabo.stellar.util.StellarLoggingFormatter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;

/**
 * An interface representing the mode that the converter is in. Principally
 * designed to be used in command line mode.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public enum StellarMode {

    /**
     * Selects a file, and uses that file for pictures for the rest of the
     * files.
     */
    PICTURE_SELECT {
        @Override
        public void start(String... args) throws IOException {
            //Get From Clipboard
            List<Path> paths = new ArrayList<>(StellarCLIUtils.getFilesFromClipboard().get());
            //Select File From List
            Path selected = StellarCLIUtils.selectPath(paths).orElseThrow(() -> new RuntimeException("Nothing Selected from path chooser!"));
            //Remove Video Entry from Paths
            paths.remove(selected);
            //Convert Selected File Using Converter Methods to Generate Metadata for it, even if it is an .MP4
            StellarOPUSConverter metadataConverter = new StellarOPUSConverter(selected, StellarDiskManager.getTempDirectory());
            Path metaDataOpusFile = metadataConverter.convertToOPUS().get();
            //Get Old Metadata, If it Exists
            ConverterMetadata newMetadata = StellarDiskManager.getMetadata(metaDataOpusFile);
            if (ConverterMetadata.isDefaultMetadata(MetadataType.ALBUM_ART, newMetadata)) {
                throw new RuntimeException("There is no Album Art Available for: " + selected);
            }
            for (Path path : paths) {
                //Get Old Metadata for our Files
                ConverterMetadataBuilder oldOpusMetadata
                        = new ConverterMetadataBuilder(StellarDiskManager.getMetadata(path));
                //Set With new Album Art
                oldOpusMetadata.albumArtPath(newMetadata.getAlbumArtPath());
                StellarOPUSConverter converter = new StellarOPUSConverter(path, oldOpusMetadata.buildMetadata());
                converter.convertToOPUS();
            }
            logger.info(paths.stream().map(path -> path.toAbsolutePath().toString()).collect(Collectors.joining(", ")) + " album art have been"
                    + "switched to " + selected.toAbsolutePath() + "'s album art");
        }
    },
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
                //Determine if this is a web link or a file link
                if (args[0].contains("http")) {
                    //This is a web link!
                    Uplink uplink = new YoutubeUplink(args[0]);
                    uplink.recieveTransmission();
                    //File is now on disk, convert
                    StellarOPUSConverter converter = new StellarOPUSConverter(uplink.getPath());
                    converter.convertToOPUS();
                } else {
                    //Regular file on disk!
                    StellarOPUSConverter converter = new StellarOPUSConverter(Paths.get(args[0]));
                    converter.convertToOPUS();
                }
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
                        return converter.convertToOPUS(s, e, 320).get();
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
            deleteOriginalFilesWithConfirmation(List.of(path));
        }
    },
    /**
     * Creates the opus file with the specified metadata.
     */
    LINK_AUTHOR_TITLE {
        @Override
        public void start(String... args) throws IOException {
            StellarOPUSConverter converter = new StellarOPUSConverter(Paths.get(args[0]),
                    new ConverterMetadataBuilder().artist(args[1]).title(args[2]).buildMetadata());
            converter.convertToOPUS();
        }
    },
    /**
     * Gets the files from the clipboard.
     */
    GET_FROM_CLIPBOARD {
        @Override
        public void start(String... args) {
            doMultipleConversion(Optional.of(StellarCLIUtils.getFilesFromClipboard().get()));
        }

    },
    /**
     * Used when using graphics mode to directly convert links.
     */
    GRAPHICAL_FILE_CHOICE_DIRECT_LINKS {
        @Override
        public void start(String... args) {
            doMultipleConversion(StellarCLIUtils.getFiles());
        }

    },
    /**
     * A shortcut for when all files on the clipboard are made by the same
     * artist. Uses the provided artist with the filenames.
     */
    CLIPBOARD_SAME_ARTIST {
        @Override
        public void start(String... args) throws IOException {
            printFileList(StellarHyperspace.runGeneralConversionTasks(StellarCLIUtils.getFilesFromClipboard().get().stream()
                    .map(path -> toTaskFormat(path, args[1])).collect(Collectors.toList())));
        }

        /**
         * Helper subroutine for task creation.
         *
         * @return The task to run in hyperspace
         */
        private Callable<Path> toTaskFormat(Path filePath, String artist) {
            return () -> {
                StellarOPUSConverter converter = new StellarOPUSConverter(filePath,
                        new ConverterMetadataBuilder().artist(artist).title(FileExtension.stripFileExtension(filePath)).buildMetadata());
                return converter.convertToOPUS().get();
            };
        }
    };

    /**
     * The logger for stellar mode.
     */
    private static Logger logger = StellarLoggingFormatter.forClass(StellarMode.class);

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
     * Prompts the user if they want to delete all the original files and does
     * it if they want to.
     *
     * @param paths The collection of paths to delete
     */
    private static void deleteOriginalFilesWithConfirmation(Collection<Path> paths) {
        if (StellarCLIUtils.showConfirmationDialog("Delete Original Files? \n\n" + paths.toString())) {
            for (var path : paths) {
                FileUtils.deleteQuietly(path.toFile());
                logger.info(path + " deleted!\n");
            }
        } else {
            Logger.getLogger(StellarMode.class.getName()).info("All Paths Do Not Exist or Are Directories");
        }
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
            finalPaths.stream().forEachOrdered(path -> System.out.println(path));
            //Delete Original Files
            deleteOriginalFilesWithConfirmation(paths.get());
            try {
                StellarHyperspace.initiateFalseVacuum();
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
            }
            Platform.exit();
        } else {
            throw new IllegalStateException("A File in the Path Doesn't Exist! "
                    + "You might be using an illegal characterset for the filename!");
        }
    }

    private static void printIOExceptionMessage() {
        Logger.getLogger(StellarMode.class.getName()).info("We've encountered an I/O error. Check your disk capacity.");
    }
}
