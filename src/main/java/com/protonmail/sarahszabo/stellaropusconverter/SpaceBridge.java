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
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.LogLevel;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.NebulaCartographer;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.StellarCartographer;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.TerrestrialCartographer;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.BufferingModule;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
    private final StellarCartographer<SpaceBridge> cartographer, exceptionCartographer;

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
            this.cartographer = new TerrestrialCartographer<SpaceBridge>("Space-Bridge Log.dat", LogLevel.DEFAULT,
                    Stream.of(new BufferingModule()).collect(Collectors.toList()));
            this.exceptionCartographer = new TerrestrialCartographer<>("Space-Bridge Exception Log.dat", LogLevel.EXCEPTION);
            this.cartographer.log("SpaceBridge Initial Setup Complete!");
        } catch (IOException ex) {
            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
            new NebulaCartographer<>(LogLevel.EXCEPTION).logException("Constructor", ex);
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
        this.cartographer.log("\n\nAbout to Mirror Directories");
        //Mirror Directories
        fileWalkFilterUs().filter(path -> Files.isDirectory(path)).forEachOrdered(folder -> {
            try {
                Path folderPath = getCopyPath(folder);
                Files.createDirectories(folderPath);
                this.cartographer.log("Created Folder: " + folderPath);
            } catch (IOException ex) {
                Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                this.exceptionCartographer.logException("Init Bridge, Create Directories", ex);
                throw new RuntimeException(ex);
            }
        });
        //Purge Non .opus files in the space-bridge directory
        this.cartographer.log("About to scan for non .opus files");
        Files.walk(this.completed, FileVisitOption.FOLLOW_LINKS).filter(path -> !Files.isDirectory(path)
                && !path.getFileName().toString().contains(".opus")).forEach(path -> {
            FileUtils.deleteQuietly(path.toFile());
            this.cartographer.log(path + " deleted");
        });
        this.cartographer.log("\n\nMirroring Process Complete");
        this.cartographer.log("\n\nAbout to Mirror And Convert Files to 120K");
        //Check if Converted Files Exist Already, if Not Convert Them
        //No Directories, File Must be of expected file type, File Should Not Already Be Indexed
        fileWalkFilterUsNoDirectories().filter(path -> stringContains(path.getFileName().toString(), ".opus",
                ".mp3", ".ogg")
                //Replace Possible Other Filenames, all files are just .opus in the Space-Bridge Directory
                && !Files.exists(getCopyPath(Paths.get(path.getParent().toString(), path.getFileName().toString()
                        .replace("[.][^.]+$", ".opus")))))
                .forEachOrdered(file -> {
                    Future<Path> future = StellarHyperspace.getHyperspace().submit(() -> {
                        //Doesn't Exist in Destination, Convert to 120K
                        StellarOPUSConverter converter = new StellarOPUSConverter(file, getCopyPath(file).getParent());
                        Path path = converter.decreaseBitrate();
                        this.cartographer.log("\nFile Convertion Complete: " + path);
                        return path;
                    });
                    StellarHyperspace.getSpaceBridge().submit(() -> {
                        try {
                            this.cartographer.log(future.get() + " Converted Succesfully!");
                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
                            this.exceptionCartographer.logException("Space-Bridge Logging Thread", ex);
                        }
                    });

                });
        this.cartographer.log("All Conversion Tasks Submitted to Hyperspace");
        //Shut Down, then Await Termination of Hyperspace Task Executor
        try {
            StellarHyperspace.initiateFalseVacuum();
            StellarHyperspace.getHyperspace().awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(SpaceBridge.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.cartographer.log("Space-Bridge Conversions Complete!");
    }
}
