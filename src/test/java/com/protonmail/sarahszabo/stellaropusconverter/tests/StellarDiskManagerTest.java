/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.tests;

import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class StellarDiskManagerTest {

    public StellarDiskManagerTest() {
    }

    @Test
    public void testExifprocess() throws IOException {
        //File Located at /home/sarah/Aero.opus
        Path path = StellarGravitonField.newPath("Test Files", "Aero.txt");
        ProcessBuilder builder = new ProcessBuilder("exiftool", "Aero.opus").directory(StellarGravitonField.newPath("", "Test Files").toFile())
                .inheritIO();
        builder.redirectOutput(path.toFile());
        Process process = builder.start();
        assertTrue(Files.exists(path));
        Files.deleteIfExists(path);
    }

    /**
     * Test of values method, of class StellarDiskManager.
     */
    @Test
    public void testValues() {
    }

    /**
     * Test of valueOf method, of class StellarDiskManager.
     */
    @Test
    public void testValueOf() {
    }

    /**
     * Test of getOPUSMetadata method, of class StellarDiskManager.
     */
    @Test
    public void testGetOPUSMetadata() throws Exception {
    }

    /**
     * Test of readHelpText method, of class StellarDiskManager.
     */
    @Test
    public void testReadHelpText() throws Exception {
    }

    /**
     * Test of setSpaceBridgeDirectory method, of class StellarDiskManager.
     */
    @Test
    public void testSetSpaceBridgeDirectory() {
    }

    /**
     * Test of setPictureOutputFolder method, of class StellarDiskManager.
     */
    @Test
    public void testSetPictureOutputFolder() {
    }

    /**
     * Test of setOutputFolder method, of class StellarDiskManager.
     */
    @Test
    public void testSetOutputFolder() throws Exception {
    }

    /**
     * Test of copyToTemp method, of class StellarDiskManager.
     */
    @Test
    public void testCopyToTemp() throws Exception {
    }

    /**
     * Test of copyFromTemp method, of class StellarDiskManager.
     */
    @Test
    public void testCopyFromTemp() throws Exception {
    }

    /**
     * Test of getPictureOutputFolder method, of class StellarDiskManager.
     */
    @Test
    public void testGetPictureOutputFolder() {
    }

    /**
     * Test of getOutputFolder method, of class StellarDiskManager.
     */
    @Test
    public void testGetOutputFolder() {
    }

    /**
     * Test of getTempDirectory method, of class StellarDiskManager.
     */
    @Test
    public void testGetTempDirectory() {
    }

    /**
     * Test of getState method, of class StellarDiskManager.
     */
    @Test
    public void testGetState() {
    }

}
