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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public enum StellarUI {
    ;
    public static enum EXTENSION_FILTER {
        ALL {
            @Override
            public FileChooser.ExtensionFilter getFilter() {
                return new FileChooser.ExtensionFilter("ALL Files", "*");
            }
        }, OPUS_FILES {
            @Override
            public FileChooser.ExtensionFilter getFilter() {
                return new FileChooser.ExtensionFilter("OPUS Files", "*.opus");
            }
        }, PICTURE_FILES {
            @Override
            public FileChooser.ExtensionFilter getFilter() {
                return new FileChooser.ExtensionFilter("Picture Files", "*.jpeg", "*.png");
            }
        };

        /**
         * Gets the file filter associated with this tag.
         *
         * @return
         */
        public abstract FileChooser.ExtensionFilter getFilter();
    }

    private static final BlockingQueue<List<File>> FILE_LIST_QUEUE = new ArrayBlockingQueue<>(1);
    private static final BlockingQueue<File> FILE_QUEUE = new ArrayBlockingQueue<>(1);
    private static final BlockingQueue<Optional<String>> ASK_USER_METADATA = new ArrayBlockingQueue<>(1);

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
    public static List<Path> getFilesFromClipboard() {
        try {
            BlockingQueue<List<File>> queue = new ArrayBlockingQueue<>(1);
            Platform.runLater(() -> {
                try {
                    queue.put(Clipboard.getSystemClipboard().getFiles());
                } catch (InterruptedException ex) {
                    Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            return queue.take().stream().map(file -> file.toPath()).collect(Collectors.toList());
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
    public static Map<StellarOPUSConverter.MetadataType, String> askUserForArtistTitle() {
        return askUserForArtistTitle("");
    }

    /**
     * Asks the user for the artist/title combination. By convention, the
     * zeroith element is the artist, and the first is the title. If the user
     * aborts the operations, Unknown Artist/Unknown Title is returned.
     *
     * @param fileName The file name to list on the dialog
     * @return The list containing the information
     */
    public static Map<StellarOPUSConverter.MetadataType, String> askUserForArtistTitle(String fileName) {
        Platform.runLater(() -> {
            try {
                TextInputDialog dialog = new TextInputDialog(" , " + fileName);
                dialog.setTitle("Stellar: Enter Artist/Track Title");
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
            Map<StellarOPUSConverter.MetadataType, String> map = StellarOPUSConverter.getDefaultMetadata();
            list.add(map.get(StellarOPUSConverter.MetadataType.ARTIST));
            list.add(map.get(StellarOPUSConverter.MetadataType.TITLE));
        }
        //Format Nicely
        List<String> newList = list.stream().map(string -> string.trim()).collect(Collectors.toList());
        //Convert to Map
        Map<StellarOPUSConverter.MetadataType, String> map = new HashMap<>(2);
        map.put(StellarOPUSConverter.MetadataType.ARTIST, newList.get(0));
        map.put(StellarOPUSConverter.MetadataType.TITLE, newList.get(1));
        return map;
    }

    /**
     * Gets a file from the disk.
     *
     * @param label The label to use for the chooser
     * @param currentlySelected The currently selected filter
     * @return The path to the file on the disk
     */
    public static Optional<Path> getFile(String label, EXTENSION_FILTER currentlySelected) {
        Platform.runLater(() -> {
            FileChooser chooser = new FileChooser();
            //Add all extension filters
            for (EXTENSION_FILTER filter : EXTENSION_FILTER.values()) {
                FileChooser.ExtensionFilter extensionFilter = filter.getFilter();
                chooser.getExtensionFilters().add(extensionFilter);
            }
            chooser.setTitle("Stellar: " + label);
            chooser.setSelectedExtensionFilter(currentlySelected.getFilter());
            File file = chooser.showOpenDialog(null);
            try {
                FILE_QUEUE.put(file);
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        try {
            File file = FILE_QUEUE.take();
            return Optional.of(file.toPath().toAbsolutePath());
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Gets the output folder location.
     *
     * @param type The String type of object to set as the label Ex: Converted
     * Files
     * @return The folder path
     */
    public static Optional<Path> getFolderFor(String type) {
        Platform.runLater(() -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Stellar: Choose Directory for " + type);
            File file = chooser.showDialog(null);
            try {
                FILE_QUEUE.put(file);
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        try {
            return Optional.of(FILE_QUEUE.take().toPath());
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
        chooser.setTitle("Stellar: Choose Files to Convert to OPUS");
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
                FILE_LIST_QUEUE.put(files);
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        try {
            List<File> files = FILE_LIST_QUEUE.take();
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
