/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar.util;

import com.protonmail.sarahszabo.stellar.util.StellarGreatFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class StellarFileConverterTest {

    public StellarFileConverterTest() {
    }

    @Test
    public void testConvertBeginningNumbers() throws IOException {
        List<Path> paths = new ArrayList<>(2), newPaths;
        paths.add(Paths.get("TEST/00-NGE.mp3"));
        paths.add(Paths.get("TEST/00 - NGE.mp3"));
        paths.add(Paths.get("TEST/00 NGE.mp3"));
        newPaths = StellarGreatFilter.convertBeginningNumbers(paths);
        assertEquals(newPaths.get(0).getFileName().toString(), "NGE.mp3");
        assertEquals(newPaths.get(1).getFileName().toString(), "NGE.mp3");
        assertEquals(newPaths.get(2).getFileName().toString(), "NGE.mp3");
    }

}
