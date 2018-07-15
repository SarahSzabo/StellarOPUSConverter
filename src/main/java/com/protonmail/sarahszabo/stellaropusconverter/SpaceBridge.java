/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import static com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField.*;
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarLoggingFormatter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final Path watching, completed, logFolder, logFile;
    private final Logger logger = StellarLoggingFormatter.forClass(SpaceBridge.class);

    /**
     * Constructor for {@link SpaceBridge}. Sets up the initial state.
     */
    private SpaceBridge() {
        try {
            StellarDiskManager.DiskManagerState state = StellarDiskManager.getState();
            //The directory we're watching
            this.watching = state.getSpaceBridgeDirectory();
            //Our completed files go here
            this.completed = Paths.get(this.watching.toString(), "Space-Bridge Completed");
            this.logFolder = Paths.get("", "Log Files");
            this.logFile = Paths.get(this.logFolder.toString(), "Log.dat");
            Files.createDirectories(this.logFolder);
            if (Files.notExists(this.logFile)) {
                Files.createFile(this.logFile);
            }
            this.logger.log(Level.INFO, "SpaceBridge Initial Setup Complete!");
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
     * @return The mirrored path
     */
    private Path getCopyPath(Path current) {
        //If watching is a/b and current is a/b/c/d, get path for SpaceBridge/c/d
        return Paths.get(this.completed.toString(), this.watching.relativize(current).toString());
    }

    /**
     * Gets a stream newPath the walk newPath the watched directory minus
     * "Space-Bridge" folders. Excludes all directories and only gets a stream
     * newPath files.
     *
     * @return The stream
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterUsNoDirectories() throws IOException {
        return fileWalkFilterUs().filter(path -> !Files.isDirectory(path)).distinct();
    }

    /**
     * Gets a stream newPath the walk newPath the watched directory minus our
     * folders.
     *
     * @return The stream
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterUs() throws IOException {
        return Files.walk(this.watching, FileVisitOption.FOLLOW_LINKS)
                .filter(path -> !path.toString().contains(this.completed.getFileName().toString())).distinct();
    }

    /**
     * Checks whether or not the file exists in the space-bridge folder. NOTE:
     * Only .opus files are allowed in the space-bridge folder.
     *
     * @param path The path newPath the file
     * @return Whether or not the file exists in the space-bridge folder
     */
    private boolean fileDoesnExistInSpaceBridgeFolder(Path path) {
        //Replace Possible Other Filenames, All Files are Just .opus in the Space-Bridge Directory
        //Check to See If They Exist
        String fileName = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf("."));
        Path newPath = getCopyPath(Paths.get(path.getParent().toString(), fileName + ".opus"));
        logger.fine("Space-Bridge Path: " + newPath);
        logger.fine("Original: " + path);
        logger.fine("Exists? " + Files.exists(newPath));
        return Files.notExists(getCopyPath(Paths.get(path.getParent().toString(), fileName + ".opus")));
    }

    /**
     * Initiates the conversion and watch processes
     *
     * @throws java.io.IOException If something happened
     */
    public void initBridge() throws IOException {
        this.logger.log(Level.INFO, "\n\nAbout to Mirror Directories");
        //Mirror Directories
        fileWalkFilterUs().filter(path -> Files.isDirectory(path)).forEachOrdered(folder -> {
            try {
                Path folderPath = getCopyPath(folder);
                Files.createDirectories(folderPath);
                this.logger.log(Level.INFO, "Created Folder: " + folderPath + "\n");
            } catch (IOException ex) {
                Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                this.logger.log(Level.SEVERE, "Init Bridge, Create Directories", ex);
                throw new RuntimeException(ex);
            }
        });
        //Purge Non .opus files in the space-bridge directory
        this.logger.log(Level.INFO, "About to scan for non .opus files");
        Files.walk(this.completed, FileVisitOption.FOLLOW_LINKS).filter(path -> !Files.isDirectory(path)
                && !path.getFileName().toString().contains(".opus")).forEach(path -> {
            FileUtils.deleteQuietly(path.toFile());
            this.logger.log(Level.INFO, "{0} deleted", path);
        });
        this.logger.log(Level.INFO, "\n\nMirroring Process Complete");
        this.logger.log(Level.INFO, "\n\nAbout to Mirror And Convert Files to 120K");
        //Check if Converted Files Exist Already, if Not Convert Them
        //No Directories, File Must be newPath expected file type, File Should Not Already Be Indexed
        fileWalkFilterUsNoDirectories().filter(path -> stringContains(path.getFileName().toString(), ".opus",
                ".mp3", ".ogg") && fileDoesnExistInSpaceBridgeFolder(path))
                .forEachOrdered(file -> {
                    Future<Path> future = StellarHyperspace.getHyperspace().submit(() -> {
                        //Doesn't Exist in Destination, Convert to 120K
                        StellarOPUSConverter converter = new StellarOPUSConverter(file, getCopyPath(file).getParent());
                        Path path = converter.decreaseBitrate();
                        this.logger.log(Level.INFO, "\nFile Convertion Complete: " + path + "\n");
                        return path;
                    });
                });
        this.logger.log(Level.INFO, "All Conversion Tasks Submitted to Hyperspace");
        //Shut Down, then Await Termination newPath Hyperspace Task Executor
        try {
            StellarHyperspace.initiateFalseVacuum();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        this.logger.log(Level.INFO, "Space-Bridge Conversions Complete!");
    }
}
