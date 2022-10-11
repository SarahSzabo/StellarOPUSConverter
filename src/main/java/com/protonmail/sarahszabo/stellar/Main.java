/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar;

import com.protonmail.sarahszabo.stellar.conversions.StellarFFMPEGTimeStamp;
import com.protonmail.sarahszabo.stellar.conversions.converters.StellarOPUSConverter;
import com.protonmail.sarahszabo.stellar.conversions.converters.StellarStandardFormConverter;
import com.protonmail.sarahszabo.stellar.metadata.ConverterMetadataBuilder;
import com.protonmail.sarahszabo.stellar.util.StellarCLIUtils;
import com.protonmail.sarahszabo.stellar.util.StellarGravitonField;
import static com.protonmail.sarahszabo.stellar.util.StellarGravitonField.*;
import com.protonmail.sarahszabo.stellar.util.StellarGreatFilter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The main class.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class Main {

    /**
     * The version of the program.
     */
    public static final String VERSION = "1.4.21α";
    /**
     * The name of the program.
     */
    public static final String PROGRAM_NAME = "Stellar OPUS Conversion Library";
    /**
     * The full name and version number of the program.
     */
    public static final String FULL_PROGRAM_NAME = PROGRAM_NAME + " " + VERSION;

    /**
     * The logger for this class
     */
    public static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * @param args the command line arguments
     * @throws java.io.IOException If something went wrong
     */
    public static void main(String[] args) throws IOException {
        //We have to check greater than section first, then we check integer comparison
        //Odd Number of Entries >= 3 (URL, timestamp0 timestamp1 timestamp2 timestamp3
        if (args.length >= 3 && argIsLink(args[0]) && (args.length + 1) % 2 == 0
                && allStringsAreTimestamps(args)) {
            StellarMode.LINK_TIMESTAMPS.start(args);
        } //Rest of integer comparisons
        else if (args.length == 0) {
            System.out.println("There is no file specified!\n");
            printHelp();
        } //Direct Link Conversion
        else if (args.length == 1) {
            if (argIsLink(args[0])) {
                StellarMode.DIRECT_LINK.start(args);
            } //Init Graphical File Chooser
            else if (args[0].equalsIgnoreCase("-G")) {
                StellarMode.GRAPHICAL_FILE_CHOICE_DIRECT_LINKS.start(args);
            } //Get From Clipboard
            else if (args[0].equalsIgnoreCase("-CL")) {
                StellarMode.GET_FROM_CLIPBOARD.start(args);
            } else if (args[0].equalsIgnoreCase("-TO_STANDARD_FORM")) {
                handleToStandardForm();
            } //Initiate Region Scan, Applying all filters from grand filter
            else if (args[0].equalsIgnoreCase("Region-Scan")) {
                StellarGreatFilter.filterPaths(StellarCLIUtils.getFilesFromClipboard().get());
            } //Ask For Status of Folder Paths
            else if (args[0].equalsIgnoreCase("Status")) {
                System.out.println(StellarDiskManager.DISKMANAGER.getState());
            } //Initialise Space-Bridge
            else if (args[0].equalsIgnoreCase("Space-Bridge")) {
                stellarConversion(StellarMode.SPACE_BRIDGE, args);
            } else {
                printHelp();
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("-CL")) {
                //Set All Files  With the Same Picture
                if (args[1].equalsIgnoreCase("Picture-Select")) {
                    stellarConversion(StellarMode.PICTURE_SELECT, args);
                } //-CL With All Same Artist
                else {
                    stellarConversion(StellarMode.CLIPBOARD_SAME_ARTIST, args);
                }
            }//Change Settings
            else if (args[0].equalsIgnoreCase("Set")) {
                if (args[1].equalsIgnoreCase("Pictures-Folder")) {
                    Path path = StellarCLIUtils.getFolderFor("Picture Folder")
                            .orElse(StellarDiskManager.USER_DIR);
                    StellarDiskManager.setPictureOutputFolder(path);
                    messageThenExit("Picture Folder Changed To:" + path);
                } else if (args[1].equalsIgnoreCase("Output-Folder")) {
                    Path path = StellarCLIUtils.getFolderFor("Output Folder")
                            .orElse(StellarDiskManager.USER_DIR);
                    StellarDiskManager.setOutputFolder(path);
                    messageThenExit("Output Folder Changed To: " + path);
                } else if (args[1].equalsIgnoreCase("Space-Bridge-Folder")) {
                    Path path = StellarCLIUtils.getFolderFor("Space-Bridge Folder")
                            .orElse(StellarDiskManager.USER_DIR);
                    StellarDiskManager.setSpaceBridgeDirectory(path);
                    messageThenExit("Space-Bridge Folder Changed To: " + path);
                } //Set Picture Metadata
                if (args[1].equalsIgnoreCase("Album-Art")) {
                    Path imageFile = StellarCLIUtils.getFile("Select an Image File",
                            StellarCLIUtils.EXTENSION_FILTER.PICTURE_FILES).orElseGet(() -> {
                                return StellarDiskManager.getGenericPicture();
                            });
                    List<Path> files = StellarCLIUtils.getFilesFromClipboard().orElse(Collections.emptyList());
                    if (files.isEmpty()) {
                        throw new IllegalStateException("Attempt to set album art with null clipboard opus file or null image selected");
                    }
                    files.parallelStream().forEach(path -> {
                        try {
                            //Get metadata from file
                            ConverterMetadataBuilder metadata
                                    = new ConverterMetadataBuilder(StellarDiskManager.getMetadata(path));
                            //Set to new image
                            metadata.albumArtPath(imageFile);
                            //Map new metadata to opus file
                            StellarOPUSConverter converter = new StellarOPUSConverter(StellarGravitonField.newPath(path),
                                    StellarDiskManager.getOutputFolder(), metadata.buildMetadata());
                            converter.convertToOPUS();
                        } catch (IOException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });

                }
            } else {
                printHelp();
            }
        } else if (args.length == 3) {
            //Manually Specify Author/Title with Timestamps
            if (argIsLink(args[0]) && !allStringsAreTimestamps(args)) {
                stellarConversion(StellarMode.LINK_AUTHOR_TITLE, args);
            } else {
                printHelp();
            }
        } else {
            printHelp();
        }
        StellarCLIUtils.shutdownUI();
        System.exit(0);
    }

    /**
     * Scans the current directory for malformatted filenames: multiple "-"
     * characters. Exits if it finds one.
     */
    private static void scanForMalformattedFilenames(Path currentDir) throws IOException {
        System.out.println("\nBeginning Scan for Malformatted Filenames");
        var files = Files.list(currentDir);
        var malformedList = new ArrayList<Path>(100);
        files.parallel().map(path -> new StellarStandardFormConverter(path, path.getParent()))
                .filter(converter -> !converter.isConversionCandidate())
                .forEach(converter -> malformedList.add(converter.getInputFile()));
        if (!malformedList.isEmpty()) {
            StellarGravitonField.messageThenExit("The following files are malformed (multiple \"-\" characters):\n\n"
                    + malformedList.stream().map(Path::toString).collect(Collectors.joining("\n"))
                    + "\n\nIndexing Aborted");
        } else {
            System.out.println("Scan completed. No malformed filenames detected\n");
        }
    }

    /**
     * Handles the file rename request by setting the filename to standard form
     * for the entire directory.
     */
    private static void handleToStandardForm() throws IOException {
        //TODO: If the title tag has ARTIST - TITLE in it, auto-correct to just TITLE, same for the artist tag field
        var currentDir = System.getProperty("user.dir");
        System.out.println("Current Directory: " + currentDir);
        //Scan for malformatted filenames <multiple "-">
        scanForMalformattedFilenames(Paths.get(currentDir));

        //UI for confirmation
        var response = StellarCLIUtils.showConfirmationDialog("Current Directory: " + currentDir
                + "\n\nAre all the files in this folder in ARTIST - FILENAME.EXTENSION format and are ready to be changed?");
        if (response) {
            //List for duplicate files to watch out for at the end
            var duplicateList = new ArrayList<Path>(10);
            Files.list(Paths.get(currentDir)).parallel().map(path -> new StellarStandardFormConverter(path, path.getParent()))
                    .forEach(converter -> {
                        converter.convert();
                        if (converter.hadDuplicate()) {
                            duplicateList.add(converter.getDestinationFile());
                        }
                    });

            //Report duplicate files
            hyperlightMessage("Duplicate Files Observed: \n" + duplicateList.stream().map(Path::toString).collect(Collectors.joining("\n")));
        }
    }

    /**
     * Checks to ensure that all timestamps are in timestamp format. You may
     * pass the URL/Path into this method, it will ignore it and send the
     * substring instead of just the timestamps.
     *
     * @param timestamps The string of timestamps, or suspected timestamps
     * @return If they all match timstamp format
     */
    private static boolean allStringsAreTimestamps(String[] args) {
        if (argIsLink(args[0])) {
            return allStringsAreTimestampsActual(Arrays.copyOfRange(args, 1, args.length));
        } else {
            return allStringsAreTimestampsActual(args);
        }
    }

    /**
     * Checks to ensure that all timestamps are in timestamp format.
     *
     * @param timestamps The string of timestamps, or suspected timestamps
     * @return If they all match timstamp format
     */
    private static boolean allStringsAreTimestampsActual(String[] timestamps) {
        return Arrays.asList(timestamps).stream().allMatch(StellarFFMPEGTimeStamp::matchesFFmpegTimestampFormat);
    }

    /**
     * Checks if the argument is a link or not.
     *
     * @param arg The argument
     * @return If it is a link
     */
    private static boolean argIsLink(String arg) {
        return Files.exists(Paths.get(arg)) || arg.contains("http");
    }

    /**
     * Prints the help doc.
     */
    private static void printHelp() throws IOException {
        String message = StellarDiskManager.readHelpText();
        message += "\n\n" + StellarDiskManager.getState();
        message += "\n\n" + Main.FULL_PROGRAM_NAME;
        messageThenExit(message);
    }

    /**
     * Convert using the specified conversion mode.
     *
     * @param mode The mode to use
     */
    private static void stellarConversion(StellarMode mode, String... args) throws IOException {
        mode.start(args);
    }
}
