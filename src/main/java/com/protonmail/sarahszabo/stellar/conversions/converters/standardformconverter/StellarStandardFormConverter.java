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
     * Determines whether or not a filename matches the currently supported
     * types or not.
     *
     * @return The boolean
     */
    public static boolean isFiletypeCandidate(String filename) {
        return filename.matches(".*\\.(mp3|opus|ogg|wav|aac|flac|m4a)");
    }

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
        //Ternery is a bugfix for Linux OS's that do not require file names to be present
        super(inputFile, destinationFolder, inputFile.getFileName().toString().contains(".")
                ? inputFile.getFileName().toString().substring(inputFile.getFileName().toString().lastIndexOf('.')) : "", true);
    }

    /**
     * Determines whether or not a filename matches the currently supported
     * types or not.
     *
     * @return The boolean
     */
    public boolean isFiletypeCandidate() {
        return isFiletypeCandidate(this.FILE_NAME);
    }

    @Override
    public boolean isConversionCandidate() {
        //Matches anything but "-" with a - and anything but "-" a . and then one or more text characters.
        return this.FILE_NAME.matches("[^-]*-[^-]*\\.mp3|opus|ogg|wav|aac");
    }

    /**
     * Handles the decision logic for the tags to set them into standard form.
     * Preserves if both tags exist, deletes tag1. If tag1 but not tag2 exist,
     * copies tag1->tag2 and deletes tag1. If tag2 exists, but not tag1, then
     * does nothing.
     *
     * @param tag1 The first tag
     * @param tag2 The second tag
     */
    private void convertHandleTagDecisionLogic(String tag1, String tag2, Path inputFile) {
        if (KID3CommandBuilder.isTagIDV3Valid(tag1) && KID3CommandBuilder.isTagIDV3Valid(tag2)) {
            var s = new KID3CommandBuilder(inputFile).deleteTag(1).buildAndExecuteString();
            System.out.println(s);
        } else if (KID3CommandBuilder.isTagIDV3Valid(tag1) && !KID3CommandBuilder.isTagIDV3Valid(tag2)) {
            var s = new KID3CommandBuilder(inputFile).copyTag(1, 2).deleteTag(1).buildAndExecuteString();
        }
    }

    /**
     * Converts the AUTHOR - TITLE.EXTENSION track to the proper form ->
     * TITLE.EXTENSION. Also checks miscellanious details about the file to
     * ensure that the tags are set properly for compliance with tagging
     * proceedures whether or not this routine converted the filename.
     *
     * @return The path to the newly converted file (will rename if discovers
     * duplicates)
     */
    @Override
    public Path convert() {
        //Use this temp variable just in case we get a conversion and the filename changes
        var inputFileInCaseOfConversion = this.INPUT_FILE;
        if (isConversionCandidate()) {
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
            this.DESTINATION_FILE = this.INPUT_FILE.getParent().resolve(this.finalFileName);
            //Handler for the file move and renaming operations
            convertHandleFileMoveOperations();
            inputFileInCaseOfConversion = this.DESTINATION_FILE;
        }
        if (isFiletypeCandidate()) {
            //Do miscellanious checks on IDV3 Tags This runs whether there was a conversion or not
            //Check That tag 2 is the primary tag and delete tag 1 if it exists, copying if tag2 does not exist, but 1 does
            var tag2 = new KID3CommandBuilder(inputFileInCaseOfConversion).selectTag(2).get().buildAndExecuteString();
            var tag1 = new KID3CommandBuilder(inputFileInCaseOfConversion).selectTag(1).get().buildAndExecuteString();
            convertHandleTagDecisionLogic(tag1, tag2, inputFileInCaseOfConversion);
        }
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
        return new ConverterMetadata(artist, title,
                ConverterMetadata.DEFAULT_METADATA.getCreatedBy(), ConverterMetadata.DEFAULT_METADATA.getStellarIndexDate(),
                ConverterMetadata.DEFAULT_METADATA.getAlbumArtPath(), ConverterMetadata.DEFAULT_METADATA.getBitrate());
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
