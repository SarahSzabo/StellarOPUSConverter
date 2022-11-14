/*
 * Copyright (C) 2022 Sarah Szabo <SarahSzabo@Protonmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.protonmail.sarahszabo.stellar.conversions.converters.standardformconverter;

import com.protonmail.sarahszabo.stellar.Main;
import static com.protonmail.sarahszabo.stellar.Main.logger;
import com.protonmail.sarahszabo.stellar.conversions.converters.StellarConverter;
import com.protonmail.sarahszabo.stellar.metadata.ConverterMetadata;
import static com.protonmail.sarahszabo.stellar.util.StellarGravitonField.processOP;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A converter for converting music files that are malformed to their proper
 * tagged versions in the standard TITLE.EXTENSION format with set Artist and
 * Title tags.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class StellarStandardFormConverter extends StellarConverter {

    /**
     * Whether or not we identified a duplicate in the destination and had to
     * rename.
     */
    private boolean hadDuplicate = false;
    /**
     * The ultimate filename for the file
     */
    private String finalFileName;

    /**
     * Constructs a new {@link StellarStandardFormConverter} with the specified
     * inputFile on the disk and the destination folder.
     *
     * @param inputFile The file on the disk to convert, must exist
     * @param destinationFolder The folder to save the file in
     */
    public StellarStandardFormConverter(Path inputFile, Path destinationFolder) {
        super(inputFile, destinationFolder, inputFile.getFileName().toString()
                .substring(inputFile.getFileName().toString().lastIndexOf('.')), true);
    }

    @Override
    public boolean isConversionCandidate() {
        return this.FILE_NAME.matches("[^-]*-[^-]*");
    }

    /**
     * Converts the AUTHOR - TITLE.EXTENSION track to the proper form ->
     * TITLE.EXTENSION.
     *
     * @return The path to the newly converted file (will rename if discovers
     * duplicates)
     */
    @Override
    public Path convert() {
        if (!isConversionCandidate()) {
            throw new IllegalStateException("File: " + this.INPUT_FILE + " is not a candidate for"
                    + " standard-form conversion to TITLE.EXTENSION.");
        }

        System.out.println("Indexing File: " + this.INPUT_FILE);
        this.metadata = generateArtistTitle();
        this.finalFileName = (this.metadata.getTitle() + super.FILE_EXTENSION).trim();
        //Generates and executes the conversion
        convertHandleConversionString();

        ///Finish up filename, remove space character which appears as first character sometimes
        if (this.finalFileName.charAt(0) == ' ') {
            this.finalFileName = this.finalFileName.substring(1);
        }
        System.out.println("Indexing Output: " + this.finalFileName);
        this.DESTINATION_FILE = this.DESTINATION_FILE.resolve(this.finalFileName);
        //Handler for the file move and renaming operations
        convertHandleFileMoveOperations();
        return this.DESTINATION_FILE;
    }

    /**
     * Handler for the file move and renaming operations.
     */
    private void convertHandleFileMoveOperations() {
        //Rename on Disk
        if (Files.exists(this.DESTINATION_FILE)) {
            this.hadDuplicate = true;
            //File Already Exists, rename
            this.finalFileName = super.FILE_NAME_NO_EXTENSION + " (" + this.metadata.getArtist() + ")" + super.FILE_EXTENSION;
            super.DESTINATION_FILE = super.DESTINATION_FILE.getParent().resolve(this.finalFileName);
            logger.info("Duplicate Observed & Corrected: " + this.finalFileName);
        }
        try {
            Files.move(this.INPUT_FILE, this.DESTINATION_FILE);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Something went wrong with renaming: " + this.INPUT_FILE, ex);
        }
    }

    /**
     * Handles convert's conversion string step.
     */
    private void convertHandleConversionString() {
        try {
            //Set Tags: kid3-cli -c "tag 1" -c "set Title "TITLE"" -c "set Artist "ARTIST"" FILENAME.EXT
            /*processOP("kid3-cli", "-c", "tag 2", "-c", "set Title \'" + this.metadata.getTitle() + "\'",
                    "-c", "set Artist \'" + this.metadata.getArtist() + "\'", this.INPUT_FILE.toString());*/
            processOP(new KID3CommandBuilder(this.INPUT_FILE).selectTag(2).setTitle(this.metadata.getTitle())
                    .setArtist(this.metadata.getArtist()).Tag2To2Point4().build());
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Could not set tags for: " + this.INPUT_FILE, ex);
        }
    }

    @Override
    public Path convert(ConverterMetadata metadata) {
        throw new UnsupportedOperationException("This class looks for mismatched file names and tags and corrects those errors,"
                + " it does not use metadata to construct a file.");
    }

    /**
     * Generates a {@link ConverterMetadata} from the artist title combination.
     *
     * @return The metadata
     */
    private ConverterMetadata generateArtistTitle() {
        //Find filename and artist for tags
        var splitByHyphen = this.FILE_NAME.split("-");

        //Extract Tags
        String artist = splitByHyphen[0].trim(), title = splitByHyphen[1].substring(0, splitByHyphen[1].lastIndexOf("."))
                .trim();
        System.out.println("Auto-Detected: Artist = " + artist + ", Title = " + title);
        return new ConverterMetadata(artist, title, "Unknown", LocalDate.MIN, null, 0);
    }

    /**
     * A boolean for whether or not this conversion operation has a duplicate
     * file or not.
     *
     * @return The boolean value
     */
    public boolean hadDuplicate() {
        return this.hadDuplicate;
    }
}
