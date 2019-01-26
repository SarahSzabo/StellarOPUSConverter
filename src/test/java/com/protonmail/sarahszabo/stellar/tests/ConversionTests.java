/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar.tests;

import com.protonmail.sarahszabo.stellar.StellarDiskManager;
import com.protonmail.sarahszabo.stellar.StellarMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class ConversionTests {

    /**
     * The test folder
     */
    private static final Path TEST_FOLDER = Paths.get(StellarDiskManager.USER_DIR.toString(), "Testing Files");
    /**
     * The file to be converted in out examples.
     */
    private static final Path TEST_FILE = Paths.get(StellarDiskManager.CONFIGURATION_FOLDER.toString(),
            "Sarah Szabo -- The Fourtyth Divide.mp4");

    public ConversionTests() throws IOException {
        if (Files.notExists(TEST_FOLDER)) {
            Files.createDirectory(TEST_FOLDER);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FileUtils.deleteQuietly(TEST_FOLDER.toFile());
        }));
    }

    private void resetTestFolder() throws IOException {
        FileUtils.deleteQuietly(TEST_FOLDER.toFile());
        if (Files.notExists(TEST_FOLDER)) {
            Files.createDirectory(TEST_FOLDER);
        }
    }
}
