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
package com.protonmail.sarahszabo.stellar.conversions.converters;

import com.protonmail.sarahszabo.stellar.StellarDiskManager;
import com.protonmail.sarahszabo.stellar.metadata.ConverterMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An interface for standardizing the converter subclasses. Converters take a
 * file in their constructor and save it for use later. The conversion methods
 * convert this to the proper format. They also store and accept the destination
 * folder in their constructors.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public abstract class StellarConverter {

    /**
     * The input file on the filesystem from which we will get data.
     */
    protected final Path INPUT_FILE;
    /**
     * The destination file which we will create.
     */
    protected Path DESTINATION_FILE;
    /**
     * The name of the file.
     */
    protected final String FILE_NAME,
            /**
             * The name of the file without the file extension.
             */
            FILE_NAME_NO_EXTENSION,
            /**
             * The file extension (should it exist; else empty as a string.
             * INCLUDES THE DOT '.'.
             */
            FILE_EXTENSION;
    /**
     * The metadata of this file.
     */
    protected ConverterMetadata metadata;

    /**
     * Constructs a new {@link StellarConverter} with the specified inputFile on
     * the disk and the destination folder. By default, calls for EXIF metadata
     * on the input file if metadata is true.
     *
     * @param inputFile The file on the disk to convert, must exist
     * @param destinationFolder The folder to save the file in
     * @param fileExtension The file extension of the child converter's output
     * @param metadataSearch Whether or not we should set the metadata using
     * {@link StellarDiskManager#getMetadata(java.nio.file.Path)}
     */
    protected StellarConverter(Path inputFile, Path destinationFolder, String fileExtension, boolean metadataSearch) {
        if (Files.notExists(inputFile)) {
            throw new IllegalStateException("File: " + inputFile + " does not exist.");
        }
        try {
            Files.createDirectories(destinationFolder);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create directory: " + destinationFolder, ex);
        }
        this.INPUT_FILE = inputFile;
        //Get the file extension of this file and set it as both the extension and destination file path.
        this.FILE_NAME = this.INPUT_FILE.getFileName().toString();
        //Assuming file extension might not exist (Linux)
        if (!fileExtension.isEmpty()) {
            this.FILE_NAME_NO_EXTENSION = this.FILE_NAME.substring(0, this.FILE_NAME.lastIndexOf("."));
        } else {
            this.FILE_NAME_NO_EXTENSION = this.FILE_NAME;
        }
        this.FILE_EXTENSION = fileExtension;
        this.DESTINATION_FILE = destinationFolder.resolve(this.FILE_NAME_NO_EXTENSION + this.FILE_EXTENSION);

        if (metadataSearch) {
            //Get metadata from either filename or tags
            StellarDiskManager.getMetadata(this.INPUT_FILE);
        }
    }

    /**
     * A subroutine for checking ahead of time whether or not this object thinks
     * that the file is a candidate for conversion.
     *
     * @return The boolean
     */
    public boolean isConversionCandidate() {
        return true;
    }

    /**
     * Checks whether or not the current filename is malformatted relative to
     * the standard TITLE.EXTENSION format.
     *
     * @return The boolean
     */
    protected boolean isMalformattedFilename() {
        return this.FILE_NAME.matches(".*-[^-]*-.*");
    }

    /**
     * Converts the file with standard metadata options and stores it in the
     * destination folder.
     *
     * @return The path to the destination file
     * @throws IllegalStateException If the input file is not a candidate for
     * conversion
     */
    public abstract Path convert();

    /**
     * Converts the file with custom metadata options and stores it in the
     * destination folder.
     *
     * @return The path to the destination file
     * @throws IllegalStateException If the input file is not a candidate for
     * conversion
     */
    public abstract Path convert(ConverterMetadata metadata);

    /**
     * Gets the metadata of this file, assuming it has any. If not, returns
     * {@link ConverterMetadata#DEFAULT_METADATA}.
     *
     * @return The metadata
     */
    public ConverterMetadata getMetadata() {
        return this.metadata;
    }

    /**
     * Gets the file extension to be used for this type of converter.
     *
     * @return The extension as a string
     */
    public String getFileExtension() {
        return this.FILE_EXTENSION;
    }

    /**
     * Gets this variable.
     *
     * @return Get this variable
     */
    public Path getInputFile() {
        return this.INPUT_FILE;
    }

    /**
     * Gets this variable.
     *
     * @return Get this variable
     */
    public Path getDestinationFile() {
        return this.DESTINATION_FILE;
    }
}
