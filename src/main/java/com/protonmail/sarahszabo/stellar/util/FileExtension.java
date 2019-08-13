/*
 * Copyright (C) 2019 Sarah Szabo <SarahSzabo@Protonmail.com>
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
package com.protonmail.sarahszabo.stellar.util;

import com.protonmail.sarahszabo.stellar.metadata.MetadataType;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * An enum representing a file extension.
 */
public enum FileExtension {

    FLV {
        @Override
        public String toString() {
            return ".flv";
        }
    }, M4A {
        @Override
        public String toString() {
            return ".m4a";
        }
    }, FLAC {
        @Override
        public String toString() {
            return ".flac";
        }
    }, OGG {
        @Override
        public String toString() {
            return ".ogg";
        }
    },
    WEBM {
        @Override
        public String toString() {
            return ".webm";
        }
    }, /**
     * Already Existing OPUS Files. The bitrate option does not apply here since
     * we are copying the bitrate over.
     */
    OPUS {
        @Override
        public List<String> getGeneralConversionString(String inputFileName, String outputFileName, Map<MetadataType, String> metadata, int bitrate) {
            return new ProcessBuilder("ffmpeg", inputFileName, "-c", "copy", "-y", "-metadata", "title=" + metadata.get(MetadataType.TITLE), "-metadata", "artist=" + metadata.get(MetadataType.ARTIST), "-strict", "-2", StellarGravitonField.preferredTitleFormat(outputFileName + ".opus")).command();
        }

        @Override
        public String toString() {
            return ".opus";
        }
    }, /**
     * MP4 Files.
     */
    MP4 {
        @Override
        public String toString() {
            return ".mp4";
        }
    }, /**
     * MP3 Files.
     */
    MP3 {
        @Override
        public String toString() {
            return ".mp3";
        }
    };

    /**
     * Compares the current extension against the list of supported video files.
     *
     * @param ext The file extension to compare
     * @return Whether or not the sent extension is a video or not.
     */
    public static boolean isVideo(FileExtension ext) {
        return ext == MP4 || ext == FLV;
    }

    /**
     * Strips the file extension off newPath the fileName and returns it.
     *
     * @param path The path to have stripped
     * @return The stripped file name
     */
    public static String stripFileExtension(Path path) {
        return path.getFileName().toString().replaceFirst("[.][^.]+$", "");
    }

    /**
     * Gets the low bitrate conversion (120K) and saves it to
     * {@link StellarDiskManager#getOutputFolder()}.
     *
     * @param inputFile The path newPath the input file
     * @return The list newPath commands needing execution
     */
    public List<String> getLowBitrateConversion(Path inputFile) {
        return new ProcessBuilder("ffmpeg", "-i", inputFile.getFileName().toString(), "-b:a", "190k", "-strict", "-2", StellarGravitonField.preferredTitleFormat(inputFile.getFileName().toString().replace(toString(), "") + "(((Copy))).opus")).command();
    }

    /**
     * Gets the conversion string for
     * {@link StellarOPUSConverter#processOP(java.lang.String...)}, give it the
     * input file name, and the output file name, and it will give you the
     * string list necessary to do the operation.
     *
     * @param inputFileName The filename newPath the input file, should contain
     * the file extension
     * @param outputFileName The filename newPath the output file, should NOT
     * contain the file extension
     * @return The process necessary to convert this filetype to OPUS.
     */
    public List<String> getGeneralConversionString(String inputFileName, String outputFileName, Map<MetadataType, String> metadata, int bitrate) {
        return new ProcessBuilder("ffmpeg", "-i", inputFileName, "-y", "-b:a", bitrate + "k", "-metadata", "title=" + metadata.get(MetadataType.TITLE), "-metadata", "artist=" + metadata.get(MetadataType.ARTIST), "-strict", "-2", StellarGravitonField.preferredTitleFormat(outputFileName + ".opus")).command();
    }

    public abstract String toString();

}
