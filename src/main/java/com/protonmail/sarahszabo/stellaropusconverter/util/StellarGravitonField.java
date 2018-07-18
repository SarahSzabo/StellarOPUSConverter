/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util;

import com.protonmail.sarahszabo.stellaropusconverter.StellarDiskManager;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
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
     * /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param commands The commands to execute
     * @param inheritIO Merge the process streams?
     * @throws IOException InterruptedException If something went wrong
     */
    public static void processOP(List<String> commands, boolean inheritIO) throws IOException {
        processOP(inheritIO, commands.toArray(new String[4]));
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion. Does not inherit IO.
     *
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     */
    public static void processOP(String... commands) throws IOException {
        processOP(false, commands);
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
     */
    public static void processOP(boolean inheritIO, Path redirect, String... commands) throws IOException {
        //Print out FFMPEG Command
        String s = "";
        for (String st : processOPBuilder(true, redirect, commands).command()) {
            s += " " + st;
        }
        logger.info("COMMAND: " + s);
        //Actually do it
        Process proc = processOPBuilder(inheritIO, redirect, commands).start();
        try {
            proc.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarGravitonField.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     */
    public static void processOP(boolean inheritIO, String... commands) throws IOException {
        processOP(inheritIO, null, commands);
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
        ProcessBuilder builder;
        if (inheritIO) {
            builder = new ProcessBuilder(commands)
                    .directory(StellarDiskManager.getTempDirectory().toFile()).inheritIO();

        } else {
            builder = new ProcessBuilder(commands).directory(StellarDiskManager.getTempDirectory().toFile());
        }
        if (redirect != null) {
            builder = builder.redirectOutput(redirect.toFile());
        }
        return builder;
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
