/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;
import static com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField.*;
import org.apache.commons.io.FileUtils;

/**
 * A class that oversees conversions in a certain folder and converts them to
 * 120K mobile formats.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public enum SpaceBridge {

    /**
     * The instance of space-bridge.
     */
    SPACE_BRIDGE;

    private final StellarDiskManager diskManager = StellarDiskManager.DISKMANAGER;
    private final Path watching, completed, logFolder, log;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SpaceBridge.class);

    /**
     * Constructor for {@link SpaceBridge}. Sets up the initial state.
     */
    private SpaceBridge() {
        try {
            StellarDiskManager.DiskManagerState state = this.diskManager.getState();
            //The directory we're watching
            this.watching = state.getSpaceBridgeDirectory();
            //Our completed files go here
            this.completed = Paths.get(this.watching.toString(), "Space-Bridge Completed");
            this.logFolder = Paths.get("", "Log Files");
            this.log = Paths.get(this.logFolder.toString(), "Log.dat");
            Files.createDirectories(this.logFolder);
            if (Files.notExists(this.log)) {
                Files.createFile(this.log);
            }
            enterLog("SpaceBridge Initial Setup Complete!");
        } catch (IOException ex) {
            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
            enterExceptionIntoLog("Constructor", ex);
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
     * Gets a stream of the walk of the watched directory minus "Space-Bridge"
     * folders. Excludes all directories and only gets a stream of files.
     *
     * @return The stream
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterUsNoDirectories() throws IOException {
        return fileWalkFilterUs().filter(path -> !Files.isDirectory(path)).distinct();
    }

    /**
     * Gets a stream of the walk of the watched directory minus our folders.
     *
     * @return The stream
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterUs() throws IOException {
        return Files.walk(this.watching, FileVisitOption.FOLLOW_LINKS)
                .filter(path -> !path.toString().contains(this.completed.getFileName().toString())).distinct();
    }

    /**
     * Initiates the conversion and watch processes
     *
     * @throws java.io.IOException If something happened
     */
    public void initBridge() throws IOException {
        enterLog("\n\nAbout to Mirror Directories");
        //Mirror Directories
        fileWalkFilterUs().filter(path -> Files.isDirectory(path)).forEachOrdered(folder -> {
            try {
                Path folderPath = getCopyPath(folder);
                Files.createDirectories(folderPath);
                enterLog("Created Folder: " + folderPath);
            } catch (IOException ex) {
                Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                enterExceptionIntoLog("Init Bridge, Create Directories", ex);
                throw new RuntimeException(ex);
            }
        });
        //Purge Non .opus files in the space-bridge directory
        enterLog("About to scan for non .opus files");
        Files.walk(this.completed, FileVisitOption.FOLLOW_LINKS).filter(path -> !Files.isDirectory(path)
                && !path.getFileName().toString().contains(".opus")).forEach(path -> {
            FileUtils.deleteQuietly(path.toFile());
            enterLog(path + " deleted");
        });
        enterLog("\n\nMirroring Process Complete");
        enterLog("\n\nAbout to Mirror And Convert Files to 120K");
        //Check if Converted Files Exist Already, if Not Convert Them
        //No Directories, File Must be .opus, File Should Not Already Be Indexed
        fileWalkFilterUsNoDirectories().filter(path -> stringContains(path.getFileName().toString(), ".opus",
                ".mp3", ".ogg")
                && !Files.exists(getCopyPath(path)))
                .forEachOrdered(file -> {
                    Future<Path> future = StellarHyperspace.getHyperspace().submit(() -> {
                        String entry = enterLogString("About to Convert " + file);
                        //Doesn't Exist in Destination, Convert to 120K
                        StellarOPUSConverter converter = new StellarOPUSConverter(file, getCopyPath(file).getParent());
                        Path path = converter.decreaseBitrate();
                        enterLog("\nFile Convertion Complete: " + path);
                        return path;
                    });
                    enterLog("All Tasks Submitted Succesfully!");
                    StellarHyperspace.getSpaceBridge().submit(() -> {
                        try {
                            enterLog(future.get() + " Converted Succesfully!");
                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                            enterExceptionIntoLog("Space-Bridge Logging Thread", ex);
                        }
                    });

                });
        enterLog("Space-Bridge Conversions Complete!");
    }
}