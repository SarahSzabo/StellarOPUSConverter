/*/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import static com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField.*;
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarLoggingFormatter;
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
    private static final Path COMPLETED;

    /**
     * The path for 320K bitrate opus files.
     */
    private static final Path COMPLETED320K;
    /**
     * The path for 190K bitrate opus files.
     */
    private static final Path COMPLETED190K;
    /**
     * The folder for re-indexing files in the
     * {@link StellarDiskManager.DiskManagerState#getSpaceBridgeDirectory()}
     * folder.
     */
    private static final Path reIndexingFolder;
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
    private static final Path LOGGING_FOLDER = newPath(StellarDiskManager.CONFIGURATION_FOLDER, "Log Files");
    /**
     * The log file for space-bridge exceptions. Timestamped to avoid naming
     * conflicts.
     */
    private static final Path EXCEPTION_LOG_PATH = newPath(LOGGING_FOLDER, "Space-Bridge Exception Log "
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
            COMPLETED = newPath(watching, "Space-Bridge Completed");
            COMPLETED320K = newPath(COMPLETED, "320K");
            COMPLETED190K = newPath(COMPLETED, "190K");
            Files.createDirectories(COMPLETED320K);
            Files.createDirectories(COMPLETED190K);
            SB_EXCEPTION_LOGGER = StellarLoggingFormatter.forTitle("Space-Bridge", EXCEPTION_LOG_PATH);
            reIndexingFolder = newPath(StellarDiskManager.getState().getSpaceBridgeDirectory(), "Space-Bridge ReIndexing");
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
    private Path getCopyPath(Path current, Path outputDirectory) {
        //If watching is a/b and current is a/b/c/d, get path for SpaceBridge/c/d
        return newPath(outputDirectory, watching.relativize(current));
    }

    /**
     * Gets a stream for the walk path for the watched directory minus
     * Space-Bridge folders. Excludes all directories and only gets a stream of
     * path files.
     *
     * @return The stream
     * @param outputFolder The folder to create files/folders in
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterDuplicatesOnlyFiles(Path outputFolder) throws IOException {
        return fileWalkFilterDuplicates(outputFolder).parallel().filter(path
                -> !Files.isDirectory(path) && !path.getFileName().toString().startsWith("."));
    }

    /**
     * Gets a stream for the walk path for the watched directory minus
     * Space-Bridge folders.
     *
     * @return The stream
     * @param outputFolder The folder to create files/folders in
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterDuplicates(Path outputFolder) throws IOException {
        //Ensure no duplicates in both folders
        return Files.walk(watching, FileVisitOption.FOLLOW_LINKS).parallel()
                .filter(path -> !path.startsWith(outputFolder) && !path.startsWith(reIndexingFolder) && !path.startsWith(COMPLETED));
    }

    /**
     * Checks whether or not the file exists in the space-bridge folder. NOTE:
     * Only .opus files are allowed in the space-bridge folder.
     *
     * @param path The path of the file
     * @return Whether or not the file exists in the space-bridge folder
     */
    private boolean existInSpaceBridge(Path path, Path outputFolder) {
        //Replace Possible Other Filenames, All Files are Just .opus in the Space-Bridge Directory
        //Strip Extension
        String fileName = preferredTitleFormat(StellarOPUSConverter.stripFileExtension(path));
        //Does this path exist in the copy directory?
        Path newPath = getCopyPath(newPath(path.getParent(), fileName + ".opus"), outputFolder);
        logger.fine("Space-Bridge Path: " + newPath);
        logger.fine("Original: " + path);
        logger.fine("Exists? " + Files.exists(newPath));
        return Files.exists(newPath);
    }

    /**
     * Mirrors the directories between the watching folder and the specified
     * outputFolder.
     *
     * @param outputFolder The folder to create files/folders in
     * @throws IOException If something went wrong
     */
    private void mirrorDirectories(Path outputFolder) throws IOException {
        //Mirror Directories
        fileWalkFilterDuplicates(outputFolder).filter(path -> Files.isDirectory(path)).forEach(folder -> {
            try {
                Path folderPath = getCopyPath(folder, outputFolder);
                Files.createDirectories(folderPath);
                logger.log(Level.INFO, "Created Folder: " + folderPath + "\n");
            } catch (IOException ex) {
                SB_EXCEPTION_LOGGER.log(Level.SEVERE, "Init Bridge, Create Directories", ex);
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * Initiates the conversion processes and generates playlists for 320K &
     * 190K.
     *
     * @throws java.io.IOException If something happened
     */
    public void initBridge() throws IOException {
        logger.log(Level.INFO, "\n\nAbout to Mirror Directories");
        mirrorDirectories(COMPLETED320K);
        mirrorDirectories(COMPLETED190K);
        logger.log(Level.INFO, "\n\nDirectory Mirroring Process Complete");
        logger.log(Level.INFO, "\n\nAbout to Mirror And Convert Files to lower bitrate");
        //Check if Converted Files Exist Already, if Not Convert Them
        //No Directories, File Must be newPath expected file type, File Should Not Already Be Indexed
        //Filtering by just 320K is ok because they are always created together
        fileWalkFilterDuplicatesOnlyFiles(COMPLETED).filter(path -> !existInSpaceBridge(path, COMPLETED320K))
                .forEach(filePath -> {
                    Future<Path> future = StellarHyperspace.getHyperspace().submit(() -> {
                        try {
                            //Doesn't Exist in Destination, Convert to 320K & 190K
                            Path destination = getCopyPath(filePath, COMPLETED320K).getParent();
                            StellarOPUSConverter converter = new StellarOPUSConverter(filePath, destination);
                            Path path = converter.convertToOPUS().orElseThrow(() -> new IOException("Error in Processing <320K>" + filePath));
                            LEDGER.add(new SBDatapacket(path));
                            logger.log(Level.INFO, "\nFile Convertion Complete <320K>: " + path + "\n");
                            destination = getCopyPath(filePath, COMPLETED190K).getParent();
                            converter = new StellarOPUSConverter(filePath, destination, converter.getMetadata());
                            path = converter.convertToOPUS(190).orElseThrow(() -> new IOException("Error in Processing <190K>" + filePath));
                            LEDGER.add(new SBDatapacket(path));
                            logger.log(Level.INFO, "\nFile Convertion Complete <190K>: " + path + "\n");
                            return path;
                        } catch (IOException ex) {
                            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                            try {
                                //Some exception in the process, just copy the file over to the directory, best we can do
                                Files.copy(filePath, getCopyPath(filePath, COMPLETED),
                                        StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                                SB_EXCEPTION_LOGGER.severe("Encountered an unconvertable file: " + filePath.toAbsolutePath() + " & just copied it");
                                return getCopyPath(filePath, COMPLETED);
                            } catch (IOException ex1) {
                                SB_EXCEPTION_LOGGER.log(Level.SEVERE, null, ex1);
                                throw new RuntimeException(ex1);
                            }
                        }
                    });
                });
        //All work completed
        logger.info("Shutting Down Stellar Hyperspace");
        shutdownBridge();
        logger.info("Hyperspace Shutdown Complete!");
        logger.info("About to Generate Temporal Playlists");
        //Init Playlists
        generateTemporalPlaylists(COMPLETED320K);
        generateTemporalPlaylists(COMPLETED190K);
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
     * Checks a path to see if its metadata attribute title matches the
     * space-bridge title.
     *
     * @param path The path to investigate
     * @return Whether or not this is true
     */
    private boolean filterByFileNameExistance(Path path) {
        return existInSpaceBridge(path, reIndexingFolder);
    }

    /**
     * Checks a path to see if its metadata attribute title matches the
     * space-bridge title.
     *
     * @param path The path to investigate
     * @return Whether or not this is true
     * @throws IOException If something went wrong
     */
    private boolean filterByFileNamePattern(Path path) {
        //Or could exist in the filename metadata format itself
        return existInSpaceBridge(newPath(path.getParent(),
                StellarOPUSConverter.generateMetadata(path).getTitle() + ".opus"),
                reIndexingFolder);
    }

    /**
     * Checks a path to see if its metadata attribute title matches the
     * space-bridge title.
     *
     * @param path The path to investigate
     * @return Whether or not this is true
     * @throws IOException If something went wrong
     */
    private boolean filterByFileAttributes(Path path) {
        try {
            //Could be Author - Title or have title field set in metadata attribute fields
            return existInSpaceBridge(newPath(path.getParent(),
                    StellarDiskManager.getMetadata(path).getTitle() + ".opus"), reIndexingFolder);
        } catch (RuntimeException ex) {
            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
            SB_EXCEPTION_LOGGER.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Initiates the ReIndexing bridge which uses
     * {@link StellarOPUSConverter#reIndexOPUSFile()} to apply the latest
     * version of conversion options to older .opus libraries. Doesn't generate
     * playlists.
     *
     * @param bitrate The bitrate for the converted files
     * @throws java.io.IOException If something went wrong
     */
    public void initReIndexingBridge(int bitrate) throws IOException {
        logger.info("About to Mirror Directories");
        //Mirror Directories, We'll do this in the ReIndexing Folder
        mirrorDirectories(reIndexingFolder);
        logger.info("Dirctory Mirroring Complete!");
        //Reindex All Files & Put them in the ReIndexing Folder
        //If either one exists in space-bridge folder, the overall statement is false, do not convert this file
        fileWalkFilterDuplicatesOnlyFiles(reIndexingFolder).filter(path
                -> !(filterByFileNameExistance(path) || filterByFileNamePattern(path)))
                .forEach(path -> {
                    Future<Path> future = StellarHyperspace.getHyperspace().submit(() -> {
                        try {
                            StellarOPUSConverter converter = new StellarOPUSConverter(path, getCopyPath(path, reIndexingFolder).getParent());
                            Path completedFilePath = converter.convertToOPUS(bitrate).orElseThrow(() -> new IOException("Error in Processing"));
                            logger.log(Level.INFO, "\nFile Convertion Complete: " + path + "\n");
                            return completedFilePath;
                        } catch (IOException ex) {
                            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                            try {
                                //Some exception in the process, just copy the file over to the directory, best we can do
                                Files.copy(path, getCopyPath(path, reIndexingFolder),
                                        StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                                SB_EXCEPTION_LOGGER.severe("Encountered an unconvertable file: " + path.toAbsolutePath() + " & just copied it");
                                return getCopyPath(path, reIndexingFolder);
                            } catch (IOException ex1) {
                                SB_EXCEPTION_LOGGER.log(Level.SEVERE, null, ex1);
                                throw new RuntimeException(ex1);
                            }
                        }
                    });
                });
        //All work completed
        logger.info("Shutting Down ReIndexing Bridge");
        shutdownBridge();
        logger.info("Shutdown Complete!");
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
        //Ledger is Already Set Up for New Data, get new data that is for our root (320K/190K) playlist and add it to ledger file
        List<SBDatapacket> ledger;
        //Define Overall Playlist Folder
        Path playlistFolder = root.resolve("Playlists");
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
                            Path destinationPath = currentPlaylistFolder.resolve(path.getFileName());
                            if (Files.notExists(destinationPath)) {
                                Files.copy(path, destinationPath, StandardCopyOption.COPY_ATTRIBUTES);
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                            SB_EXCEPTION_LOGGER.severe("I/O Exception While trying to copy file to current playlist folder" + ex);
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
            return newPath(root, toString());
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
