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
