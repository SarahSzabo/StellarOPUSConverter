/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.commons.io.FileUtils;

/**
 * The file disk manager. This class is responsible for all disk operations
 * performed by the program.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public enum StellarDiskManager {
    /**
     * The disk manager instance.
     */
    DISKMANAGER;
    /**
     * The configuration folder
     */
    public static final Path CONFIGURATION_FOLDER = Paths.get("Configuration");
    /**
     * The previous state file
     */
    public static final Path PREVIOUS_CONFIGURATION = Paths.get(CONFIGURATION_FOLDER.toString(), "Previous Configuration.json");

    /**
     * The user directory.
     */
    public static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

    /**
     * Sets up the file system, adds the shutdown hook, and gets the default
     * configuration.
     *
     * @return The default configuration
     * @throws IOException If something happened
     */
    private static PreviousState initialSetUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        //If File Doesn't Exist Create it & Set Default State
        if (Files.notExists(CONFIGURATION_FOLDER)) {
            Files.createDirectories(CONFIGURATION_FOLDER);
            Files.createFile(PREVIOUS_CONFIGURATION);
            //Set Previous State, by default is the program folder
            PreviousState state = new PreviousState(StellarUI.getOutputFolder().orElse(USER_DIR));
            mapper.writeValue(PREVIOUS_CONFIGURATION.toFile(), state);
            System.out.println("The output directory is now set to: " + state.getOutputFolder());
        }
        return mapper.readValue(PREVIOUS_CONFIGURATION.toFile(), PreviousState.class);
    }

    /**
     * Changes the output folder location. Does not write the new location to
     * the disk for use next time.
     *
     * @param newOutputFolder The path of the output folder
     * @return The new disk manager for this folder
     */
    public static synchronized StellarDiskManager changeOutputFolderNonPermanant(Path newOutputFolder) throws IOException {
        outputFolder = newOutputFolder;
        return DISKMANAGER;
    }

    /**
     * Changes the output folder location and writes the new location to the
     * disk.
     *
     * @param newOutputFolder The path of the output folder
     * @return The new disk manager for this folder
     * @throws java.io.IOException If something happened
     */
    public static synchronized StellarDiskManager changeOutputFolder(Path newOutputFolder) throws IOException {
        outputFolder = newOutputFolder;
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(PREVIOUS_CONFIGURATION.toFile(), new PreviousState(newOutputFolder));
        return DISKMANAGER;
    }

    private static Path outputFolder = USER_DIR;
    private static final Path tempDirectory;

    static {
        try {
            PreviousState state = initialSetUp();
            outputFolder = state.getOutputFolder();
            //Create Temp Directory & Set Deletion Hook
            tempDirectory = Files.createTempDirectory("Stellar OPUS Converter Temporary Directory");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                FileUtils.deleteQuietly(tempDirectory.toFile());
            }, "Stellar OPUS Converter Cleanup Thread"));
        } catch (IOException ex) {
            throw new IllegalStateException("We encountered an IO error, the disk might be full."
                    + " We couldn't create our temporary directory");
        }
    }

    /**
     * Copies a file to the temporary directory from the specified directory.
     *
     * @param filePath The file to copy
     * @throws IOException If something happened
     */
    public void copyToTemp(Path filePath) throws IOException {
        System.out.println(tempDirectory);
        Files.copy(filePath, Paths.get(tempDirectory.toString(), filePath.getFileName().toString()),
                StandardCopyOption.REPLACE_EXISTING);
    }

    public void copyFromTemp(String fileName) throws IOException {
        Files.copy(Paths.get(tempDirectory.toString(), fileName),
                Paths.get(this.outputFolder.toString(), fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Gets the output folder.
     *
     * @return The output folder
     */
    public Path getOutputFolder() {
        return outputFolder;
    }

    /**
     * Gets the temporary working directory path.
     *
     * @return The temporary directory path
     */
    public Path getTempDirectory() {
        return tempDirectory;
    }

    /**
     * An abstraction representing the previous state of the program.
     */
    static final class PreviousState {

        private Path outputFolder;

        /**
         * Default constructor for Jackson (JSON).
         */
        public PreviousState() {
            this.outputFolder = Paths.get("");
        }

        /**
         * Creates a new previous state with the specified output folder.
         *
         * @param outputFolder
         */
        PreviousState(Path outputFolder) {
            this.outputFolder = outputFolder;
        }

        /**
         * A getter for the output folder.
         *
         * @return The output folder
         */
        public Path getOutputFolder() {
            return this.outputFolder;
        }

        /**
         * Sets the default folder path.
         *
         * @param outputFolder The folder path
         */
        public void setOutputFolder(Path outputFolder) {
            this.outputFolder = outputFolder;
        }
    }
}
