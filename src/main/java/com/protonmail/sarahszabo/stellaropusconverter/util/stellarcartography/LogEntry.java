/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A class representing a log entry.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class LogEntry implements Comparable<LogEntry> {

    @JsonIgnore
    private final int ID;
    private final String logEntry, timestamp;
    private final LogLevel level;

    /**
     * Constructs a new {@link LogEntry} with the specified arguments.
     *
     * @param ID The ID of the owner
     * @param timestamp The timestamp from the formatter in
     * {@link StellarCartographer}
     * @param logEntry The entry to log
     * @param level The level of this entry
     */
    public LogEntry(int ID, String logEntry, String timestamp, LogLevel level) {
        this.ID = ID;
        this.logEntry = logEntry;
        this.timestamp = timestamp;
        this.level = level;
    }

    /**
     * Constructs a new {@link LogEntry} with the specified arguments.
     *
     * @param timestamp The timestamp from the formatter in
     * {@link StellarCartographer}
     * @param logEntry The entry to log
     * @param level The level of this entry
     */
    @JsonCreator
    public LogEntry(String logEntry, String timestamp, LogLevel level) {
        this.logEntry = logEntry;
        this.timestamp = timestamp;
        this.level = level;
        this.ID = Integer.MIN_VALUE;
    }

    /**
     * Gets the log entry.
     *
     * @return The log entry
     */
    public String getLogEntry() {
        return this.logEntry;
    }

    /**
     * Gets the timestamp formatted using {@link StellarCartographer}'s
     * formatter.
     *
     * @return The timestamp
     */
    public String getTimestamp() {
        return this.timestamp;
    }

    /**
     * Gets the level of this log event.
     *
     * @return The log level
     */
    public LogLevel getLevel() {
        return this.level;
    }

    @Override
    public String toString() {
        return getLogEntry();
    }

    /**
     * Gets the ID of the person ho created this entry.
     *
     * @return The ID of the owner
     */
    public int getID() {
        return this.ID;
    }

    @Override
    public int compareTo(LogEntry other) {
        return Integer.compare(this.ID, other.getID());
    }
}
