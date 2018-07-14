/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Our custom formatter for logging. Produces a pleasing date-time format.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class StellarLoggingFormatter extends Formatter {

    /**
     * Creates a new logger for the specified class in the preferred logger
     * format.
     *
     * @param clazz The class of the caller
     * @return The modified logger in the proper format
     */
    public static Logger forClass(Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        StellarGravitonField.toTypicalLoggerFormat(logger, new ConsoleHandler());
        return logger;
    }

    /**
     * Constructs a new formatter.
     */
    public StellarLoggingFormatter() {
    }

    @Override
    public String format(LogRecord record) {
        return record.getLevel() + ": " + record.getMessage() + " -- " + StellarGravitonField.generateTimestamp();
    }

}
