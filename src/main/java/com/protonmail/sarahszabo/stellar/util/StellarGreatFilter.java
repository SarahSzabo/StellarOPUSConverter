/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A filter class used for capturing odd filenames and converting them to a more
 * proper format.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public enum StellarGreatFilter {
    ;
        private static final Logger logger = StellarLoggingFormatter.forClass(StellarGreatFilter.class);

    /**
     * Filters the given files through filters to improve quality of the
     * filename at each step.
     *
     * @param paths The paths to modify
     * @return The list of original paths, minus the malformed ones, plus the
     * paths to the modified files
     * @throws java.io.IOException If something went wrong
     */
    public static List<Path> filterPaths(List<Path> paths) throws IOException {
        List<Path> newPaths = convertBeginningNumbers(paths);
        return newPaths;
    }

    /**
     * Converts filenames with numbers and hyphens in the beginning of the
     * filename. Renames the file to the typical format without the numbers.
     *
     * @param paths The paths to investigate for conversion
     * @return The list of all paths given, but with the malformed paths changed
     * @throws java.io.IOException If something went wrong
     */
    public static List<Path> convertBeginningNumbers(List<Path> paths) throws IOException {
        String overallPattern = "\\d+\\s*[-]*\\s*.+";
        logger.fine(paths.get(0).getFileName().toString().matches(overallPattern) + "");
        List<Path> newPaths = new ArrayList<>(paths.size());
        //Add all paths and remove the malformed ones later
        newPaths.addAll(paths);
        for (Path path : paths) {
            logger.fine("Original: " + path);
            if (!path.getFileName().toString().matches(overallPattern)) {
                continue;
            }
            //Remove malformed path from the list
            newPaths.remove(path);
            String string = path.getFileName().toString().replaceFirst("\\d+\\s*[-]*\\s*", "");
            logger.fine("String: " + string);
            logger.fine("Parent: " + path.getParent());
            Path newPath = StellarGravitonField.newPath(path.toAbsolutePath().getParent(), string);
            logger.fine("New Path " + newPath);
            logger.fine("New: " + newPath);
            newPaths.add(newPath);
            //Avoid Test Cases
            if (Files.notExists(path)) {
                continue;
            }
            logger.fine("New: " + newPath);
            logger.fine("Old: " + path);
            logger.fine("New Exists? " + Files.exists(newPath));
            logger.fine("Old Exists? " + Files.exists(path));
            //Move Original File to New Filename of Same Location
            Files.move(path, newPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            logger.fine("AFTER MOVE");
            logger.fine("New Exists? " + Files.exists(newPath));
            logger.fine("Old Exists? " + Files.exists(path));
        }
        return newPaths;
    }
}
