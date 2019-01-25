/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
                return new FileChooser.ExtensionFilter("Picture Files", "*.jpeg", "*.png", "*.jpg");
            }
        };

        /**
         * Gets the file filter associated with this tag.
         *
         * @return
         */
        public abstract FileChooser.ExtensionFilter getFilter();
    }

    private static final BlockingQueue<List<Path>> PATH_LIST_QUEUE = new ArrayBlockingQueue<>(50);
    private static final BlockingQueue<Optional<Path>> PATH_QUEUE = new ArrayBlockingQueue<>(50);
    private static final BlockingQueue<Optional<String>> ASK_USER_METADATA = new ArrayBlockingQueue<>(50);

    static {
        //Initialize Toolkit
        new JFXPanel();
        Platform.setImplicitExit(false);
    }

    /**
     * Shows a confirmation dialog with the following text.
     *
     * @param text The content area text
     * @return Whether or not the user agrees to continue the operation
     */
    public static boolean showConfirmationDialog(String text) {
        try {
            var queue = new ArrayBlockingQueue<Optional<ButtonType>>(1);
            Platform.runLater(() -> {
                try {
                    var alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setContentText(text);
                    var stage = (Stage) alert.getDialogPane().getScene().getWindow();
                    var image = new Image(Files.newInputStream(StellarDiskManager.SYSTEM_ICON), 300, 500, true, true);
                    stage.getIcons().add(image);
                    stage.setResizable(true);
                    alert.setGraphic(new ImageView(image));
                    var response = alert.showAndWait();
                    queue.put(response);
                } catch (InterruptedException ex) {
                    Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                } catch (IOException ex) {
                    Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            var response = queue.take().orElse(ButtonType.CANCEL);
            return response == ButtonType.YES || response == ButtonType.OK;
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Sets the icon for the dialog.
     *
     * @param <T> The type of the dialog
     * @param dialog The dialog to set
     * @throws IOException If something happened
     */
    private static <T> void setDialogIcon(Dialog<T> dialog) throws IOException {
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Files.newInputStream(StellarDiskManager.SYSTEM_ICON)));
        dialog.getDialogPane().setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        dialog.setResizable(true);
    }

    /**
     * Performs the icon set operation with the specified supplier to set the
     * owner given the stage.
     *
     * @param function The set icon function
     * @return The same return type as the function
     */
    private static <T> T stageOP(Function<Stage, T> function) {
        Stage stage = getOwner();
        stage.show();
        T t = function.apply(stage);
        stage.close();
        return t;
    }

    /**
     * Gets the iconized stage owner. Should be run from the platform thread.
     *
     * @return The iconized stage
     */
    private static Stage getOwner() {
        try {
            Stage stage = new Stage();
            stage.getIcons().add(new Image(Files.newInputStream(StellarDiskManager.SYSTEM_ICON)));
            stage.show();
            return stage;
        } catch (IOException ex) {
            Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Shuts down the UI.
     */
    public static void shutdownUI() {
        Platform.exit();
    }

    /**
     * Selects a path from a list of path files.
     *
     * @param paths The paths to select from
     * @return The selected path
     */
    public static Optional<Path> selectPath(Collection<Path> paths) {
        synchronized (PATH_QUEUE) {
            Platform.runLater(() -> {
                try {
                    ChoiceDialog<Path> dialog = new ChoiceDialog<>(null, paths);
                    dialog.setContentText("Choose The Path of the Video to Rip the Album Art From: ");
                    dialog.setTitle("Stellar: Picture-Select Path Choice");
                    setDialogIcon(dialog);
                    PATH_QUEUE.put(dialog.showAndWait());
                } catch (InterruptedException | IOException ex) {
                    Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            try {
                return PATH_QUEUE.take();
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException("Interrupted During Waiting for Path Queue");
            }
        }
    }

    /**
     * Gets the files from the clipboard.
     *
     * @return The files or null if none
     */
    public static Optional<List<Path>> getFilesFromClipboard() {
        try {
            synchronized (PATH_LIST_QUEUE) {
                Platform.runLater(() -> {
                    try {
                        PATH_LIST_QUEUE.put(Clipboard.getSystemClipboard().getFiles().stream().map(file -> file.toPath())
                                .collect(Collectors.toList()));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                return Optional.of(PATH_LIST_QUEUE.take());
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.empty();
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
     * @param header The header text to list on the dialog
     * @param contentArea The text to put in the context area of the prompt
     * @return The list containing the information
     */
    public static Map<StellarOPUSConverter.MetadataType, String> askUserForArtistTitle(String header, String contentArea) {
        String newHeader = StellarGravitonField.preferredTitleFormat(header),
                newContentArea = StellarGravitonField.preferredTitleFormat(contentArea);
        synchronized (ASK_USER_METADATA) {
            Platform.runLater(() -> {
                try {
                    TextInputDialog dialog = new TextInputDialog(" , " + newContentArea);
                    dialog.setTitle("Stellar: Enter Artist/Track Title");
                    dialog.setHeaderText((header != null ? ("(" + newHeader + ")\n") : "")
                            + "Enter Author/Track Title Like So: Author, Title");
                    //Set Icon
                    setDialogIcon(dialog);
                    ASK_USER_METADATA.put(dialog.showAndWait());
                } catch (InterruptedException | IOException ex) {
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
                        return askUserForArtistTitle(header, contentArea);
                    }
                    list.addAll(Arrays.asList(elements));
                } else {
                    System.out.println("Aborting Operation");
                    System.exit(0);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Interrupted while waiting for user input!");
                ConverterMetadataBuilder builder = new ConverterMetadataBuilder();
                list.add(builder.getArtist());
                list.add(builder.getTitle());
            }
            //Format Nicely
            List<String> newList = list.stream().map(string -> string.trim()).collect(Collectors.toList());
            //Convert to Map
            Map<StellarOPUSConverter.MetadataType, String> map = new HashMap<>(2);
            map.put(StellarOPUSConverter.MetadataType.ARTIST, newList.get(0));
            map.put(StellarOPUSConverter.MetadataType.TITLE, newList.get(1));
            return map;
        }
    }

    /**
     * Asks the user for the artist/title combination. By convention, the
     * zeroith element is the artist, and the first is the title. If the user
     * aborts the operations, Unknown Artist/Unknown Title is returned.
     *
     * @param header The header text to list on the dialog
     * @return The list containing the information
     */
    public static Map<StellarOPUSConverter.MetadataType, String> askUserForArtistTitle(String header) {
        return askUserForArtistTitle(header, header);
    }

    /**
     * Gets a file from the disk.
     *
     * @param label The label to use for the chooser
     * @param currentlySelected The currently selected filter
     * @return The path to the file on the disk
     */
    public static Optional<Path> getFile(String label, EXTENSION_FILTER currentlySelected) {
        synchronized (PATH_QUEUE) {
            Platform.runLater(() -> {
                FileChooser chooser = new FileChooser();
                //Add all extension filters
                for (EXTENSION_FILTER filter : EXTENSION_FILTER.values()) {
                    FileChooser.ExtensionFilter extensionFilter = filter.getFilter();
                    chooser.getExtensionFilters().add(extensionFilter);
                }
                chooser.setTitle("Stellar: " + label);
                chooser.setSelectedExtensionFilter(currentlySelected.getFilter());
                File file = stageOP(stage -> chooser.showOpenDialog(stage));
                try {
                    PATH_QUEUE.put(file == null ? Optional.empty() : Optional.of(file.toPath()));
                } catch (InterruptedException ex) {
                    Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            try {
                return PATH_QUEUE.take();
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
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
        synchronized (PATH_QUEUE) {
            Platform.runLater(() -> {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Stellar: Choose Directory for " + type);
                File file = stageOP(stage -> chooser.showDialog(stage));
                try {
                    PATH_QUEUE.put(Optional.of(file.toPath()));
                } catch (InterruptedException ex) {
                    Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            try {
                return PATH_QUEUE.take();
            } catch (InterruptedException ex) {
                System.err.println("Interrupted during waiting for output file list!");
            }
            return Optional.empty();
        }
    }

    /**
     * Selects a list of files for conversion.
     *
     * @return The files for conversion
     */
    public static Optional<List<Path>> getFiles() {
        synchronized (PATH_LIST_QUEUE) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Stellar: Choose Files to Convert to OPUS");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Formats", "*.*"));
            FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("All Video Files (.mp4, .mkv)", "*.mp4", "*.mkv");
            chooser.getExtensionFilters().add(videoFilter);
            chooser.setSelectedExtensionFilter(videoFilter);
            Platform.runLater(() -> {
                List<File> files = stageOP(stage -> chooser.showOpenMultipleDialog(getOwner()));
                try {
                    if (files == null) {
                        System.out.println("No Files Chosen, Aborting");
                        System.exit(0);
                    }
                    PATH_LIST_QUEUE.put(files.stream().map(file -> file.toPath()).collect(Collectors.toList()));
                } catch (InterruptedException ex) {
                    Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            try {
                List<Path> files = PATH_LIST_QUEUE.take();
                if (files != null) {
                    return Optional.of(files);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(StellarUI.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Interrupted during waiting for file list!");
            }
            return Optional.empty();
        }
    }
}
