/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
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
     * The location of the help text file.
     */
    public static final Path HELP_TEXT_PATH = Paths.get(CONFIGURATION_FOLDER.toString(), "Help Text.dat");

    /**
     * The user directory.
     */
    public static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

    /**
     * The default object mapper for this application.
     */
    public static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Gets the help text from the help text file.
     *
     * @return The string containing all the text
     * @throws IOException If the help file is missing
     */
    public static String readHelpText() throws IOException {
        if (Files.notExists(HELP_TEXT_PATH)) {
            throw new IllegalStateException("Help files not found in their home directory");
        } else {
            return new String(Files.readAllBytes(HELP_TEXT_PATH));
        }
    }

    /**
     * Sets up the file system, adds the shutdown hook, and gets the default
     * configuration.
     *
     * @return The default configuration
     * @throws IOException If something happened
     */
    private static PreviousState initialSetUp() throws IOException {
        try {
            //If File Doesn't Exist Create it & Set Default State
            if (Files.notExists(CONFIGURATION_FOLDER)) {
                Files.createDirectories(CONFIGURATION_FOLDER);
            }
            if (Files.notExists(PREVIOUS_CONFIGURATION)) {
                //Set Previous State, by default is the program folder
                PreviousState state = new PreviousState(StellarUI.getOutputFolderFor("Converted Files")
                        .orElse(USER_DIR),
                        StellarUI.getOutputFolderFor("Picture Folder").orElse(USER_DIR));
                mapper.writeValue(PREVIOUS_CONFIGURATION.toFile(), state);
                System.out.println("The output directory is now set to: " + state.getOutputFolder()
                        + "\nThe picture output directory is set to: " + state.getPictureOutputFolder());
                return state;
            } else {
                return mapper.readValue(PREVIOUS_CONFIGURATION.toFile(), PreviousState.class);
            }
        } catch (InvalidDefinitionException | UnrecognizedPropertyException exc) {
            //JSON Exception Happened, Delete Settings & Reset Configuration Folder & Try Again
            FileUtils.deleteQuietly(PREVIOUS_CONFIGURATION.toFile());
            return initialSetUp();
        }
    }

    /**
     * Changes the picture output folder to the one specified.
     *
     * @param newPath The New Folder Path
     */
    public static synchronized void changePictureOutputFolder(Path newPath) {
        pictureOutputFolder = newPath;
    }

    /**
     * Changes the output folder location and writes the new location to the
     * disk.
     *
     * @param newOutputFolder The path of the output folder
     * @throws java.io.IOException If something happened
     */
    public static synchronized void changeOutputFolder(Path newOutputFolder) throws IOException {
        outputFolder = newOutputFolder;
    }

    private static Path outputFolder = USER_DIR, pictureOutputFolder = USER_DIR;
    private static final Path tempDirectory;

    static {
        try {
            PreviousState state = initialSetUp();
            outputFolder = state.getOutputFolder();
            pictureOutputFolder = state.getPictureOutputFolder();
            if (pictureOutputFolder == null) {
                pictureOutputFolder = StellarUI.getOutputFolderFor("Picture Output Folder").orElse(USER_DIR);
                System.out.println("Picture Output Folder Changed to: " + pictureOutputFolder);
            }
            //Create Temp Directory & Set Deletion Hook
            tempDirectory = Files.createTempDirectory("Stellar OPUS Converter Temporary Directory");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    //Delete Temp Folder =D
                    FileUtils.deleteQuietly(tempDirectory.toFile());
                    //Save Settings
                    mapper.writeValue(PREVIOUS_CONFIGURATION.toFile(), new PreviousState(outputFolder, pictureOutputFolder));
                } catch (IOException ex) {
                    Logger.getLogger(StellarDiskManager.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Oh Noes! The cleanup thread had an exception!\n\n");
                    throw new RuntimeException(ex);
                }
            }, "Stellar OPUS Converter Cleanup Thread"));
        } catch (IOException ex) {
            throw new IllegalStateException("We encountered an IO error, the disk might be full."
                    + " We couldn't create our temporary directory", ex);
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

    /**
     * Copies the selected file from the temp directory to the output folder.
     *
     * @param fileName The filename to move
     * @throws IOException If something went wrong
     */
    public void copyFromTemp(String fileName) throws IOException {
        System.out.println("\n\nFile Copy Exists in Temp Folder: " + Files.exists(Paths.get(tempDirectory.toString(), fileName)));
        Files.copy(Paths.get(tempDirectory.toString(), fileName),
                Paths.get(this.outputFolder.toString(), fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Gets the picture output folder path.
     *
     * @return The path
     */
    public Path getPictureOutputFolder() {
        return pictureOutputFolder;
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class PreviousState {

        @JsonProperty("outputFolder")
        private final Path outputFolder;

        @JsonProperty("pictureOutputFolder")
        private final Path pictureOutputFolder;

        /**
         * Creates a new previous state with the specified output folder.
         *
         * @param outputFolder
         */
        PreviousState(Path outputFolder) {
            this(outputFolder, Paths.get(""));
        }

        /**
         * Creates a new previous state with the specified output folder.
         *
         * @param outputFolder
         */
        @JsonCreator
        PreviousState(@JsonProperty("outputFolder") Path outputFolder,
                @JsonProperty("pictureOutputFolder") Path pictureOutputFolder) {
            this.outputFolder = outputFolder;
            this.pictureOutputFolder = pictureOutputFolder;
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
         * Gets the picture output folder.
         *
         * @return The picture output folder
         */
        public Path getPictureOutputFolder() {
            return this.pictureOutputFolder;
        }
    }
}
