/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

/**
 * The UI for the Stellar OPUS Converter. Must be shutdown via
 * {@link StellarUI#shutdownUI()}.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class StellarUI {

    private static final BlockingQueue<List<File>> PATHS_QUEUE = new ArrayBlockingQueue<>(2);
    private static final BlockingQueue<File> OUTPUT_FOLDER_QUEUE = new ArrayBlockingQueue<>(2);
    private static final BlockingQueue<Optional<String>> ASK_USER_METADATA = new ArrayBlockingQueue<>(2);

    static {
        //Initialize Toolkit
        new JFXPanel();
        Platform.setImplicitExit(false);
    }

    /**
     * Shuts down the UI.
     */
    public static void shutdownUI() {
        Platform.exit();
    }

    /**
     * Gets the files from the clipboard.
     *
     * @return The files or null if none
     */
    public static List<File> getFilesFromClipboard() {
        try {
            BlockingQueue<List<File>> queue = new ArrayBlockingQueue<>(1);
            Platform.runLater(() -> {
                try {
                    queue.put(Clipboard.getSystemClipboard().getFiles());
                } catch (InterruptedException ex) {
                    Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            return queue.take();
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Asks the user for the artist/title combination. By convention, the
     * zeroith element is the artist, and the first is the title. If the user
     * aborts the operations, Unknown Artist/Unknown Title is returned.
     *
     * @return The list containing the information
     */
    public static List<String> askUserForArtistTitle() {
        return askUserForArtistTitle(null);
    }

    /**
     * Asks the user for the artist/title combination. By convention, the
     * zeroith element is the artist, and the first is the title. If the user
     * aborts the operations, Unknown Artist/Unknown Title is returned.
     *
     * @param fileName The file name to list on the dialog
     * @return The list containing the information
     */
    public static List<String> askUserForArtistTitle(String fileName) {
        Platform.runLater(() -> {
            try {
                TextInputDialog dialog = new TextInputDialog("");
                dialog.setTitle("Stellar OPUS Converter: Enter Artist/Track Title");
                dialog.setHeaderText((fileName != null ? ("(" + fileName + ")\n") : "")
                        + "Enter Author/Track Title Like So: Author, Title");
                dialog.getDialogPane().setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                ASK_USER_METADATA.put(dialog.showAndWait());
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        List<String> list = new ArrayList<>(2);
        try {
            Optional<String> response = ASK_USER_METADATA.take();
            if (response.isPresent()) {
                String[] elements = response.get().trim().split(",");
                if (elements.length != 2) {
                    Platform.runLater(() -> Notifications.create().hideAfter(Duration.seconds(10))
                            .text("Wrong format for Artist, Track Title. Seperate the values using a comma.")
                            .title("Processing Error").showError());
                    return askUserForArtistTitle(fileName);
                }
                list.addAll(Arrays.asList(elements));
            } else {
                System.out.println("Aborting Operation");
                System.exit(0);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Interrupted while waiting for user input!");
            list.addAll(StellarOPUSConverter.getDefaultMetadataList());
        }
        return list.stream().map(string -> string.trim()).collect(Collectors.toList());
    }

    /**
     * Gets the output folder location.
     *
     * @return The folder path
     */
    public static Optional<Path> getOutputFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Stellar OPUS Converter: Choose Output Directory for Converted Files");
        Platform.runLater(() -> {
            File file = chooser.showDialog(null);
            try {
                OUTPUT_FOLDER_QUEUE.put(file);
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        try {
            return Optional.of(OUTPUT_FOLDER_QUEUE.take().toPath());
        } catch (InterruptedException ex) {
            System.err.println("Interrupted during waiting for output file list!");
        }
        return Optional.empty();
    }

    /**
     * Selects a list of files for conversion.
     *
     * @return The files for conversion
     */
    public static Optional<List<Path>> getFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Stellar OPUS Converter: Choose Files to Convert to OPUS");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Formats", "*.*"));
        FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("All Video Files (.mp4, .mkv)", "*.mp4", "*.mkv");
        chooser.getExtensionFilters().add(videoFilter);
        chooser.setSelectedExtensionFilter(videoFilter);
        Platform.runLater(() -> {
            List<File> files = chooser.showOpenMultipleDialog(null);
            try {
                if (files == null) {
                    System.out.println("No Files Chosen, Aborting");
                    System.exit(0);
                }
                PATHS_QUEUE.put(files);
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        try {
            List<File> files = PATHS_QUEUE.take();
            if (files != null) {
                return Optional.of(files.stream().map(file -> file.toPath()).collect(Collectors.toList()));
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Interrupted during waiting for file list!");
        }
        return Optional.empty();
    }
}
