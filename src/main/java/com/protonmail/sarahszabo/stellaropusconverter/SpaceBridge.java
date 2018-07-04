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
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;

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
            //System.setOut(new PrintStream(log.toFile()));
            //System.setErr(new PrintStream(log.toFile()));
            enterLog("SpaceBridge Initial Setup Complete!");
        } catch (IOException ex) {
            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
            enterExceptionIntoLog("Constructor", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Enters an exception into the permanant log.
     *
     * @param location The code location where the exception occurred.
     * @param ex The exception
     */
    private void enterExceptionIntoLog(String location, Exception ex) {
        enterLog("Exception Encountered <" + location + ">: " + ex.getMessage());
    }

    /**
     * Enters a log into the log file with the timestamp
     *
     * @param log The log to enter
     */
    private void enterLog(String log) {
        System.out.println(log + " || " + LocalDateTime.now());
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
     * Gets a stream of the walk of the watched directory minus our folders.
     *
     * @return The stream
     * @throws IOException If something went wrong
     */
    private Stream<Path> fileWalkFilterUs() throws IOException {
        return Files.walk(this.watching).filter(path -> !path.toString().contains(this.completed.getFileName().toString()));
    }

    /**
     * Initiates the conversion and watch processes
     *
     * @throws java.io.IOException If something happened
     */
    public void initBridge() throws IOException {
        //Mirror Directories
        fileWalkFilterUs().filter(path -> Files.isDirectory(path)).forEachOrdered(folder -> {
            try {
                Files.createDirectories(getCopyPath(folder));
            } catch (IOException ex) {
                Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                enterExceptionIntoLog("Init Bridge, Create Directories", ex);
                throw new RuntimeException(ex);
            }
        });
        //Check if Converted Files Exist Already, if Not Convert Them
        //No Directories, File Must be .opus, File Should Not Already Be Indexed
        fileWalkFilterUs().filter(path -> !Files.isDirectory(path) && path.getFileName().toString().contains(".opus")
                && !Files.exists(getCopyPath(path)))
                .forEachOrdered(file -> {
                    Future<Path> future = StellarHyperspace.getHyperspace().submit(() -> {
                        //Doesn't Exist, Convert to 120K
                        StellarOPUSConverter converter = new StellarOPUSConverter(file, getCopyPath(file).getParent());
                        return converter.decreaseOPUSBitrate();
                    });
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
