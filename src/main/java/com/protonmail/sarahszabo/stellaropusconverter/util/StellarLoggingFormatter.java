/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * Our custom formatter for logging. Produces a pleasing date-time format.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public final class StellarLoggingFormatter extends Formatter {

    /**
     * Creates a new logger for the specified title in the preferred logger
     * format. Writes the logs to the disk. Creates the directories if they
     * don't exist already.
     *
     * @param title The title the caller wants the logger to be
     * @param filePath The file path for the stream handler to write to the disk
     * @param level The logging level to use
     * @return The modified logger in the proper format
     * @throws java.io.IOException If something went wrong
     */
    public static Logger forTitle(String title, Path filePath, Level level) throws IOException {
        Logger logger = Logger.getLogger(title);
        logger.setLevel(level);
        Files.createDirectories(filePath.getParent());
        if (Files.notExists(filePath)) {
            Files.createFile(filePath);
        }
        Handler handler = new StreamHandler(Files.newOutputStream(filePath, StandardOpenOption.APPEND), new StellarLoggingFormatter());
        handler.setLevel(level);
        logger.addHandler(handler);
        return logger;
    }

    /**
     * Creates a new logger for the specified title in the preferred logger
     * format. Writes the logs to the disk. Creates the directories if they
     * don't exist already.
     *
     * @param title The title the caller wants the logger to be
     * @param filePath The file path for the stream handler to write to the disk
     * @return The modified logger in the proper format
     * @throws java.io.IOException If something went wrong
     */
    public static Logger forTitle(String title, Path filePath) throws IOException {
        return forTitle(title, filePath, Level.INFO);
    }

    /**
     * Creates a new logger for the specified class in the preferred logger
     * format.
     *
     * @param title The title the caller wants the logger to be
     * @param level The logging level to use
     * @return The modified logger in the proper format
     */
    public static Logger forTitle(String title, Level level) {
        Logger logger = Logger.getLogger(title);
        logger.setLevel(level);
        Handler handler = new ConsoleHandler();
        handler.setLevel(level);
        handler.setFormatter(new StellarLoggingFormatter());
        logger.addHandler(handler);
        return logger;
    }

    /**
     * Creates a new logger for the specified class in the preferred logger
     * format.
     *
     * @param title The title the caller wants the logger to be
     * @return The modified logger in the proper format
     */
    public static Logger forTitle(String title) {
        return forTitle(title, Level.INFO);
    }

    /**
     * Creates a new logger for the specified class in the preferred logger
     * format. Writes the logs to the specified file path.
     *
     * @param clazz The class of the caller
     * @param filePath The file path for the stream handler to write to the disk
     * @return The modified logger in the proper format
     * @throws java.io.IOException If something went wrong
     */
    public static Logger forClass(Class<?> clazz, Path filePath) throws IOException {
        return forTitle(clazz.getName(), filePath);
    }

    /**
     * Creates a new logger for the specified class in the preferred logger
     * format.
     *
     * @param clazz The class of the caller
     * @return The modified logger in the proper format
     */
    public static Logger forClass(Class<?> clazz) {
        return forTitle(clazz.getName());
    }

    /**
     * Constructs a new formatter.
     */
    public StellarLoggingFormatter() {
    }

    @Override
    public String format(LogRecord record) {
        return record.getLevel() + ": " + record.getMessage() + " -- " + StellarGravitonField.generateTimestamp() + "\n\n";
    }

}
