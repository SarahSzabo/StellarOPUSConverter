/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar.util;

import com.protonmail.sarahszabo.stellar.StellarDiskManager;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A helper class which assists in various activities.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class StellarGravitonField {

    private static final String MESSAGE_FIELD_SEPERATOR = "---------------------------------------------------------------------------------------";

    /**
     * The primary date-time formatter.
     */
    //Format: Day/Month/Year--Hour:Minute:Second.Millisecond
    public static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH).appendLiteral("/").appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral("/")
            .appendValue(ChronoField.YEAR).appendLiteral(" -- ").appendValue(ChronoField.HOUR_OF_DAY).appendLiteral(":")
            .appendValue(ChronoField.MINUTE_OF_HOUR).appendLiteral(":").appendValue(ChronoField.SECOND_OF_MINUTE)
            .appendLiteral(".").appendValue(ChronoField.MILLI_OF_SECOND).toFormatter();

    private static final Logger logger = Logger.getLogger(StellarGravitonField.class.getName());

    /**
     * Prints out a large message with horizontal bars for seperators.
     *
     * @param message The message to print
     */
    public static final String hyperlightMessageString(Object message) {
        return ("\n\n" + MESSAGE_FIELD_SEPERATOR + "\n" + message
                + "\n" + MESSAGE_FIELD_SEPERATOR + "\n\n");
    }

    /**
     * Prints out a large message with horizontal bars for seperators.
     *
     * @param message The message to print
     */
    public static final void hyperlightMessage(Object message) {
        System.out.println("\n\n" + MESSAGE_FIELD_SEPERATOR + "\n" + message
                + "\n" + MESSAGE_FIELD_SEPERATOR + "\n");
    }

    /**
     * Prints out a large message with horizontal bars for seperators.
     *
     * @param message The message to print
     */
    public static final void hyperlightMessage(String message) {
        System.out.println("\n\n" + MESSAGE_FIELD_SEPERATOR + "\n" + message
                + "\n" + MESSAGE_FIELD_SEPERATOR + "\n");
    }

    /**
     * Strips the file extension off newPath the fileName and returns it.
     * Convenience subroutine for {@link FileExtension#stripFileExtension(java.nio.file.Path)
     * }
     *
     * @param path The path to have stripped
     * @return The stripped file name
     */
    public static String stripFileExtension(Path path) {
        return FileExtension.stripFileExtension(path);
    }

    /**
     * /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param commands The commands to execute
     * @param inheritIO Merge the process streams?
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(List<String> commands, boolean inheritIO) throws IOException {
        return processOP(inheritIO, commands.toArray(new String[4]));
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion. Does not inherit IO.
     *
     * @param directory The directory to be in
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(Path directory, String... commands) throws IOException {
        return processOP(false, null, StellarDiskManager.REINDEXING_FOLDER, commands);
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion. Does not inherit IO.
     *
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(String... commands) throws IOException {
        return processOP(false, commands);
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion. Does not inherit IO.
     *
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(List<String> commands) throws IOException {
        return processOP(commands.toArray(String[]::new));
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion. Does not inherit IO.
     *
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOPCurrentDIR(boolean inheritIO, String... commands) throws IOException {
        return processOP(true, null, Paths.get(System.getProperty("user.dir")), commands);
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param redirect The path to direct output from the process to, if null,
     * prints to terminal
     * @param directory The directory to be in
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(boolean inheritIO, Path redirect, Path directory, String... commands) throws IOException {
        ProcessBuilder builder = processOPBuilder(inheritIO, redirect, directory, commands);
        //Print out FFMPEG Command
        logger.info("COMMAND: " + builder.command().stream().collect(Collectors.joining(" ")));
        //Actually do it
        Process proc = builder.start();
        try {
            proc.waitFor(30, TimeUnit.SECONDS);
            if (proc.isAlive()) {
                proc.destroyForcibly();
                return false;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarGravitonField.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param redirect The path to direct output from the process to, if null,
     * prints to terminal
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(boolean inheritIO, Path redirect, String... commands) throws IOException {
        return processOP(inheritIO, redirect, StellarDiskManager.getTempDirectory(), commands);
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(boolean inheritIO, String... commands) throws IOException {
        return processOP(inheritIO, null, commands);
    }

    /**
     * Gets the process builder with the specified boolean indicating whether IO
     * should be inherited or not, and the commands to execute. This process
     * builder is localised at the temp directory.
     *
     * @param inheritIO If IO should be inherited
     * @param redirect The path to direct output from the process to, if null,
     * prints to terminal
     * @param directory The directory that the process builder should be in
     * @param commands The commands to execute
     * @return The process builder with these properties
     */
    public static ProcessBuilder processOPBuilder(boolean inheritIO, Path redirect, Path directory, String... commands) {
        ProcessBuilder builder = new ProcessBuilder(commands).directory(directory.toFile());
        if (inheritIO) {
            builder = builder.inheritIO();

        }
        if (redirect != null) {
            builder = builder.redirectOutput(redirect.toFile());
        }
        return builder;
    }

    /**
     * Gets the process builder with the specified boolean indicating whether IO
     * should be inherited or not, and the commands to execute. This process
     * builder is localised at the temp directory.
     *
     * @param inheritIO If IO should be inherited
     * @param redirect The path to direct output from the process to, if null,
     * prints to terminal
     * @param commands The commands to execute
     * @return The process builder with these properties
     */
    public static ProcessBuilder processOPBuilder(boolean inheritIO, Path redirect, String... commands) {
        return processOPBuilder(inheritIO, redirect, StellarDiskManager.getTempDirectory(), commands);
    }

    /**
     * Gets the process builder with the specified boolean indicating whether IO
     * should be inherited or not, and the commands to execute. This process
     * builder is localized at the temp directory.
     *
     * @param inheritIO If IO should be inherited
     * @param commands The commands to execute
     * @return The process builder with these properties
     */
    public static ProcessBuilder processOPBuilder(boolean inheritIO, String... commands) {
        return inheritIO ? new ProcessBuilder(commands)
                .directory(StellarDiskManager.getTempDirectory().toFile()).inheritIO()
                : new ProcessBuilder(commands).directory(StellarDiskManager.getTempDirectory().toFile());
    }

    /**
     * Helper method, prints the message then System.exit().
     *
     * @param message The message to print
     */
    public static <T> void messageThenExit(T message) {
        logger.info(message.toString());
        System.exit(0);
    }

    /**
     * Helper method, prints the message then System.exit().
     *
     * @param message The message to print
     */
    public static void messageThenExit(String message) {
        logger.info(message);
        System.exit(0);
    }

    /**
     * Sets the logger with a handler of your choosing with a
     * {@link StellarLoggingFormatter}.
     *
     * @param logger The logger to attach the formatter to
     * @param handler The handler for the formatter
     */
    public static void toTypicalLoggerFormat(Logger logger, Handler handler) {
        handler.setFormatter(new StellarLoggingFormatter());
        logger.addHandler(handler);
    }

    /**
     * Generates a new timestamp in our custom format which can be put as a
     * filename.
     *
     * @return The timestamp
     */
    public static String generateTimestampFileNameFriendly() {
        return LocalDateTime.now().format(FORMATTER).replaceAll("/", ".");
    }

    /**
     * Generates a new timestamp in our custom format.
     *
     * @return The timestamp
     */
    public static String generateTimestamp() {
        return LocalDateTime.now().format(FORMATTER);
    }

    /**
     * A subroutine to automate Path creation from existing paths. Essentially
     * the same as {@link Paths#get(java.lang.String, java.lang.String...)}.
     *
     * @param first The root section newPath the path
     * @param more The branch section newPath the path
     * @return The newly created path
     */
    public static Path newPath(String first, String... more) {
        return Paths.get(first, more);
    }

    /**
     * A subroutine to automate Path creation from existing paths. Essentially
     * the same as {@link Paths#get(java.lang.String, java.lang.String...)}, but
     * with paths, calls {@link Object#toString()} on both path arguments.
     *
     * @param first The root section newPath the path
     * @param more The branch section newPath the path
     * @return The newly created path
     */
    public static Path newPath(Path first, String... more) {
        return Paths.get(first.toString(), more);
    }

    /**
     * A subroutine to automate Path creation from existing paths. Essentially
     * the same as {@link Paths#get(java.lang.String, java.lang.String...)}, but
     * with paths, calls {@link Object#toString()} on both path arguments.
     *
     * @param first The root section newPath the path
     * @param more The branch section newPath the path
     * @return The newly created path
     */
    public static Path newPath(Path first, Path more) {
        return Paths.get(first.toString(), more.toString());
    }

    /**
     * An subroutine representing an operation. In general what this is supposed
     * to do is perform the pre-operation, then perform the operation, and then
     * call the post operation. The return type if newPath the return type
     * newPath the operation.
     *
     * @param <O> The type newPath the function argument
     * @param <R> The return type
     * @param pre The subroutine containing the code to execute right before the
     * operation call
     * @param operation The operation function to use between pre & post calls
     * @param functionArgument The argument to give operation
     * @param post The subroutine containing the code to execute right after the
     * operation call
     * @return
     */
    public static <O, R> R OP(OPSubroutine pre, Function<O, R> operation, O functionArgument, OPSubroutine post) {
        pre.init();
        R r = operation.apply(functionArgument);
        post.init();
        return r;
    }

    /**
     * A subroutine to test if the original string contains any newPath the
     * capture elements.
     *
     * @param original The string to search
     * @param capture The element which might be contained in original
     * @return If any newPath the sequences are contained in the string
     */
    public static boolean stringContains(String original, String... capture) {
        return Stream.of(capture).anyMatch(captureElement -> original.contains(captureElement));
    }

    /**
     * Turns the given field into the proper format, which is the beginning
     * newPath each character is capitalised.
     *
     * @param field The field to capitalise
     * @return The formatted string
     */
    public static String preferredTitleFormat(String field) {
        //Uppercase First Character newPath Both Titles & Trim. Handles unusual edge cases such as F I L E.opus
        return Stream.of(field.trim().split(" ")).map(string -> string.isEmpty() ? " " : (string.charAt(0) + "").toUpperCase()
                + (string.length() > 1 ? string.substring(1) : "")).collect(Collectors.joining(" "));
    }
}
