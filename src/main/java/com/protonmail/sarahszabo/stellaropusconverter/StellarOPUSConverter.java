/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import static com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField.*;
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGreatFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A converter that converts files to the OPUS format.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class StellarOPUSConverter {

    /**
     * Gets the default metadata list.
     *
     * @return The default list
     */
    public static Map<MetadataType, String> getDefaultMetadata() {
        Map<MetadataType, String> metadata = new HashMap<>(3);
        metadata.put(MetadataType.ARTIST, "Unknown Artist");
        metadata.put(MetadataType.TITLE, "Unknown Title");
        metadata.put(MetadataType.ALBUM_ART, "0");
        return metadata;
    }

    private final String fileNameNoEXT, trueFileName;
    private final Path filePath, outputFolder;
    private final Map<MetadataType, String> metadata;
    private final FileExtension fileExtension;
    private final Logger logger;

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path and manually specified output folder.
     *
     * @param filePath The file for conversion
     * @param outputFolder The path to put the finished file in
     * @param logger The cartographer to use with this converter
     * @throws java.io.IOException If something happened
     */
    public StellarOPUSConverter(Path filePath, Path outputFolder, Logger logger) throws IOException {
        //Run through filters before accepting path
        this.filePath = Objects.requireNonNull(StellarGreatFilter.filterPaths(Arrays.asList(filePath)).get(0));
        this.outputFolder = Objects.requireNonNull(outputFolder);
        this.logger = Objects.requireNonNull(logger);
        StellarGravitonField.toTypicalLoggerFormat(this.logger, new ConsoleHandler());
        //Gets only the file name without extension
        this.fileNameNoEXT = FileExtension.stripFileExtension(this.filePath);
        FileExtension dummyExtension = null;
        for (FileExtension extension : FileExtension.values()) {
            Pattern pattern = Pattern.compile(extension.toString(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(this.filePath.getFileName().toString());
            if (matcher.find()) {
                dummyExtension = extension;
            }
        }
        if (dummyExtension == null) {
            throw new IllegalStateException("The chosen file is not among the list of convertable formats");
        } else {
            this.fileExtension = dummyExtension;
        }
        this.trueFileName = preferredTitleFormat(this.fileNameNoEXT + ".opus");
        this.metadata = new HashMap<>(3);
    }

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path and manually specified output folder.
     *
     * @param filePath The file for conversion
     * @param outputFolder The path to put the finished file in
     * @throws java.io.IOException If something happened
     */
    public StellarOPUSConverter(Path filePath, Path outputFolder) throws IOException {
        this(filePath, outputFolder, Logger.getLogger(StellarOPUSConverter.class.getName()));
    }

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path.
     *
     * @param filePath The file for conversion
     * @throws java.io.IOException If something happened
     */
    public StellarOPUSConverter(Path filePath) throws IOException {
        this(filePath, StellarDiskManager.getTempDirectory());
    }

    /**
     * Re indexes an OPUS file, applying modern standards to older versions of
     * .opus files. Uses 320K by default
     */
    public void reIndexOPUSFile() throws IOException {
        reIndexOPUSFile(320);
    }

    /**
     * Re indexes an OPUS file, applying modern standards to older versions of
     * .opus files. Has a parameter for bitrate, ex: 320 -> 320K.
     *
     * @param bitrate The bitrate in K
     * @throws java.io.IOException If something went wrong
     */
    public void reIndexOPUSFile(int bitrate) throws IOException {
        Path tempFilePath = newPath(StellarDiskManager.getTempDirectory(), FileExtension.stripFileExtension(this.filePath));
        Files.copy(this.filePath, tempFilePath);
        Map<MetadataType, String> metadata = StellarDiskManager.getOPUSMetadata(this.filePath);
        this.metadata.putAll(metadata);
        //Convert to FLAC
        String flacFileName = FileExtension.stripFileExtension(tempFilePath) + ".flac";
        processOP("ffmpeg", "-i", tempFilePath.getFileName().toString(), flacFileName);
        //Convert Back to .opus
        processOP("opusenc", flacFileName, FileExtension.stripFileExtension(tempFilePath) + ".opus",
                "--bitrate", bitrate + "k", "--title", metadata.get(MetadataType.TITLE), "--artist", metadata.get(MetadataType.ARTIST));
    }

    /**
     * Converts the selected file to .OPUS. Doesn't attempt to edit the
     * filename, or automatically generate metadata aside for the title, which
     * is set to the title of the track.
     *
     * @return The file path newPath the .opus file.
     * @throws IOException If something went wrong
     */
    public Path convertToOPUSNoAutomaticMetadata() throws IOException {
        this.metadata.put(MetadataType.ARTIST, getDefaultMetadata().get(MetadataType.ARTIST));
        this.metadata.put(MetadataType.TITLE, this.fileNameNoEXT);
        return convertToOPUS(this.metadata.get(MetadataType.ARTIST), this.metadata.get(MetadataType.TITLE));
    }

    /**
     * Converts the selected file to .OPUS. Makes a best effort to get the title
     * name and artist name from the filename. Works if the filename looks like
     * MyTitle -- MyTrack.mp4.
     *
     * @return The file path newPath the .opus file.
     * @throws IOException If something went wrong
     */
    public Path convertToOPUS() throws IOException {
        generateMetadata();
        return convertToOPUS(this.metadata.get(MetadataType.ARTIST), this.metadata.get(MetadataType.TITLE));
    }

    /**
     * Converts the selected file to .OPUS.
     *
     * @param artist The track artist
     * @param title The track title
     * @return The Path to the converted File
     * @throws IOException If something happened
     */
    public Path convertToOPUS(String artist, String title) throws IOException {
        return convertToOPUS(artist, title, 320);
    }

    /**
     * Converts the selected file to .OPUS.
     *
     * @param artist The track artist
     * @param title The track title
     * @param bitrate The bitrate newPath the resultant audio in K, so 320 =
     * 320K
     * @return The Path to the converted File
     * @throws IOException If something happened
     */
    public Path convertToOPUS(String artist, String title, int bitrate) throws IOException {
        //If Metadata Not Already Generated, Set it
        if (this.metadata.isEmpty()) {
            this.metadata.put(MetadataType.ARTIST, preferredTitleFormat(artist));
            this.metadata.put(MetadataType.TITLE, preferredTitleFormat(title));
        }
        copyOP(() -> {
            try {
                processImage();
                processOP(this.fileExtension.getGeneralConversionString(this.filePath.getFileName().toString(), preferredTitleFormat(title),
                        this.metadata, bitrate), false);
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(StellarOPUSConverter.class.getName()).log(Level.SEVERE, null, ex);
                this.logger.log(Level.SEVERE, "Error encountered during conversion", ex);
            }
        });
        //Clear Metadata to Restore Default State for next use
        this.metadata.clear();
        return Paths.get(StellarDiskManager.getOutputFolder().toString(), title + ".opus");
    }

    /**
     * Decreases the selected file's bitrate. Useful for mobile opus libraries.
     * Copies the file to the output folder. If not manually set, defaults to
     * {@link StellarDiskManager#getOutputFolder()}. If not an .opus file,
     * converts to .opus, may not faithfully capture artist/title using this
     * method. Does not attempt to adjust filename using this conversion method.
     *
     * @return The path to the converted file
     * @throws java.io.IOException If something happened
     */
    public Path decreaseBitrate() throws IOException {
        this.metadata.put(MetadataType.ARTIST, "Unknown Artist");
        this.metadata.put(MetadataType.TITLE, this.filePath.getFileName().toString().replace(this.fileExtension.toString(), ""));
        copyOPSameFileName(() -> {
            try {
                processOP(this.fileExtension.getLowBitrateConversion(this.filePath), false);
            } catch (IOException ex) {
                Logger.getLogger(StellarOPUSConverter.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        });
        return Paths.get(this.outputFolder.toString(),
                preferredTitleFormat(this.metadata.get(MetadataType.TITLE)) + this.fileExtension.toString());

    }

    /**
     * An iterated version newPath
     * {@link Objects#requireNonNull(java.lang.Object)}
     *
     * @param <T> The type
     * @param objects The objects to test
     */
    @SafeVarargs
    @SuppressWarnings("FinalPrivateMethod")
    private final <T> void requireNonNullIterated(T... objects) {
        for (T t : objects) {
            Objects.requireNonNull(t);
        }
    }

    /**
     * Makes a best effort to find the metadata Author and Title from the
     * filename, and tags the opus file with it. The opus file generated is
     * within start and end times specified.
     *
     * @param start The start time newPath the recording window
     * @param end The end newPath the recording window
     * @return The path to the newly created file
     * @throws java.io.IOException If something happened
     */
    public Path convertToOPUS(StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end) throws IOException {
        //Convert to Proper Format
        StellarUI.askUserForArtistTitle("Start: " + start + "\nEnd: " + end)
                .entrySet().stream().map(entry -> new Map.Entry<MetadataType, String>() {
            @Override
            public MetadataType getKey() {
                return entry.getKey();
            }

            @Override
            public String getValue() {
                return preferredTitleFormat(entry.getValue());
            }

            @Override
            public String setValue(String value) {
                String val = entry.getValue();
                entry.setValue(preferredTitleFormat(value));
                return val;
            }
        }).collect(Collectors.toMap(key -> key.getKey(), value -> value.getValue()));
        return convertToOPUS(this.metadata.get(MetadataType.ARTIST), this.metadata.get(MetadataType.TITLE), start, end);
    }

    /**
     * Converts the selected file to .OPUS. Time start and end are expected to
     * be newPath the form HH:MM:SS. So: 00:01:23 with nothing else in the
     * string.
     *
     * @param artist The track artist
     * @param title The track title
     * @param start The starting time
     * @param end The end time
     * @return The path to the newly created file
     * @throws IOException If something happened
     * @throws IllegalArgumentException If start or end is not in the specified
     * format, or if start is less than or equal to zero.
     */
    public Path convertToOPUS(String artist, String title, StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end) throws IOException {
        requireNonNullIterated(artist, title, start, end);
        if (artist.isEmpty() || title.isEmpty()) {
            throw new IllegalArgumentException("One of the fields is empty!");
        }
        if (start.compareTo(end) >= 0) {
            throw new IllegalArgumentException("Start is less than or equal to end!");
        }
        //Were we a part newPath the constructor chain? If not, set metadata
        if (this.metadata.isEmpty()) {
            this.metadata.put(MetadataType.ARTIST, artist);
            this.metadata.put(MetadataType.TITLE, title);
        }
        //ffmpeg -ss 00:00:24.0 -i 40.mp4 -t 00:01:20.0 -y -b:a 320k 40.opus
        copyOP(() -> {
            try {
                processImage();
                processOP("ffmpeg", "-i", this.filePath.getFileName().toString(),
                        "-ss", start.getTimestamp(), "-to", end.getTimestamp(), "-y", "-b:a", "320k", "-metadata", "title=" + title,
                        "-metadata", "artist=" + artist, "-strict", "-2", preferredTitleFormat(title) + ".opus");
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(StellarOPUSConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        //Clear Metadata to Restore to Default State
        this.metadata.clear();
        return Paths.get(StellarDiskManager.getOutputFolder().toString(), title + ".opus");
    }

    /**
     * A utility method for getting metadata from the targetted filename.
     * Specify the regex for the separator. Filename usually take on the form
     * newPath SONGAUTHOR -- SONGTITLE. Separators usually look like "[-+]".
     *
     * @param seperator The regular expression used for the separator
     * @return The metadata list
     */
    private List<String> getListFromRegex(String seperator) {
        //Filters out the empty space in returned list and maps it to a list after trimming empty space
        return Arrays.asList(this.fileNameNoEXT.split(seperator)).stream().filter(string -> !string.isEmpty())
                .map(string -> string.trim()).collect(Collectors.toList());
    }

    /**
     * The previous program versions used a list to keep track of metadata, this
     * is an adapter subroutine for that format.
     */
    private Map<MetadataType, String> listToMap(List<String> list) {
        Map<MetadataType, String> map = new HashMap<>(2);
        map.put(MetadataType.ARTIST, list.get(0));
        map.put(MetadataType.TITLE, list.get(1));
        return map;
    }

    /**
     * Tests to see if the format is matched. Specify the regex for the
     * separator. Filename usually take on the form newPath SONGAUTHOR --
     * SONGTITLE. Separators usually look like "+" or "|". Returns a list that,
     * by convention has zeroith element as the artist, and second element as
     * the title.
     *
     * @param separators The regular expressions to use
     * @return The metadata list
     */
    private void generateMetadataFromRegex(String... separators) {
        List<String> dividers = Arrays.asList(separators);
        if (dividers.stream().anyMatch(separator -> this.fileNameNoEXT.matches(".*[" + separator + "]+.*"))) {
            //Maps to proper metadata format
            this.metadata.putAll(listToMap(getListFromRegex("[" + dividers.stream().filter(separator
                    -> this.fileNameNoEXT.matches(".*[" + separator + "+].*")).findFirst().get() + "*]").stream()
                    .map(string -> preferredTitleFormat(string)).collect(Collectors.toList())));

        } else {
            this.metadata.putAll(StellarUI.askUserForArtistTitle(this.fileNameNoEXT));
        }
    }

    /**
     * Gets the title and author from the file name. By convention, the zeroith
     * element is the artist, and the first is the title track.
     *
     * @return The list newPath information, or empty if none was available
     */
    private void generateMetadata() {
        generateMetadataFromRegex("-", "|", "/");
    }

    /**
     * Performs an operation after first copying the target file over to the
     * temp directory. Ensures that the copied filename is the same as the
     * original. Copies the file to the OPUS converter's output folder. If not
     * manually set, defaults to {@link StellarDiskManager#getOutputFolder()}.
     *
     * @param operation The operations to perform
     * @throws IOException If something happened
     */
    private void copyOPSameFileName(CopyOperation operation) throws IOException {
        String originalFileName = this.filePath.getFileName().toString();
        StellarDiskManager.copyToTemp(this.filePath);
        operation.doOperation();
        //The filepath of the (((Copy))).opus file
        Path copyDummyPath = newPath(StellarDiskManager.getTempDirectory(),
                preferredTitleFormat(originalFileName.replace(this.fileExtension.toString(), "(((Copy))).opus"))),
                originalFileNamePath = Paths.get(StellarDiskManager.getTempDirectory().toString(), originalFileName);
        //Logging
        this.logger.log(Level.FINE, "FROM: \n{0}", copyDummyPath);
        this.logger.log(Level.FINE, "TO: {0}", originalFileNamePath);
        try {
            //Replacing Original Only Makes Sense for .opus Files
            if (this.fileExtension == FileExtension.OPUS) {
                //Move to same directory to take the place newPath the original file
                Files.move(copyDummyPath, originalFileNamePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                //Copy to Output Folder, due to different output folder, use custom copy operation
                Files.move(originalFileNamePath, newPath(this.outputFolder, originalFileNamePath.getFileName()));
            } else {
                //Just Move the .opus File
                Files.move(copyDummyPath, newPath(this.outputFolder, trueFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            this.logger.log(Level.SEVERE, "copyOP -> Move Section", ex);
            //throw new RuntimeException(ex);
        }
    }

    /**
     * Performs an operation after first copying the target file over to the
     * temp directory.
     *
     * @param operation The operations to perform
     * @throws IOException If something happened
     */
    private void copyOP(CopyOperation operation) throws IOException {
        StellarDiskManager.copyToTemp(this.filePath);
        operation.doOperation();
        StellarDiskManager.copyFromTemp(preferredTitleFormat(this.metadata.get(MetadataType.TITLE)) + ".opus");
    }

    /**
     * Gets the filename format for the image conversion process. The metadata
     * must be set before calling this method.
     *
     * @return The filename
     */
    private String getImageFileName() {
        return preferredTitleFormat(this.metadata.get(MetadataType.ARTIST)) + " -- "
                + preferredTitleFormat(this.metadata.get(MetadataType.TITLE)) + ".png";
    }

    /**
     * Gets the image from the video, only called after the video has been moved
     * to the working directory.
     *
     * @throws IOException If something went wrong
     */
    private void processImage() throws IOException, InterruptedException {
        Path picturePath = newPath(StellarDiskManager.getOutputFolder(), getImageFileName());
        //Check if Image Already Exists, if not, generate image
        if (!Files.exists(picturePath)) {
            //ffmpeg -ss 25 -i input.mp4 -qscale:v 2 -frames:v 1 -huffman optimal output.jpg
            processOP("ffmpeg", "-ss", "30", "-i", this.filePath.getFileName().toString(), "-y", "-qscale:v", "2",
                    "-frames:v", "1", "-huffman", "optimal", getImageFileName());
            //Copy Image to Picture Output Folder
            //Files.copy(newPath(StellarDiskManager.getTempDirectory(), getImageFileName()),
            //      newPath(StellarDiskManager.getPictureOutputFolder(), getImageFileName()));
            //Assign Picture to File
            //TODO: Disable in Testing Environment
            ProcessBuilder builder = processOPBuilder(false, "kid3-cli");
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process process = builder.start();
            process.getInputStream().read("pwd\n".getBytes());
            System.exit(0);
        }
    }

    /**
     * Represents an operation to be performed.
     */
    @FunctionalInterface
    private static interface CopyOperation {

        /**
         * The operation to perform.
         */
        void doOperation();
    }

    /**
     * An enum representing .opus metadata information
     */
    public static enum MetadataType {
        /**
         * The artist of the track.
         */
        ARTIST,
        /**
         * The title of the track
         */
        TITLE,
        /**
         * The album art of the track.
         */
        ALBUM_ART;
    }

    /**
     * An enum representing a file extension.
     */
    public static enum FileExtension {
        OGG {
            @Override
            public List<String> getGeneralConversionString(String inputFileName, String outputFileName, Map<MetadataType, String> metadata,
                    int bitrate) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public String toString() {
                return ".ogg";
            }

        },
        /**
         * Already Existing OPUS Files. The bitrate option does not apply here
         * since we are copying the bitrate over.
         */
        OPUS {
            @Override
            public List<String> getGeneralConversionString(String inputFileName, String outputFileName, Map<MetadataType, String> metadata,
                    int bitrate) {
                //ffmpeg -i HI.opus -c copy -metadata artist="Someone" output.opus
                return new ProcessBuilder("ffmpeg", inputFileName, "-c", "copy", "-y", "-metadata", "title=" + metadata.get(MetadataType.TITLE),
                        "-metadata", "artist=" + metadata.get(MetadataType.ARTIST), "-strict", "-2",
                        preferredTitleFormat(outputFileName + ".opus")).command();
            }

            @Override
            public String toString() {
                return ".opus";
            }
        },
        /**
         * MP4 Files.
         */
        MP4 {
            @Override
            public String toString() {
                return ".mp4";
            }
        },
        /**
         * MP3 Files.
         */
        MP3 {
            @Override
            public String toString() {
                return ".mp3";
            }
        };

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
            return new ProcessBuilder("ffmpeg", "-i", inputFile.getFileName().toString(), "-b:a", "120k", "-strict",
                    "-2", preferredTitleFormat(inputFile.getFileName().toString().replace(toString(), "") + "(((Copy))).opus")).command();
        }

        /**
         * Gets the conversion string for
         * {@link StellarOPUSConverter#processOP(java.lang.String...)}, give it
         * the input file name, and the output file name, and it will give you
         * the string list necessary to do the operation.
         *
         * @param inputFileName The filename newPath the input file, should
         * contain the file extension
         * @param outputFileName The filename newPath the output file, should
         * NOT contain the file extension
         * @return The process necessary to convert this filetype to OPUS.
         */
        public List<String> getGeneralConversionString(String inputFileName, String outputFileName, Map<MetadataType, String> metadata,
                int bitrate) {
            return new ProcessBuilder("ffmpeg", "-i", inputFileName,
                    "-y", "-b:a", bitrate + "k", "-metadata", "title=" + metadata.get(MetadataType.TITLE),
                    "-metadata", "artist=" + metadata.get(MetadataType.ARTIST),
                    "-strict", "-2", preferredTitleFormat(outputFileName + ".opus")).command();
        }

        public abstract String toString();
    }
}
