/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.FileChooser;

/**
 * The UI for the Stellar OPUS Converter.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class StellarUI {

    private static final BlockingQueue<List<File>> PATHS_QUEUE = new ArrayBlockingQueue<>(1);
    private static final BlockingQueue<File> OUTPUT_FOLDER_QUEUE = new ArrayBlockingQueue<>(1);

    static {//Initialize Toolkit
        Platform.runLater(() -> {
            JFXPanel panel = new JFXPanel();
        });
    }

    /**
     * Gets the output folder location.
     *
     * @return The folder path
     */
    public static Optional<Path> getOutputFolder() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Stellar OPUS Converter: Choose Output Directory for Converted Files");
        Platform.runLater(() -> {
            File file = chooser.showOpenDialog(null);
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
    public static Optional<List<Path>> getFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Stellar OPUS Converter: Choose Files to Convert to OPUS");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Formats", "*.*"));
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("All Video Files", "*.mp4", "*.mkv"));
        Platform.runLater(() -> {
            List<File> files = chooser.showOpenMultipleDialog(null);
            try {
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
