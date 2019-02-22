/*/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar.conversions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.protonmail.sarahszabo.stellar.StellarDiskManager;
import com.protonmail.sarahszabo.stellar.metadata.ConverterMetadata;
import com.protonmail.sarahszabo.stellar.util.StellarGravitonField;
import com.protonmail.sarahszabo.stellar.util.StellarLoggingFormatter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/**
 * A class that oversees conversions in a certain folder and converts them to
 * 190K mobile formats or 320K for typical libraries, also manages temporal
 * playlists.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public enum SpaceBridge {

    /**
     * The instance newPath space-bridge.
     */
    SPACE_BRIDGE;

    /**
     * The directory that we are looking to duplicate.
     */
    private static final Path watching;
    /**
     * The path for space-bridge completed files. This is the home directory for
     * the copied file system.
     */
    private static final Path SB_COMPLETED;
    /**
     * The ledger used for new additions
     */
    private static final List<SBDatapacket> LEDGER = new ArrayList<>(1500);
    /**
     * The filename and extension of the playlist ledger.
     */
    private static final String LEDGER_FILENAME = "Playlist Ledger.dat";

    /**
     * The object mapper for mapping SB objects.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /**
     * The default console logger for space-bridge operations.
     */
    private static final Logger logger = StellarLoggingFormatter.forClass(SpaceBridge.class);
    /**
     * The path to the logging folder, which is used for storing logs.
     */
    private static final Path LOGGING_FOLDER = StellarDiskManager.CONFIGURATION_FOLDER.resolve("Log Files");
    /**
     * The log file for space-bridge exceptions. Timestamped to avoid naming
     * conflicts.
     */
    private static final Path EXCEPTION_LOG_PATH = LOGGING_FOLDER.resolve("Space-Bridge Exception Log "
            + StellarGravitonField.generateTimestampFileNameFriendly() + ".dat");

    /**
     * The exception logger for logging exceptions for space-bridge operations.
     */
    private static final Logger SB_EXCEPTION_LOGGER;

    /**
     * Static Initialiser for {@link SpaceBridge}. Sets up the initial state.
     */
    static {
        try {
            MAPPER.registerModule(new ParameterNamesModule());
            MAPPER.registerModule(new Jdk8Module());
            MAPPER.registerModule(new JavaTimeModule()); // new module, NOT JSR310Module
            StellarDiskManager.DiskManagerState state = StellarDiskManager.getState();
            //The directory we're watching
            watching = state.getSpaceBridgeDirectory();
            //Our completed files go here
            SB_COMPLETED = watching.resolve("Space-Bridge Completed");
            SB_EXCEPTION_LOGGER = StellarLoggingFormatter.forTitle("Space-Bridge", EXCEPTION_LOG_PATH);
            logger.log(Level.INFO, "Space-Bridge Initial Setup Complete!");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (Files.size(EXCEPTION_LOG_PATH) == 0) {
                        Files.deleteIfExists(EXCEPTION_LOG_PATH);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                }
            }, "Stellar Space-Bridge Cleanup Thread"));
        } catch (IOException ex) {
            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Gets the appropriate path for the copy folder by mirroring and resolving
     * the two folder paths. In other words, if watching is a/b and current is
     * a/b/c/d, get path for SpaceBridge/c/d.
     *
     * @param current The current path in the walk traversal
     * @param outputDirectory The base directory to output the files to
     * @return The mirrored path
     */
    private Path getCopyPath(Path current) {
        //If watching is a/b and current is a/b/c/d, get path for SpaceBridge/c/d
        return SB_COMPLETED.resolve(watching.relativize(current));
    }

    /**
     * Initiates the conversion processes and generates playlists for the
     * library.
     *
     * @throws java.io.IOException If something happened
     */
    public void initBridge() throws IOException {
        logger.log(Level.INFO, "\n\nSpace-Bridge Initiation");
        //Begin file walk
        Files.walk(watching, FileVisitOption.FOLLOW_LINKS).parallel()
                //Not a directory, not IN the SB directory, & filename isn't already in the completed folder
                .filter(path -> !Files.isDirectory(path) && !path.startsWith(SB_COMPLETED)
                && Files.notExists(getCopyPath(path)))
                //Main loop, copy everything over in a parallel fasion
                .forEach(filePath -> {
                    Future<Path> future = StellarHyperspace.getHyperspace().submit(() -> {
                        try {
                            //Define destination of copy operation
                            Path destination = getCopyPath(filePath);
                            //Create Directories for this file if they don't exist already
                            if (Files.notExists(destination.getParent())) {
                                Files.createDirectories(destination.getParent());
                            }
                            //Doesn't Exist in Destination, Copy Over
                            Files.copy(filePath, getCopyPath(filePath), StandardCopyOption.COPY_ATTRIBUTES,
                                    StandardCopyOption.REPLACE_EXISTING);
                            logger.log(Level.INFO, "<Space-Bridge>: \nCopied: " + filePath + "\n To: " + destination);
                            LEDGER.add(new SBDatapacket(destination));
                            return destination;
                        } catch (IOException ex) {
                            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                            throw new RuntimeException("We were unable to copy: " + filePath + "\n during the space-bridge operation."
                                    + " The disk might be full or some other I/O related error.", ex);
                        }
                    });
                });
        //All work completed
        logger.info("Shutting Down Stellar Hyperspace");
        shutdownBridge();
        logger.info("Hyperspace Shutdown Complete!");
        logger.info("About to Generate Temporal Playlists");
        //Init Playlists
        generateTemporalPlaylists(SB_COMPLETED);
        logger.info("Temporal Playlist Generation Complete!");
    }

    /**
     * Shuts down the executors.
     */
    private static void shutdownBridge() {
        try {
            StellarHyperspace.initiateFalseVacuum();
        } catch (InterruptedException ex) {
            SB_EXCEPTION_LOGGER.severe("Interrupted During Waiting for Stellar Hyperspace Task Completion");
            throw new RuntimeException(ex);
        }
    }

    /**
     * Generates all temporal playlists. Temporal playlists are playlists that
     * contain files from the library that were created in a certain time
     * period. Folders must be set up/exist prior to calling this subroutine.
     *
     * @param root The root folder to generate the playlists in, should also be
     * where the freshly converted .opus files are
     */
    private static void generateTemporalPlaylists(Path root) throws IOException {
        //Ledger is Already Set Up for New Data, get new data that is for our root playlist and add it to ledger file
        List<SBDatapacket> ledger;
        //Define Overall Playlist Folder
        Path playlistFolder = root.resolve("Temporal Playlists");
        for (PLAYLIST playlist : PLAYLIST.values()) {
            //Define Current Playlist Folder
            Path currentPlaylistFolder = playlistFolder.resolve(playlist.toString());
            //Folders Are Already Set Up, But we Need to Make the Current Playlists Folder
            Files.createDirectories(currentPlaylistFolder);
            Path ledgerPath = currentPlaylistFolder.resolve(LEDGER_FILENAME);
            //If Ledger Exists, Get Old Data & Combine With New Data -> Back to Disk
            if (Files.exists(ledgerPath)) {
                logger.info("Ledger Found for " + ledgerPath + "!");
                //Get list from local file
                ledger = MAPPER.readValue(ledgerPath.toFile(), new TypeReference<List<SBDatapacket>>() {
                });
            } else {
                logger.info("No ledger found for " + ledgerPath + "\nCreating a New One");
                //If the folder is empty, all entries are in LEDGER, no need to exiftool entire directory
                if (Files.list(currentPlaylistFolder).count() == 0) {
                    ledger = Collections.emptyList();
                } else {
                    //There Were Previous Entries, Generate New Ledger
                    ledger = Files.walk(root, FileVisitOption.FOLLOW_LINKS).parallel()
                            .filter(path -> !path.startsWith(playlistFolder) && !Files.isDirectory(path)).map(SBDatapacket::new)
                            .collect(Collectors.toList());
                }
                //Make Ledger
                Files.createFile(ledgerPath);
            }
            List<SBDatapacket> combined = Stream.concat(LEDGER.stream(), ledger.stream()).collect(Collectors.toList());
            //Copy Current Entries to Current Playlist
            combined.stream().filter(data -> !Files.isDirectory(data.getPath())
                    && playlist.isInCurrentDateRange(data)).map(SBDatapacket::getPath)
                    .forEach(path -> {
                        try {
                            //Checking for existance is far less intensive than copying a large 320K file
                            path.toString();
                            Path destinationPath = currentPlaylistFolder.resolve(path.getFileName());
                            if (Files.notExists(destinationPath)) {
                                Files.copy(path, destinationPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                            SB_EXCEPTION_LOGGER.severe("I/O Exception While trying to copy file to current playlist folder " + ex);
                        }
                    });
            //Delete Old Entries From Current Playlist
            combined.parallelStream().filter(data -> !playlist.isInCurrentDateRange(data)).map(SBDatapacket::getPath)
                    .map(path -> currentPlaylistFolder.resolve(path.getFileName()))
                    .map(Path::toFile).forEach(file -> FileUtils.deleteQuietly(file));
            combined = combined.parallelStream().filter(data -> playlist.isInCurrentDateRange(data))
                    .collect(Collectors.toList());
            MAPPER.writeValue(ledgerPath.toFile(), combined);
        }
    }

    /**
     * A wrapper class for optimising performance of the generate playlists
     * subroutine.
     */
    private static class SBDatapacket {

        @JsonProperty(value = "path")
        private final Path path;
        @JsonProperty(value = "metadata")
        private final ConverterMetadata metadata;

        public SBDatapacket(Path path) {
            this.path = path;
            this.metadata = StellarDiskManager.getMetadata(path);
        }

        /**
         * Constructor for the wrapper
         *
         * @param path The path of the file
         * @param date The date the file was created
         */
        @JsonCreator
        SBDatapacket(@JsonProperty(value = "path") Path path,
                @JsonProperty(value = "metadata") ConverterMetadata metadata) {
            this.path = path;
            this.metadata = metadata;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 11 * hash + Objects.hashCode(this.metadata);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SBDatapacket other = (SBDatapacket) obj;
            return Objects.equals(this.metadata, other.metadata);
        }

        /**
         * A getter for the path
         *
         * @return The path
         */
        public Path getPath() {
            return this.path;
        }

        /**
         * A getter for the metadata.
         *
         * @return The metadata
         */
        public ConverterMetadata getMetadata() {
            return this.metadata;
        }
    }

    /**
     * A class representing temporal playlists.
     */
    private static enum PLAYLIST {
        /**
         * The playlist for 1 week.
         */
        ONE_WEEK {
            @Override
            public boolean isInCurrentDateRange(LocalDate date) {
                boolean f = date.isAfter(LocalDate.now().minusWeeks(1));
                return date.isAfter(LocalDate.now().minusWeeks(1));
            }

            @Override
            public String toString() {
                return "1 Week";
            }
        },
        /**
         * The playlist for 2 weeks.
         */
        TWO_WEEKS {
            @Override
            public boolean isInCurrentDateRange(LocalDate date) {
                return date.isAfter(LocalDate.now().minusWeeks(2));
            }

            @Override
            public String toString() {
                return "2 Weeks";
            }
        },
        /**
         * The playlist for 1 month.
         */
        ONE_MONTH {
            @Override
            public boolean isInCurrentDateRange(LocalDate date) {
                return date.isAfter(LocalDate.now().minusMonths(1));
            }

            @Override
            public String toString() {
                return "1 Month";
            }
        },
        /**
         * The playlist for 2 months.
         */
        TWO_MONTHS {
            @Override
            public boolean isInCurrentDateRange(LocalDate date) {
                return date.isAfter(LocalDate.now().minusMonths(2));
            }

            @Override
            public String toString() {
                return "2 Months";
            }
        },
        /**
         * The playlist for 6 months.
         */
        SIX_MONTHS {
            @Override
            public boolean isInCurrentDateRange(LocalDate date) {
                return date.isAfter(LocalDate.now().minusMonths(6));
            }

            @Override
            public String toString() {
                return "6 Months";
            }
        };

        /**
         * Gets the path associated with this playlist.
         *
         * @param root The parent this playlist is expected to be in
         * @return The path to the folder of this playlist
         */
        public Path getPath(Path root) {
            return root.resolve(toString());
        }

        /**
         * Gets whether or not the current date is in range of the time
         * interval. Ex: are we within 1 week of the created date?
         *
         * @param wrapper The wrapper containing the date
         * @return Whether or not this is true
         */
        public boolean isInCurrentDateRange(SBDatapacket wrapper) {
            return isInCurrentDateRange(wrapper.getMetadata().getDate());
        }

        /**
         * Gets whether or not the current date is in range of the time
         * interval. Ex: are we within 1 week of the created date?
         *
         * @param date The date to compare against
         * @return Whether or not this is true
         */
        public abstract boolean isInCurrentDateRange(LocalDate date);

        @Override
        public abstract String toString();
    }
}
