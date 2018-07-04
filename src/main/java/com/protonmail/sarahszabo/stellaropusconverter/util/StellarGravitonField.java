/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.function.Consumer;
import java.util.function.Function;
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

    /**
     * An subroutine representing an operation. In general what this is supposed
     * to do is perform the pre-operation, then perform the operation, and then
     * call the post operation. The return type if of the return type of the
     * operation.
     *
     * @param <O> The type of the function argument
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
     * A subroutine to test if the original string contains any of the capture
     * elements.
     *
     * @param original The string to search
     * @param capture The element which might be contained in original
     * @return If any of the sequences are contained in the string
     */
    public static boolean stringContains(String original, String... capture) {
        return Stream.of(capture).anyMatch(captureElement -> original.contains(captureElement));
    }

    /**
     * Enters a log into the log file with the timestamp
     *
     * @param log The log to enter
     * @return The string form of the log entry
     */
    public static String enterLogString(String log) {
        return log + " || " + LocalDateTime.now();
    }

    /**
     * Enters a log into the log file with the timestamp
     *
     * @param log The log to enter
     */
    public static void enterLog(String log) {
        System.out.println("\n" + preferredTitleFormat(Thread.currentThread().getName()) + ": "
                + log + " || " + LocalDateTime.now().format(FORMATTER));
    }

    /**
     * Enters an exception into the permanant log.
     *
     * @param location The code location where the exception occurred.
     * @param ex The exception
     */
    public static void enterExceptionIntoLog(String location, Exception ex) {
        enterLog("Exception Encountered <" + location + ">: " + ex.getMessage());
    }

    /**
     * Turns the given field into the proper format, which is the beginning of
     * each character is capitalised.
     *
     * @param field The field to capitalise
     * @return The formatted string
     */
    public static String preferredTitleFormat(String field) {
        //Uppercase First Character of Both Titles & Trim. Handles unusual edge cases such as F I L E.opus
        return Stream.of(field.trim().split(" ")).map(string -> string.isEmpty() ? " " : (string.charAt(0) + "").toUpperCase()
                + (string.length() > 1 ? string.substring(1) : "")).collect(Collectors.joining(" "));
    }
}
