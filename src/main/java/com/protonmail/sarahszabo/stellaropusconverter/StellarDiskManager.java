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
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import static com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField.*;
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarLoggingFormatter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
     * The user directory.
     */
    public static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));
    /**
     * The configuration folder
     */
    public static final Path CONFIGURATION_FOLDER = newPath(USER_DIR, "Configuration");
    /**
     * The path to the help file containing all the help documentation for the
     * program.
     */
    public static final Path HELP_FILE = CONFIGURATION_FOLDER.resolve(Paths.get("Help Text.dat"));
    /**
     * The previous state file
     */
    public static final Path PREVIOUS_CONFIGURATION = Paths.get(CONFIGURATION_FOLDER.toString(), "Previous State.json");

    /**
     * The path to the default pictures folder.
     */
    public static final Path DEFAULT_PICTURES = newPath(CONFIGURATION_FOLDER, "Default Pictures");
    /**
     * The folder for system pictures.
     */
    public static final Path SYSTEM_PICTURES = CONFIGURATION_FOLDER.resolve("System Pictures");

    /**
     * Stellar's Icon.
     */
    public static final Path SYSTEM_ICON = SYSTEM_PICTURES.resolve("Stellar Icon.png");
    /**
     * The default object mapper for this application.
     */
    public static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = StellarLoggingFormatter.forClass(StellarDiskManager.class);

    /**
     * Gets the full folder of generic picture files.
     *
     * @return The read-only collection of picture files
     */
    public static Collection<Path> getGenericPictures() {
        try {
            return Files.list(DEFAULT_PICTURES).collect(Collectors.toUnmodifiableList());
        } catch (IOException ex) {
            Logger.getLogger(StellarDiskManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Generic Picture Folder Missing", ex);
        }
    }

    /**
     * Selects a random picture from our cache of generic cover art.
     *
     * @return The path to the cover art
     */
    public static Path getGenericPicture() {
        List<Path> pictures = new ArrayList<>(getGenericPictures());
        Path random = pictures.get(new Random().nextInt(pictures.size()));
        return random;
    }

    /**
     * Generates the album art image from the opus file, and puts it in the
     * output folder. NOTE: The .dat file may be 0 Bytes if there was no image
     * file in the .opus file. This file contains binary image information.
     *
     * @param path The path of the .dat file
     * @param partialMetadata The partial (Only artist/title) metadata
     * @return The picture file generated
     */
    private static Path generateImagefromFile(Path path) throws IOException {
        //Decompose Picture Stored as Binary File
        String noExtension = StellarOPUSConverter.stripFileExtension(path);
        Path imageFile = newPath(REINDEXING_FOLDER, noExtension + ".png");
        processOP(true, imageFile, REINDEXING_FOLDER, "exiftool", "-Picture", "-b", path.getFileName().toString());
        return imageFile;
    }

    /**
     * Gets a list of metadata from an already existing file on the disk.
     *
     * @param path The path of the opus file
     * @return The metadata of the opus file
     * @throws RuntimeException If something went wrong in I/O
     */
    public static ConverterMetadata getMetadata(Path path) {
        try {
            Path metadataFilePath = newPath(REINDEXING_FOLDER, StellarOPUSConverter.stripFileExtension(path) + ".txt");
            //Import .Opus File to ReIndexing Directory, Use Reindexing folder to avoid name clashes
            Path reindexingOpusFile = newPath(REINDEXING_FOLDER, path.getFileName());
            Files.createDirectories(REINDEXING_FOLDER);
            synchronized (StellarDiskManager.class) {
                if (Files.notExists(reindexingOpusFile)) {
                    Files.copy(path, reindexingOpusFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            //Create Metadata file
            processOP(true, metadataFilePath, REINDEXING_FOLDER, "exiftool", path.getFileName().toString());
            //Trim Entries from exiftool
            List<String> lines = Files.readAllLines(metadataFilePath).stream().collect(Collectors.toList());
            ConverterMetadataBuilder metadata
                    = new ConverterMetadataBuilder(StellarOPUSConverter.getDefaultMetadata());
            for (String str : lines) {
                String contents = str.split(":")[0];
                if (contents.matches("Artist\\s+")) {
                    //Data is always after the : character
                    String artist = preferredTitleFormat(str.split(":")[1].trim());
                    //Map Behaviour overwrites existing entry
                    metadata.artist(artist);
                } else if (contents.matches("Title\\s+")) {
                    //Data is always after the : character
                    String title = preferredTitleFormat(str.split(":")[1].trim());
                    //Map Behaviour overwrites existing entry
                    metadata.title(title);
                } else if (contents.matches("Picture\\s+")) {
                    Path picturePath = generateImagefromFile(path);
                    //Attempt Picture Reconstruction
                    //If the picture file is less than 100 bytes, there was no image data in the .opus file, we'll have to ask the user for it
                    if (Files.size(picturePath) < 100) {
                        //Put default metadata into metadata map
                        metadata.albumArtPath(StellarOPUSConverter.getDefaultMetadata().getAlbumArtPath());
                    } else {
                        //Get Picture File Path & Insert into Map
                        metadata.albumArtPath(picturePath);
                    }
                } else if (contents.matches(StellarOPUSConverter.MetadataType.DATE + "\\s+")) {
                    //Data is always after the : character
                    String date = preferredTitleFormat(str.split(":")[1].trim());
                    //Map Behaviour overwrites existing entry
                    metadata.date(LocalDate.parse(date, StellarOPUSConverter.DATE_FORMATTER));

                } else if (contents.matches(StellarOPUSConverter.CREATED_BY_TAG + "\\s+")) {
                    //Data is always after the : character
                    String comment = preferredTitleFormat(str.split(":")[1].trim());
                    //Map Behaviour overwrites existing entry
                    metadata.createdBy(comment);
                } else if (contents.matches("Encoder Options\\s+")) {
                    //Data is always after the : character
                    int bitrate = Integer.parseInt(str.split("--bitrate ")[1].replace('k', ' ').trim());
                    //Map Behaviour overwrites existing entry
                    metadata.bitrate(bitrate);
                }
            }
            return metadata.buildMetadata();
        } catch (IOException ex) {
            Logger.getLogger(StellarDiskManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Gets the help text from the help text file.
     *
     * @return The string containing all the text
     * @throws IOException If the help file is missing
     */
    public static String readHelpText() throws IOException {
        //Old Method "In Jar":
        //Path helpTextPath  =  new File(StellarDiskManager.class.getResource("/Help Text.dat").toURI()).toPath();
        return new String(Files.readAllBytes(HELP_FILE));
    }

    /**
     * Sets up the file system, adds the shutdown hook, and gets the default
     * configuration.
     *
     * @return The default configuration
     * @throws IOException If something happened
     */
    private static DiskManagerState initialSetUp() throws IOException {
        try {
            //If File Doesn't Exist Create it & Set Default State
            if (Files.notExists(CONFIGURATION_FOLDER)) {
                Files.createDirectories(CONFIGURATION_FOLDER);
            }
            if (Files.notExists(PREVIOUS_CONFIGURATION)) {
                //Set Previous State, by default is the program folder
                DiskManagerState state = new DiskManagerState(StellarUI.getFolderFor("Converted Files")
                        .orElse(USER_DIR),
                        StellarUI.getFolderFor("Picture Folder").orElse(USER_DIR),
                        StellarUI.getFolderFor("Space Bridge").orElse(USER_DIR));
                mapper.writeValue(PREVIOUS_CONFIGURATION.toFile(), state);
                logger.info("The output directory is now set to: " + state.getOutputFolder()
                        + "\nThe picture output directory is set to: " + state.getPictureOutputFolder());
                return state;
            } else {
                return mapper.readValue(PREVIOUS_CONFIGURATION.toFile(), DiskManagerState.class);
            }
        } catch (InvalidDefinitionException | UnrecognizedPropertyException exc) {
            //JSON Exception Happened, Delete Settings & Reset Configuration Folder & Try Again
            FileUtils.deleteQuietly(PREVIOUS_CONFIGURATION.toFile());
            return initialSetUp();
        }
    }

    /**
     * Changes the space-bridge watch folder to the one specified.
     *
     * @param newPath The New Folder Path
     */
    public static synchronized void setSpaceBridgeDirectory(Path newPath) {
        spaceBridgeDirectory = newPath;
    }

    /**
     * Changes the picture output folder to the one specified.
     *
     * @param newPath The New Folder Path
     */
    public static synchronized void setPictureOutputFolder(Path newPath) {
        pictureOutputFolder = newPath;
    }

    /**
     * Changes the output folder location and writes the new location to the
     * disk.
     *
     * @param newOutputFolder The path of the output folder
     * @throws java.io.IOException If something happened
     */
    public static synchronized void setOutputFolder(Path newOutputFolder) throws IOException {
        outputFolder = newOutputFolder;
    }

    /**
     * The Space-Bridge Directory
     */
    private static Path outputFolder = USER_DIR, pictureOutputFolder = USER_DIR, spaceBridgeDirectory;
    /**
     * The path of the temp folder.
     */
    private static final Path tempDirectory;

    static {
        try {
            DiskManagerState state = initialSetUp();
            outputFolder = state.getOutputFolder();
            pictureOutputFolder = state.getPictureOutputFolder();
            spaceBridgeDirectory = state.getSpaceBridgeDirectory();
            //Create Temp Directory & Set Deletion Hook
            tempDirectory = Files.createTempDirectory("Stellar OPUS Converter Temporary Directory");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    //Delete Temp Folder =D
                    FileUtils.deleteQuietly(tempDirectory.toFile());
                    //Save Settings
                    mapper.writeValue(PREVIOUS_CONFIGURATION.toFile(), new DiskManagerState(outputFolder, pictureOutputFolder,
                            spaceBridgeDirectory));
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
     * The folder used for re-indexing operations.
     */
    public static final Path REINDEXING_FOLDER = newPath(StellarDiskManager.tempDirectory, "ReIndexing");

    /**
     * Copies a file to the temporary directory from the specified directory.
     *
     * @param filePath The file to copy
     * @return The file location after it has been copied over
     * @throws IOException If something happened
     */
    public static Path copyToReindexing(Path filePath) throws IOException {
        Path destination = StellarGravitonField.newPath(REINDEXING_FOLDER, filePath.getFileName());
        logger.fine(tempDirectory.toString());
        Files.copy(filePath, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    /**
     * Copies a file to the temporary directory from the specified directory.
     *
     * @param filePath The file to copy
     * @return The file location after it has been copied over
     * @throws IOException If something happened
     */
    public static Path copyToTemp(Path filePath) throws IOException {
        Path destination = StellarGravitonField.newPath(tempDirectory, filePath.getFileName());
        logger.fine(tempDirectory.toString());
        Files.copy(filePath, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    /**
     * Copies the selected file from the temp directory to the output folder.
     *
     * @param fileName The filename to move
     * @return The path to the file after it has been copied
     * @throws IOException If something went wrong
     */
    public static Path copyFromTemp(String fileName) throws IOException {
        Path destination = Paths.get(outputFolder.toString(), fileName);
        Files.copy(Paths.get(tempDirectory.toString(), fileName), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    /**
     * Gets the picture output folder path.
     *
     * @return The path
     */
    public static Path getPictureOutputFolder() {
        return pictureOutputFolder;
    }

    /**
     * Gets the output folder.
     *
     * @return The output folder
     */
    public static Path getOutputFolder() {
        return outputFolder;
    }

    /**
     * Gets the temporary working directory path.
     *
     * @return The temporary directory path
     */
    public static Path getTempDirectory() {
        return tempDirectory;
    }

    /**
     * Gets the current state.
     *
     * @return The current state of the disk manager
     */
    public static DiskManagerState getState() {
        return new DiskManagerState(outputFolder, pictureOutputFolder, spaceBridgeDirectory);
    }

    /**
     * An abstraction representing the previous state of the program.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class DiskManagerState {

        @JsonProperty("outputFolder")
        private final Path outputFolder;
        @JsonProperty("pictureOutputFolder")
        private final Path pictureOutputFolder;
        @JsonProperty("spaceBridgeDirectory")
        private final Path spaceBridgeDirectory;

        /**
         * Creates a new disk manager with the specified output folder.
         *
         * @param outputFolder
         */
        @JsonCreator
        DiskManagerState(@JsonProperty("outputFolder") Path outputFolder,
                @JsonProperty("pictureOutputFolder") Path pictureOutputFolder,
                @JsonProperty("spaceBridgeDirectory") Path spaceBridgeDirectory) {
            this.outputFolder = Objects.requireNonNull(outputFolder);
            this.pictureOutputFolder = Objects.requireNonNull(pictureOutputFolder);
            this.spaceBridgeDirectory = Objects.requireNonNull(spaceBridgeDirectory);
        }

        /**
         * A getter for the space bridge directory
         *
         * @return The path to the directory
         */
        public Path getSpaceBridgeDirectory() {
            return spaceBridgeDirectory;
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

        @Override
        public String toString() {
            return "Disk Manager State: \nOutput Folder: " + this.outputFolder
                    + "\nPictures Folder: " + this.pictureOutputFolder + "\nSpace-Bridge Output Folder: " + this.spaceBridgeDirectory;
        }

    }
}
