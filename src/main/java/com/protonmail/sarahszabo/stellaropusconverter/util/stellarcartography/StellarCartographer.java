/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography;

import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.BufferedStellarCartographyModule;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.SingleModule;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.SingleStellarCartographyModule;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.StellarCartographyModule;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple logger implementation.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 * @param <C> The class associated with this Logger
 */
public abstract class StellarCartographer<C> {

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
     * Generates a new timestamp in our custom format.
     *
     * @return The timestamp
     */
    public static String generateTimestamp() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private final LogLevel level;
    private final List<StellarCartographyModule> modules;

    /**
     * Creates a new {@link StellarCartographer} with the specified minimum
     * logging level. Loggers may record events at the specified logging level
     * or higher.
     *
     * @param level The minimum logging level.
     */
    protected StellarCartographer(LogLevel level) {
        this(level, Stream.of(new SingleModule()).collect(Collectors.toList()));
    }

    /**
     * Creates a new {@link StellarCartographer} with the specified minimum
     * logging level. Loggers may record events at the specified logging level
     * or higher. The modules must be in the order data will be processed in.
     *
     * @param level The minimum logging level.
     * @param modules The modules to use in the order they're given
     */
    protected StellarCartographer(LogLevel level, List<StellarCartographyModule> modules) {
        this.level = level;
        this.modules = new ArrayList<>(Objects.requireNonNull(modules));
    }

    /**
     * Checks the level against our internal level.
     *
     * @param level The level specified
     * @throws IllegalStateException If our level > specified level
     */
    private void checkLevel(LogLevel level) {
        if (this.level.levelAura() > level.levelAura()) {
            IllegalStateException exception = new IllegalStateException("Logging Level is Lower Than Logger Level");
            logException("Check Level", exception);
            throw exception;
        }
    }

    /**
     * Logs at the level the logger is set at.
     *
     * @param log The log to enter
     */
    public void log(String log) {
        log(log, this.level);
    }

    /**
     * Enters a log into the log file with a timestamp at the specified level.
     * This is the primary exit point for logs, overwrite this to change the
     * exit point for logging. Any subclass implementation of this method should
     * call this subroutine to check the logging level. This method also enables
     * the modules ability for buffering and other functions.
     *
     * If modules such as a buffering module are used,
     * {@link StellarCartographer#moduleInput(com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.LogEntry)}
     * should be called as well.
     *
     * @param log The log to enter
     * @param level The level of this log
     */
    public void log(String log, LogLevel level) {
        checkLevel(level);
    }

    /**
     * Called when any internal buffers should be flushed. Implementation
     * dependant, should be overwritten by subclasses. Since we're delegating
     * out everything, exceptions thrown by subclasses should throw Runtime
     * exceptions with the thrown exception argument set.
     *
     * @throws RuntimeException If an overwritten flush method encountered an
     * exception
     */
    public void flush() {
        //Implementation Dependant Nothing for us to do!
    }

    /**
     * Called when input can be given to modules.
     *
     * @param entry The entry to input
     */
    protected void moduleInput(LogEntry entry) {
        ((StellarCartographyModule) this.modules.get(0)).input(entry);
    }

    /**
     * A recursive subroutine that gets the final entry by giving each module
     * the output from the last module as an input.
     *
     * @param elements The zeroth entry.
     * @param counter The counter for the modules list, should start at 1 (Not
     * 0)
     * @return The last entry after the I/O cascade
     */
    private List<LogEntry> cascadeModules(List<LogEntry> elements, int counter) {
        if (counter == this.modules.size() - 1) {
            return this.modules.get(counter).getOutput();
        } else {
            this.modules.get(counter).input(elements);
            return cascadeModules(this.modules.get(counter).getOutput(), counter++);
        }
    }

    /**
     * Gets the output from the modules.
     *
     * @return The output from the modules
     */
    protected Optional<LogEntry> moduleOutput() {
        List<LogEntry> entries = cascadeModules(this.modules.get(0).getOutput(), 1);
        return Optional.of(new LogEntry(entries.stream().map(entry -> entry.getLogEntry())
                .collect(Collectors.joining("\n\n")), StellarCartographer.generateTimestamp(), level));
    }

    /**
     * Enters an exception into the permanant log.
     *
     * @param location The code location where the exception occurred.
     * @param ex The exception
     */
    public void logException(String location, Exception ex) {
        log(getLogString("Exception Encountered <" + ex.getStackTrace()[1].getClassName() + ": "
                + location + ">: " + ex.getMessage()), LogLevel.EXCEPTION);
    }

    /**
     * Enters a log into the log file with the timestamp
     *
     * @param log The log to enter
     * @return The string form of the log entry
     */
    public String getLogString(String log) {
        return "\n" + StellarGravitonField.preferredTitleFormat(Thread.currentThread().getName()) + ": "
                + log + " || " + generateTimestamp();
    }
}
