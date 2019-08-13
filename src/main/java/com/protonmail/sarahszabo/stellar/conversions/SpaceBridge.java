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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/**
 * A class that oversees conversions in a certain folder and converts them to
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
     * The ledger used for mapping the entire library for the temporal playlists
     */
    private static final Map<Path, ConverterMetadata> LIBRARY_LEDGER = new HashMap<>(1500);
    /**
     * The filename and extension of the playlist ledger.
     */
    private static final Path LIBRARY_LEDGER_PATH;
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
            StellarDiskManager.DiskManagerState state = StellarDiskManager.getState();
            //The directory we're watching
            watching = state.getSpaceBridgeDirectory();
            //Our completed files go here
            SB_COMPLETED = watching.resolve("Space-Bridge Completed");
            LIBRARY_LEDGER_PATH = SB_COMPLETED.resolve("Library Ledger.dat");
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
        //Get Previous Ledger if Available, make a new one if not
        Files.createDirectories(LIBRARY_LEDGER_PATH.getParent());
        if (!Files.exists(LIBRARY_LEDGER_PATH)) {
            Files.createFile(LIBRARY_LEDGER_PATH);
        } else {
            //Previous Ledger Exists, Add It
            Map<Path, ConverterMetadata> previousLedger = MAPPER.readValue(LIBRARY_LEDGER_PATH.toFile(),
                    new TypeReference<HashMap<Path, ConverterMetadata>>() {
            });
            System.exit(0);
            LIBRARY_LEDGER.putAll(previousLedger);
            logger.info("Found a Previous Ledger, Adding It!");
        }
        //Begin file walk
        Files.walk(watching, FileVisitOption.FOLLOW_LINKS).parallel()
                //Don't get directories for the ledger & don't get playlist .xspf files
                .filter(path -> !Files.isDirectory(path) && !path.getFileName().toString().contains(".xspf"))
                //Main loop, build ledger
                .forEach(filePath -> {
                    //If the file exists in our ledger, don't force a costly exiftool
                    //Add only if we don't have it in our list
                    if (!LIBRARY_LEDGER.containsKey(filePath)) {
                        LIBRARY_LEDGER.put(filePath, StellarDiskManager.getMetadata(filePath));
                    }
                });
        //All work completed
        logger.info("Shutting Down Stellar Hyperspace");
        shutdownBridge();
        logger.info("Hyperspace Shutdown Complete!");
        logger.info("About to Generate Temporal Playlists");
        //Init Playlists
        generateTemporalPlaylists();
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
    private static void generateTemporalPlaylists() throws IOException {
        for (PLAYLIST playlist : PLAYLIST.values()) {
            logger.info("About to delte old entries from playlist: " + playlist);
            //Folders Are Already Set Up, But we Need to Make the Current Playlists Folder
            Files.createDirectories(playlist.getPath());
            //Purge Old Entries
            Files.walk(playlist.getPath()).parallel()
                    //Filter by: Is NOT in date range for deletion?
                    .filter(path -> !playlist.isInCurrentDateRange(StellarDiskManager.getMetadata(path)))
                    //Delete all not in range
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            logger.info("Deleted: " + path);
                        } catch (IOException ex) {
                            logger.severe(ex.toString());
                        }
                    });
            ;
            //Copy all files that fall within the time period for this playlist
            LIBRARY_LEDGER.keySet().stream().parallel()
                    //Filter by if it's in the right date range for this playlist& doesn't exist
                    .filter(path -> playlist.isInCurrentDateRange(LIBRARY_LEDGER.get(path))
                    && Files.notExists(playlist.getPath().resolve(path.getFileName())))
                    //Add the files to the appropriate playlist folder
                    .forEach(path -> {
                        try {
                            Files.copy(path, playlist.getPath().resolve(path.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
                        } catch (IOException ex) {
                            logger.severe(ex.toString());
                        }
                    });
            logger.info("Temporal Playlist Generation Finished for: " + playlist);
        }
        MAPPER.writeValue(LIBRARY_LEDGER_PATH.toFile(), LIBRARY_LEDGER);
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
        public Path getPath() {
            return SB_COMPLETED.resolve(toString());
        }

        /**
         * Gets whether or not the current date is in range of the time
         * interval. Ex: are we within 1 week of the created date?
         *
         * @param wrapper The wrapper containing the date
         * @return Whether or not this is true
         */
        public boolean isInCurrentDateRange(ConverterMetadata metadata) {
            return isInCurrentDateRange(metadata.getStellarIndexDate());
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
