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
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A converter that converts files to the OPUS format. NOTE: Requires these
 * libraries to use successfully: exiftool, ffmpeg, and opusenc libraries.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class StellarOPUSConverter {

    /**
     * Strips the file extension off newPath the fileName and returns it.
     * Convenience subroutine for {@link FileExtension#stripFileExtension(java.nio.file.Path)
     * }
     *
     * @param path The path to have stripped
     * @return The stripped file name
     */
    public static String stripFileExtension(Path path) {
        return FileExtension.stripFileExtension(path);
    }

    /**
     * Gets the filename format for the image conversion process. The metadata
     * must be set before calling this method.
     *
     * @param metadata The metadata list to generate the filename from
     * @return The filename with .png extension
     */
    public static String getImageFileName(Map<StellarOPUSConverter.MetadataType, String> metadata) {
        return preferredTitleFormat(metadata.get(MetadataType.ARTIST)) + " -- "
                + preferredTitleFormat(metadata.get(MetadataType.TITLE)) + ".png";
    }

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

    /**
     * A utility method for getting metadata from the targetted filename.
     * Specify the regex for the separator. Filename usually take on the form
     * newPath SONGAUTHOR -- SONGTITLE. Separators usually look like "[-+]".
     *
     * @param seperator The regular expression used for the separator
     * @return The metadata list
     */
    private static List<String> getListFromRegex(String seperator, Path filePath) {
        //Filters out the empty space in returned list and maps it to a list after trimming empty space
        String originalFileNameNoEXT = stripFileExtension(filePath);
        return Arrays.asList(originalFileNameNoEXT.split(seperator)).stream().filter(string -> !string.isEmpty())
                .map(string -> string.trim()).collect(Collectors.toList());
    }

    /**
     * The previous program versions used a list to keep track of metadata, this
     * is an adapter subroutine for that format.
     */
    private static Map<MetadataType, String> listToMap(List<String> list) {
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
    private static Map<MetadataType, String> generateMetadataFromRegex(Path filePath, String... separators) {
        List<String> dividers = Arrays.asList(separators);
        Map<MetadataType, String> map = getDefaultMetadata();
        String originalFileNameNoEXT = stripFileExtension(filePath);
        if (dividers.stream().anyMatch(separator -> originalFileNameNoEXT.matches(".*[" + separator + "]+.*"))) {
            //Maps to proper metadata format
            map.putAll(listToMap(getListFromRegex("[" + dividers.stream().filter(separator
                    -> originalFileNameNoEXT.matches(".*[" + separator + "+].*")).findFirst().get() + "*]", filePath).stream()
                    .map(string -> preferredTitleFormat(string)).collect(Collectors.toList())));
        }
        return map;
    }

    /**
     * Gets the title and author from the file name, and fills the map with this
     * data.
     *
     * @param filePath The path of the file for which metadata analysis is
     * taking place
     * @return The map of the metadata found or
     * {@link StellarOPUSConverter#getDefaultMetadata()} if none was found
     */
    public static Map<MetadataType, String> generateMetadata(Path filePath) {
        return generateMetadataFromRegex(filePath, "-", "|", "/");
    }
    private final String originalFileNameNoEXT, originalFileNameNoEXTPreferred;
    private final Path originalFilePath, outputFolder;
    private final Map<MetadataType, String> metadata;
    private final FileExtension fileExtension;
    private final Logger logger;
    private Path opusFilePath;
    private String opusFileName;

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path and manually specified output folder. NOTE: the album art of the
     * metadata should be an absolute path to the image file.
     *
     * @param filePath The file for conversion
     * @param outputFolder The path to put the finished file in
     * @param logger The cartographer to use with this converter
     * @param metadata The metadata to use for this converter
     * @throws java.io.IOException If something happened
     */
    public StellarOPUSConverter(Path filePath, Path outputFolder, Logger logger, Map<MetadataType, String> metadata) throws IOException {
        //Run through filters before accepting path
        this.originalFilePath = Objects.requireNonNull(StellarGreatFilter.filterPaths(Arrays.asList(filePath)).get(0));
        this.outputFolder = Objects.requireNonNull(outputFolder);
        this.logger = Objects.requireNonNull(logger);
        StellarGravitonField.toTypicalLoggerFormat(this.logger, new ConsoleHandler());
        //Gets only the file name without extension
        this.originalFileNameNoEXT = FileExtension.stripFileExtension(this.originalFilePath);
        FileExtension dummyExtension = null;
        for (FileExtension extension : FileExtension.values()) {
            Pattern pattern = Pattern.compile(extension.toString(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(this.originalFilePath.getFileName().toString());
            if (matcher.find()) {
                dummyExtension = extension;
                break;
            }
        }
        if (dummyExtension == null) {
            throw new IllegalStateException("The chosen file is not among the list of convertable formats");
        } else {
            this.fileExtension = dummyExtension;
        }
        this.originalFileNameNoEXTPreferred = preferredTitleFormat(this.originalFileNameNoEXT);
        this.opusFileName = preferredTitleFormat(this.originalFileNameNoEXTPreferred + ".opus");
        this.opusFilePath = newPath(this.outputFolder, this.opusFileName);
        //Artist/Title Cannot be Null
        if (metadata.get(MetadataType.ARTIST) == null || metadata.get(MetadataType.TITLE) == null) {
            throw new IllegalArgumentException("Artist/Title cannot be null in passed metadata");
        }
        this.metadata = new HashMap<>(Objects.requireNonNull(metadata));
    }

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
        this(filePath, outputFolder, logger, StellarDiskManager.getMetadata(filePath));
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
        this(filePath, StellarDiskManager.getOutputFolder());
    }

    /**
     * Checks if the metadata has been modified from the default settings.
     *
     * @return Whether or not this is true
     */
    private boolean metadataModified() {
        return !this.metadata.equals(getDefaultMetadata());
    }

    /**
     * Re indexes an OPUS file, applying modern standards to older versions of
     * .opus files. Uses 320K by default. The .opus file must exist already to
     * use this subroutine. Chains to
     * {@link StellarOPUSConverter#reIndexOPUSFile(int)}
     *
     * @throws java.io.IOException If something went wrong
     */
    public void reIndexOPUSFile() throws IOException {
        reIndexOPUSFile(320);
    }

    /**
     * Checks that the .opus file exists or throws an exception.
     */
    private void checkFileExists() {
        if (!Files.exists(this.originalFilePath)) {
            throw new IllegalStateException("You're using a subroutine that requires that the .opus file exists already,"
                    + " but you're called it at a time when it doesn't!");
        }
    }

    /**
     * Re indexes an OPUS file, applying modern standards to older versions of
     * .opus files. Has a parameter for bitrate, ex: 320 -> 320K. The .opus file
     * must exist already to use this subroutine. Uses the metadata provided in
     * the constructor, or defaults if none were set.
     *
     * @param bitrate The bitrate in K
     * @throws java.io.IOException If something went wrong
     */
    public void reIndexOPUSFile(int bitrate) throws IOException {
        checkFileExists();
        //Convert to .opus
        Path opusFile = toOpusFile(bitrate, null, null);
        //Copy Converted File to Output Directory
        StellarDiskManager.copyFromTemp(opusFile.getFileName().toString());
    }

    /**
     * Converts the selected file to .OPUS. Doesn't attempt to edit the
     * filename, or automatically generate metadata aside for the title, which
     * is set to the title of the track.
     *
     * @return The file path newPath the .opus file.
     * @throws IOException If something went wrong
     */
    public Optional<Path> convertToOPUSNoAutomaticMetadata() throws IOException {
        this.metadata.put(MetadataType.ARTIST, getDefaultMetadata().get(MetadataType.ARTIST));
        this.metadata.put(MetadataType.TITLE, this.originalFileNameNoEXT);
        return convertToOPUS(this.metadata.get(MetadataType.ARTIST), this.metadata.get(MetadataType.TITLE));
    }

    /**
     * Converts the selected file to .OPUS. Makes a best effort to get the title
     * name and artist name from the filename. Works if the filename looks like
     * MyTitle -- MyTrack.mp4. If the metadata has been set, this subroutine
     * defaults to the set metadata.
     *
     * @param bitrate The bitrate for the converted files
     * @return The file path newPath the .opus file.
     * @throws IOException If something went wrong
     */
    public Optional<Path> convertToOPUS(int bitrate) throws IOException {
        //If Metadata not modified from default, generate metadata
        if (this.fileExtension == FileExtension.MP4) {
            generateMetadata();
            return convertToOPUS(this.metadata.get(MetadataType.ARTIST), this.metadata.get(MetadataType.TITLE));
        }//Metadata modified, return opus file with set metadata
        else {
            //Attempt to set Metadata if We Have SOME default fields
            Map<MetadataType, String> map = generateMetadata(this.originalFilePath);
            //Set Metadata if not already present
            if (map.get(MetadataType.TITLE).equalsIgnoreCase(getDefaultMetadata().get(MetadataType.TITLE))) {
                this.metadata.put(MetadataType.TITLE, this.originalFileNameNoEXTPreferred);
            } //Metadata mapping found data, set it
            else {
                this.metadata.put(MetadataType.TITLE, map.get(MetadataType.TITLE));
                this.metadata.put(MetadataType.ARTIST, map.get(MetadataType.ARTIST));
            }
            //If The Default metadata is Present, Select a Random Default Image
            if (this.metadata.get(MetadataType.ALBUM_ART).equalsIgnoreCase(getDefaultMetadata().get(MetadataType.ALBUM_ART))) {
                this.metadata.put(MetadataType.ALBUM_ART, StellarDiskManager.getGenericPicture().toAbsolutePath().toString());
            }
            return Optional.of(toOpusFile(bitrate));
        }
    }

    /**
     * Converts the selected file to .OPUS. Makes a best effort to get the title
     * name and artist name from the filename. Works if the filename looks like
     * MyTitle -- MyTrack.mp4. If the metadata has been set, this subroutine
     * defaults to the set metadata.
     *
     * @return The file path newPath the .opus file.
     * @throws IOException If something went wrong
     */
    public Optional<Path> convertToOPUS() throws IOException {
        return convertToOPUS(320);
    }

    /**
     * Converts the selected file to .OPUS.
     *
     * @param artist The track artist
     * @param title The track title
     * @return The Path to the converted File
     * @throws IOException If something happened
     */
    public Optional<Path> convertToOPUS(String artist, String title) throws IOException {
        return convertToOPUS(artist, title, 320);
    }

    /**
     * Turns the original file that this opus converter was pointing at to a
     * .flac file.
     *
     * @param start The start time
     * @param end The end time
     * @return The path to this file
     * @throws java.io.IOException If something went wrong
     */
    private Path toFlacFile(StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end) throws IOException {
        StellarDiskManager.copyToTemp(this.originalFilePath);
        String title = this.metadata.get(MetadataType.TITLE) + ".flac";
        //Fast FLAC Audio ripped from video
        //ffmpeg -i "video.m2ts" -vn -sn -acodec flac -compression_level 12 "audio.flac"
        processOP(true, "ffmpeg", "-i", this.originalFilePath.getFileName().toString(), "-ss", start.toString(),
                "-t", end.toString(), "-vn", "-sn", "-acodec", "flac",
                "-compression_level", "12", title);
        return newPath(StellarDiskManager.getTempDirectory(), title);
    }

    /**
     * Turns the original file that this opus converter was pointing at to a
     * .flac file.
     *
     * @return The path to this file
     * @throws java.io.IOException If something went wrong
     */
    private Path toFlacFile() throws IOException {
        StellarDiskManager.copyToTemp(this.originalFilePath);
        String title = this.metadata.get(MetadataType.TITLE) + ".flac";
        //Fast FLAC Audio ripped from video
        //ffmpeg -i "video.m2ts" -vn -sn -acodec flac -compression_level 12 "audio.flac"
        processOP(true, "ffmpeg", "-i", this.originalFilePath.getFileName().toString(), "-vn", "-sn", "-acodec", "flac",
                "-compression_level", "0", title);
        return newPath(StellarDiskManager.getTempDirectory(), title);
    }

    /**
     * Uses opusenc to create a .opus file from a temporary .flac file. Both
     * timestamps may be null. If the either timestamp is null, the times are
     * ignored.
     *
     * @param start The start time
     * @param end The end time
     *
     * @param bitrate The bitrate in K
     * @return The path to the newly created .opus file
     * @throws IOException If something went wrong
     */
    private Path toOpusFile(int bitrate) throws IOException {
        return toOpusFile(bitrate, null, null);
    }

    /**
     * Uses opusenc to create a .opus file from a temporary .flac file. Both
     * timestamps may be null. If the either timestamp is null, the times are
     * ignored.
     *
     * @param start The start time
     * @param end The end time
     *
     * @param bitrate The bitrate in K
     * @return The path to the newly created .opus file
     * @throws IOException If something went wrong
     */
    private Path toOpusFile(int bitrate, StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end) throws IOException {
        Path flacFile = start == null || end == null ? toFlacFile() : toFlacFile(start, end);
        //Delete Intermediate .opus File Before Running New .opus Conversion
        Files.deleteIfExists(newPath(StellarDiskManager.getTempDirectory(), this.opusFileName));
        String title = FileExtension.stripFileExtension(flacFile) + ".opus";
        processOP(true, "opusenc", flacFile.getFileName().toString(), title,
                "--bitrate", bitrate + "k", "--title", this.metadata.get(MetadataType.TITLE), "--artist", this.metadata.get(MetadataType.ARTIST),
                "--picture", this.metadata.get(MetadataType.ALBUM_ART));
        //If we have metadata title, return that as the filename
        String fileTitle = this.metadata.get(MetadataType.TITLE).equalsIgnoreCase(getDefaultMetadata().get(MetadataType.TITLE))
                ? this.opusFileName : this.metadata.get(MetadataType.TITLE) + ".opus";
        //Copy Back from temp folder
        Files.copy(newPath(StellarDiskManager.getTempDirectory(), this.metadata.get(MetadataType.TITLE) + ".opus"),
                newPath(this.outputFolder, fileTitle), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        return newPath(StellarDiskManager.getTempDirectory(), title);
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
    public Optional<Path> convertToOPUS(String artist, String title, int bitrate) throws IOException {
        copyOP(() -> {
            try {
                //Create Image
                processImage();
                //Encode new .opus file using metadata set from .flac file & image in pictures folder
                toOpusFile(bitrate, null, null);
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(StellarOPUSConverter.class.getName()).log(Level.SEVERE, null, ex);
                this.logger.log(Level.SEVERE, "Error encountered during conversion", ex);
            }
        });
        //Clear Metadata to Restore Default State for next use
        this.metadata.clear();
        return Optional.of(newPath(StellarDiskManager.getOutputFolder(), title + ".opus"));
    }

    /**
     * Decreases the selected file's bitrate. Useful for mobile opus libraries.
     * Copies the file to the output folder. If not manually set, defaults to
     * {@link StellarDiskManager#getOutputFolder()}. If not an .opus file,
     * converts to .opus, may not faithfully capture artist/title using this
     * method. Does not attempt to adjust filename using this conversion method.
     * The .opus file must exist already to use this subroutine.
     *
     * @return The path to the converted file
     * @throws java.io.IOException If something happened
     */
    public Path decreaseBitrate() throws IOException {
        checkFileExists();
        this.metadata.put(MetadataType.TITLE, this.originalFilePath.getFileName().toString().replace(this.fileExtension.toString(), ""));
        copyOPSameFileName(() -> {
            try {
                processOP(this.fileExtension.getLowBitrateConversion(this.originalFilePath), false);
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
    public Optional<Path> convertToOPUS(StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end) throws IOException {
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
    public Optional<Path> convertToOPUS(String artist, String title, StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end) throws IOException {
        return convertToOPUS(320, artist, title, start, end);
    }

    /**
     * Converts the selected file to .OPUS. Time start and end are expected to
     * be newPath the form HH:MM:SS. So: 00:01:23 with nothing else in the
     * string.
     *
     * @param bitrate The bitrate in K
     * @param artist The track artist
     * @param title The track title
     * @param start The starting time
     * @param end The end time
     * @return The path to the newly created file
     * @throws IOException If something happened
     * @throws IllegalArgumentException If start or end is not in the specified
     * format, or if start is less than or equal to zero.
     */
    public Optional<Path> convertToOPUS(int bitrate, String artist, String title, StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end) throws IOException {
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
                toOpusFile(bitrate, start, end);
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(StellarOPUSConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        //Reindex to Save Album Art
        reIndexOPUSFile();
        //Clear Metadata to Restore to Default State
        this.metadata.clear();
        return Optional.of(newPath(StellarDiskManager.getOutputFolder(), title + ".opus"));
    }

    /**
     * Tests to see if the format is matched. Specify the regex for the
     * separator. Filename usually take on the form newPath SONGAUTHOR --
     * SONGTITLE. Separators usually look like "+" or "|". Returns a list that,
     * by convention has zeroith element as the artist, and second element as
     * the title.
     *
     * @param separators The regular expressions to use
     * @param map The map to fill with the metadata
     * @return The metadata list
     */
    private void generateMetadataFromRegex(String... separators) {
        List<String> dividers = Arrays.asList(separators);
        if (dividers.stream().anyMatch(separator -> this.originalFileNameNoEXT.matches(".*[" + separator + "]+.*"))) {
            //Maps to proper metadata format
            this.metadata.putAll(listToMap(getListFromRegex("[" + dividers.stream().filter(separator
                    -> this.originalFileNameNoEXT.matches(".*[" + separator + "+].*")).findFirst().get() + "*]", this.originalFilePath).stream()
                    .map(string -> preferredTitleFormat(string)).collect(Collectors.toList())));
        } else {
            this.metadata.putAll(StellarUI.askUserForArtistTitle(this.originalFileNameNoEXT));
        }
    }

    /**
     * Gets the title and author from the file name
     *
     * @param map The map to fill with metadata
     * @return The list newPath information, or empty if none was available
     */
    private void generateMetadata() {
        generateMetadataFromRegex("-", "|", "/");
        this.opusFileName = preferredTitleFormat(this.metadata.get(MetadataType.TITLE) + ".opus");
        this.opusFilePath = newPath(this.outputFolder, this.opusFileName);
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
        String originalFileName = this.originalFilePath.getFileName().toString();
        StellarDiskManager.copyToTemp(this.originalFilePath);
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
                Files.move(copyDummyPath, newPath(this.outputFolder, opusFileName), StandardCopyOption.REPLACE_EXISTING);
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
     * @param copyBackFileName The file name to copy back to the output folder
     * @throws IOException If something happened
     */
    private void copyOP(CopyOperation operation, String copyBackFileName) throws IOException {
        StellarDiskManager.copyToTemp(this.originalFilePath);
        operation.doOperation();
        StellarDiskManager.copyFromTemp(copyBackFileName);
    }

    /**
     * Performs an operation after first copying the target file over to the
     * temp directory.
     *
     * @param operation The operations to perform
     * @throws IOException If something happened
     */
    private void copyOP(CopyOperation operation) throws IOException {
        copyOP(operation, preferredTitleFormat(this.metadata.get(MetadataType.TITLE)) + ".opus");
    }

    /**
     * Gets the filename format for the image conversion process. The metadata
     * must be set before calling this method.
     *
     * @return The filename with .png extension
     */
    private String getImageFileName() {
        return preferredTitleFormat(this.metadata.get(MetadataType.ARTIST)) + " -- "
                + preferredTitleFormat(this.metadata.get(MetadataType.TITLE)) + ".png";
    }

    /**
     * Gets the image from the video, only called after the video has been moved
     * to the working directory and attaches the image to the .opus file.
     *
     * @throws IOException If something went wrong
     */
    private void processImage() throws IOException, InterruptedException {
        Path imageFilePath = this.fileExtension == FileExtension.MP4 ? newPath(StellarDiskManager.getPictureOutputFolder(), getImageFileName())
                : StellarUI.getFile("Choose an Image for " + this.opusFileName, StellarUI.EXTENSION_FILTER.PICTURE_FILES)
                        .orElseThrow(() -> new RuntimeException("No Image Selected, program aborting"));
        //If we're pointing at a video file, get it's image at 25s
        if (this.fileExtension == FileExtension.MP4) {
            //Check if Image Already Exists, if not, generate image
            //Are we adding a picture from .opus or a video file, in one case ask user for picture, in other case grab fom video
            if (!Files.exists(imageFilePath)) {
                //ffmpeg -ss 25 -i input.mp4 -qscale:v 2 -frames:v 1 -huffman optimal output.jpg
                processOP("ffmpeg", "-ss", "30", "-i", this.originalFilePath.getFileName().toString(), "-y", "-qscale:v", "2",
                        "-frames:v", "1", "-huffman", "optimal", getImageFileName());
                //Copy Image to Picture Output Folder
                Files.copy(newPath(StellarDiskManager.getTempDirectory(), getImageFileName()),
                        imageFilePath);
            }
        }
        this.metadata.put(MetadataType.ALBUM_ART, imageFilePath.toAbsolutePath().toString());
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
        M4A {
            @Override
            public String toString() {
                return ".m4a";
            }

        },
        FLAC {
            @Override
            public String toString() {
                return ".flac";
            }

        },
        OGG {

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
            return new ProcessBuilder("ffmpeg", "-i", inputFile.getFileName().toString(), "-b:a", "190k", "-strict",
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
