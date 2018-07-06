/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.BufferingModule;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.SingleModule;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.SingleStellarCartographyModule;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules.StellarCartographyModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link StellarCartographer} that logs to the disk
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class TerrestrialCartographer<C> extends StellarCartographer<C> {

    /**
     * The configuration folder for the program.
     */
    public static final Path CONFIGURATION_FOLDER = Paths.get("Configuration");
    /**
     * The Logging folder for the program.
     */
    private static final Path LOG_FOLDER_PATH = Paths.get(CONFIGURATION_FOLDER.toString(), "Logging");

    private final Path logPath;
    private final ObjectMapper mapper;
    private LogLevel level;

    /**
     * Constructs a new {@link TerrestrialCartographer} with the specified
     * logging level. The name of the file is dereived from the thread name.
     *
     * @param level The logging level
     * @throws IOException If something happened
     */
    public TerrestrialCartographer(LogLevel level) throws IOException {
        this(Thread.currentThread().getName() + " Log.dat", level);
    }

    /**
     * Constructs a new {@link TerrestrialCartographer} with the specified
     * filename. If the file path does not exist, it will be created.
     *
     * @param fileName The name of just the file (We'll create the full path
     * @param level The logging level
     * @throws IOException If something happened
     */
    public TerrestrialCartographer(String fileName, LogLevel level) throws IOException {
        this(Paths.get(LOG_FOLDER_PATH.toString(), fileName), level, Stream.of(new SingleModule()).collect(Collectors.toList()));
        this.level = level;
    }

    /**
     * Constructs a new {@link TerrestrialCartographer} with the specified
     * filename. If the file path does not exist, it will be created.
     *
     * @param fileName The name of just the file (We'll create the full path
     * @param level The logging level
     * @param modules The list of modules to use with this cartographer (Must be
     * in the order you intend them to output in. So: Buffer -> Sort by
     * Something -> Color Text Violet)
     * @throws IOException If something happened
     */
    public TerrestrialCartographer(String fileName, LogLevel level, List<StellarCartographyModule> modules) throws IOException {
        this(Paths.get(LOG_FOLDER_PATH.toString(), fileName), level, modules);
    }

    /**
     * Constructs a new {@link TerrestrialCartographer} with the specified
     * filename. If the file path does not exist, it will be created.
     *
     * @param filePath The filename/file-path to use for the log
     * @param level The logging level
     * @param modules The list of modules to use with this cartographer (Must be
     * in the order you intend them to output in. So: Buffer -> Sort by
     * Something -> Color Text Violet)
     * @throws IOException If something happened
     */
    public TerrestrialCartographer(Path filePath, LogLevel level, List<StellarCartographyModule> modules) throws IOException {
        super(level, modules);
        this.logPath = Objects.requireNonNull(filePath);
        this.mapper = new ObjectMapper();
        if (Files.isDirectory(filePath)) {
            throw new IllegalArgumentException("Specified path is that of a directory!");
        }
        Files.createDirectories(this.logPath.getParent());
        if (Files.notExists(this.logPath)) {
            Files.createFile(this.logPath);
        }
    }

    /**
     * Constructs a new {@link TerrestrialCartographer} with the specified
     * filename. If the file path does not exist, it will be created. Uses
     * {@link SingleModule} for the module.
     *
     * @param filePath The filename/file-path to use for the log
     * @param level The logging level
     * @throws IOException If something happened
     */
    public TerrestrialCartographer(Path filePath, LogLevel level) throws IOException {
        this(filePath, level, Stream.of(new SingleModule()).collect(Collectors.toList()));
    }

    @Override
    public void flush() {
        super.flush();
        Optional<LogEntry> entry = moduleOutput();
        if (!entry.isPresent()) {
            return;
        }
        try {
            this.mapper.writeValue(this.logPath.toFile(), moduleOutput().get());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Implementation of {@link StellarCartographer}'s log method. Writes the
     * log to the disk.
     *
     * @param log The log to write
     * @param level The level to write it at
     * @throws RuntimeException if an IO event happened
     */
    @Override
    public void log(String log, LogLevel level) {
        //Check Level
        super.log(log, level);
        moduleInput(new LogEntry(log,
                LocalDateTime.now().format(StellarCartographer.FORMATTER), level));
    }

    @Override
    public String toString() {
        return "Terrestrial Cartographer";
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        flush();
    }

}
