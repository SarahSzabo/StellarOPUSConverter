/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import static com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField.*;
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarLoggingFormatter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/**
 * A class that oversees conversions in a certain folder and converts them to
 * 120K mobile formats.
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
    private static final Path completed;
    /**
     * The folder for re-indexing files in the
     * {@link StellarDiskManager.DiskManagerState#getSpaceBridgeDirectory()}
     * folder.
     */
    private static final Path reIndexingFolder;
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
            StellarDiskManager.DiskManagerState state = StellarDiskManager.getState();
            //The directory we're watching
            watching = state.getSpaceBridgeDirectory();
            //Our completed files go here
            completed = newPath(watching, "Space-Bridge Completed");
            SB_EXCEPTION_LOGGER = StellarLoggingFormatter.forTitle("Space-Bridge", EXCEPTION_LOG_PATH);
            reIndexingFolder = newPath(StellarDiskManager.getState().getSpaceBridgeDirectory(), "Space-Bridge ReIndexing");
            Files.createDirectories(reIndexingFolder);
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
     * Gets a stream newPath the walk newPath the watched directory minus
     * Space-Bridge folders. Excludes all directories and only gets a stream
     * newPath files.
     *
     * @return The stream
     * @param outputFolder The folder to create files/folders in
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterDuplicatesOnlyFiles(Path outputFolder) throws IOException {
        return fileWalkFilterDuplicates(outputFolder).parallel().filter(path
                -> !Files.isDirectory(path) && !path.getFileName().toString().startsWith(".")).distinct();
    }

    /**
     * Gets a stream newPath the walk newPath the watched directory minus
     * Space-Bridge folders.
     *
     * @return The stream
     * @param outputFolder The folder to create files/folders in
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterDuplicates(Path outputFolder) throws IOException {
        //Ensure no duplicates in both folders
        return Files.walk(watching, FileVisitOption.FOLLOW_LINKS).parallel()
                .filter(path -> !path.toAbsolutePath().toString().contains(outputFolder.getFileName().toString())).distinct();
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
        fileWalkFilterDuplicates(outputFolder).filter(path -> Files.isDirectory(path)).forEachOrdered(folder -> {
            try {
                Path folderPath = getCopyPath(folder, reIndexingFolder);
                Files.createDirectories(folderPath);
                logger.log(Level.INFO, "Created Folder: " + folderPath + "\n");
            } catch (IOException ex) {
                SB_EXCEPTION_LOGGER.log(Level.SEVERE, "Init Bridge, Create Directories", ex);
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * Initiates the conversion and watch processes
     *
     * @throws java.io.IOException If something happened
     */
    public void initBridge() throws IOException {
        logger.log(Level.INFO, "\n\nAbout to Mirror Directories");
        mirrorDirectories(completed);
        //Purge Non .opus files in the space-bridge directory
        logger.log(Level.INFO, "About to scan for non .opus files");
        Files.walk(completed, FileVisitOption.FOLLOW_LINKS).filter(path -> !Files.isDirectory(path)
                && !path.getFileName().toString().contains(".opus")).forEach(path -> {
            FileUtils.deleteQuietly(path.toFile());
            logger.log(Level.INFO, "{0} deleted", path);
        });
        logger.log(Level.INFO, "\n\nMirroring Process Complete");
        logger.log(Level.INFO, "\n\nAbout to Mirror And Convert Files to lower bitrate");
        //Check if Converted Files Exist Already, if Not Convert Them
        //No Directories, File Must be newPath expected file type, File Should Not Already Be Indexed
        fileWalkFilterDuplicatesOnlyFiles(completed).filter(path -> stringContains(path.getFileName().toString(), ".opus",
                ".mp3", ".ogg") && existInSpaceBridge(path, completed))
                .forEachOrdered(file -> {
                    Future<Path> future = StellarHyperspace.getHyperspace().submit(() -> {
                        //Doesn't Exist in Destination, Convert to 120K
                        StellarOPUSConverter converter = new StellarOPUSConverter(file, getCopyPath(file, completed).getParent());
                        Path path = converter.decreaseBitrate();
                        logger.log(Level.INFO, "\nFile Convertion Complete: " + path + "\n");
                        return path;
                    });
                });
        logger.log(Level.INFO, "All Conversion Tasks Submitted to Hyperspace");
        //Shut Down, then Await Termination newPath Hyperspace Task Executor
        shutdownBridge();
        logger.log(Level.INFO, "Space-Bridge Conversions Complete!");
    }

    /**
     * Shuts down the executors.
     */
    private static void shutdownBridge() {
        try {
            StellarHyperspace.initiateFalseVacuum();
        } catch (InterruptedException ex) {
            SB_EXCEPTION_LOGGER.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Checks a path to see if its metadata attribute title matches the
     * space-bridge title.
     *
     * @param path The path to investigate
     * @return Whether or not this is true
     * @throws IOException If something went wron
     */
    private boolean filterByFileName(Path path) throws IOException {
        //Or could exist in the filename metadata format itself
        return existInSpaceBridge(newPath(path.getParent(),
                StellarOPUSConverter.generateMetadata(path).get(StellarOPUSConverter.MetadataType.TITLE) + ".opus"),
                reIndexingFolder);
    }

    /**
     * Checks a path to see if its metadata attribute title matches the
     * space-bridge title.
     *
     * @param path The path to investigate
     * @return Whether or not this is true
     * @throws IOException If something went wron
     */
    private boolean filterByFileAttributes(Path path) throws IOException {
        //Could be Author - Title or have title field set in metadata attribute fields
        return existInSpaceBridge(newPath(path.getParent(),
                StellarDiskManager.getMetadata(path).get(StellarOPUSConverter.MetadataType.TITLE) + ".opus"), reIndexingFolder);
    }

    /**
     * Initiates the ReIndexing bridge which uses
     * {@link StellarOPUSConverter#reIndexOPUSFile()} to apply the latest
     * version of conversion to older .opus libraries.
     *
     * @param bitrate The bitrate for the converted files
     * @throws java.io.IOException If something went wrong
     */
    public void initReIndexingBridge(int bitrate) throws IOException {
        //Mirror Directories, We'll do this in the ReIndexing Folder
        mirrorDirectories(reIndexingFolder);
        //Reindex All Files & Put them in the ReIndexing Folder
        fileWalkFilterDuplicatesOnlyFiles(reIndexingFolder).filter(path -> {
            try {
                //If either one exists in space-bridge folder, the overall statement is false, do not convert this file
                return !(filterByFileAttributes(path) || filterByFileName(path) || existInSpaceBridge(path, reIndexingFolder));
            } catch (IOException ex) {
                SB_EXCEPTION_LOGGER.log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        })
                .forEachOrdered(path -> {
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
        shutdownBridge();
    }
}
