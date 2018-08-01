/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import static com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField.*;
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGreatFilter;
import com.protonmail.sarahszabo.stellaropusconverter.util.StellarLoggingFormatter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A converter that converts files to the OPUS format. NOTE: Requires these
 * libraries to use successfully: exiftool, ffmpeg, and opusenc libraries.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class StellarOPUSConverter {

    /**
     * The created by tag, used for metadata comments.
     */
    public static final String CREATED_BY_TAG = MetadataType.CREATED_BY.toString();
    /**
     * The date time formatter used for metadata.
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Helper method for automating comparisons against default metadata fields.
     * Specify the field for our metadata, and we'll compare it against the
     * default metadata fields.
     *
     * @param type The type to compare
     * @param metadata The metadata to compare against
     * @return Whether or not they are equal
     */
    public static boolean isDefaultMetadata(MetadataType type, ConverterMetadata metadata) {
        if (type == MetadataType.ARTIST) {
            return metadata.getArtist().equalsIgnoreCase(getDefaultMetadata().getArtist());
        } else if (type == MetadataType.TITLE) {
            return metadata.getTitle().equalsIgnoreCase(getDefaultMetadata().getTitle());
        } else if (type == MetadataType.CREATED_BY) {
            return metadata.getCreatedBy().equalsIgnoreCase(getDefaultMetadata().getCreatedBy());
        } else if (type == MetadataType.DATE) {
            return metadata.getDate().equals(getDefaultMetadata().getDate());
        } else if (type == MetadataType.BITRATE) {
            return metadata.getBitrate() == ConverterMetadata.DEFAULT_METADATA.getBitrate();
        } else if (type == MetadataType.ALBUM_ART) {
            return metadata.getAlbumArtPath().equals(ConverterMetadata.DEFAULT_METADATA.getAlbumArtPath());
        } else {
            throw new IllegalStateException("Unrecognized Metadata Option");
        }
    }

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
    public static ConverterMetadata getDefaultMetadata() {
        return ConverterMetadata.DEFAULT_METADATA;
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
    private static ConverterMetadata generateMetadataFromRegex(Path filePath, String... separators) {
        List<String> dividers = Arrays.asList(separators);
        ConverterMetadataBuilder builder = new ConverterMetadataBuilder(getDefaultMetadata());
        String originalFileNameNoEXT = stripFileExtension(filePath);
        if (dividers.stream().anyMatch(separator -> originalFileNameNoEXT.matches(".*[" + separator + "]+.*"))) {
            //Maps to proper metadata format
            HashMap<MetadataType, String> map = new HashMap<>(3);
            map.putAll(listToMap(getListFromRegex("[" + dividers.stream().filter(separator
                    -> originalFileNameNoEXT.matches(".*[" + separator + "+].*")).findFirst().get() + "*]", filePath).stream()
                    .map(string -> preferredTitleFormat(string)).collect(Collectors.toList())));
        }
        return builder.buildMetadata();
    }

    /**
     * Gets the title and author from the file name, and fills the map with this
     * data.
     *
     * @param filePath The path of the file for which metadata analysis is
     * taking place
     * @return The metadata found or
     * {@link StellarOPUSConverter#getDefaultMetadata()} if none was found
     */
    public static ConverterMetadata generateMetadata(Path filePath) {
        return generateMetadataFromRegex(filePath, "-", "|", "/");
    }

    private final String originalFileNameNoEXT, originalFileNameNoEXTPreferred;
    private final Path originalFilePath, outputFolder;
    private final ConverterMetadataBuilder metadata;
    private final FileExtension fileExtension;
    private final Logger logger;
    private Path opusFilePath;
    private String opusFileName;

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path and manually specified output folder. If the converted file already
     * exists, it is overwritten. For supported file extensions, see
     * {@link FileExtension}.
     *
     * @param filePath The file for conversion
     * @param outputFolder The path to put the finished file in
     * @param logger The cartographer to use with this converter
     * @param metadata The metadata to use for this converter
     * @throws java.io.IOException If something happened
     * @throws IllegalArgumentException If file type not supported by this
     * converter
     */
    public StellarOPUSConverter(Path filePath, Path outputFolder, Logger logger, ConverterMetadata metadata) throws IOException {
        if (Files.isDirectory(filePath)) {
            throw new IllegalArgumentException("The given file path is that of a direcory!");
        } else if (!Files.isDirectory(outputFolder)) {
            throw new IllegalArgumentException("Output folder isn't a directory!");
        }
        //Run through filters before accepting path
        this.originalFilePath = Objects.requireNonNull(StellarGreatFilter.filterPaths(Arrays.asList(filePath)).get(0));
        this.outputFolder = Objects.requireNonNull(outputFolder);
        this.logger = Objects.requireNonNull(logger);
        StellarGravitonField.toTypicalLoggerFormat(this.logger, new ConsoleHandler());
        //Gets only the file name without extension
        this.originalFileNameNoEXT = FileExtension.stripFileExtension(this.originalFilePath);
        String fileExtention = filePath.getFileName().toString();
        try {
            this.fileExtension = FileExtension.valueOf(fileExtention.substring(fileExtention.lastIndexOf('.') + 1).toUpperCase());
        } catch (IllegalArgumentException ex) {
            logger.severe("The file extention type is not supported by the converter!" + filePath.toString());
            throw new IllegalArgumentException("The file extention type is not supported by the converter!" + filePath.toString(), ex);
        }
        this.originalFileNameNoEXTPreferred = preferredTitleFormat(this.originalFileNameNoEXT);
        this.opusFileName = preferredTitleFormat(this.originalFileNameNoEXTPreferred + ".opus");
        this.opusFilePath = newPath(this.outputFolder, this.opusFileName);
        //Define Metadata Before Metadata Generation
        this.metadata = new ConverterMetadataBuilder(Objects.requireNonNull(metadata));
        //Artist/Title Cannot be Null
        if (isDefaultMetadata(MetadataType.TITLE) || isDefaultMetadata(MetadataType.ARTIST)) {
            //Attempt to Generate Metadata
            if (!generateMetadata()) {
                throw new IllegalArgumentException("Artist/Title cannot be null in passed metadata");
            }
        }
        //If The Default metadata is Present, Select a Random Default Image
        if (isDefaultMetadata(MetadataType.ALBUM_ART)) {
            this.metadata.albumArtPath(StellarDiskManager.getGenericPicture());
        }
    }

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path and manually specified output folder.If the converted file already
     * exists, it is overwritten.
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
     * path and manually specified output folder. If the converted file already
     * exists, it is overwritten. Uses the metadata specified.
     *
     * @param filePath The file for conversion
     * @param outputFolder The path to put the finished file in
     * @param metadata The metadata to use with this converter
     * @throws java.io.IOException If something happened
     */
    public StellarOPUSConverter(Path filePath, Path outputFolder, ConverterMetadata metadata) throws IOException {
        this(filePath, outputFolder, StellarLoggingFormatter.forClass(StellarOPUSConverter.class), metadata);
    }

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path and manually specified output folder. If the converted file already
     * exists, it is overwritten.
     *
     * @param filePath The file for conversion
     * @param outputFolder The path to put the finished file in
     * @throws java.io.IOException If something happened
     */
    public StellarOPUSConverter(Path filePath, Path outputFolder) throws IOException {
        this(filePath, outputFolder, StellarLoggingFormatter.forClass(StellarOPUSConverter.class));
    }

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path and metadata.
     *
     * @param filePath The file for conversion
     * @param metadata The metadata to use with this converter
     * @throws java.io.IOException If something happened
     */
    public StellarOPUSConverter(Path filePath, ConverterMetadata metadata) throws IOException {
        this(filePath, StellarDiskManager.getOutputFolder(), StellarLoggingFormatter.forClass(StellarOPUSConverter.class), metadata);
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
     * Helper method for automating comparisons against default metadata fields.
     * Specify the field for our metadata, and we'll compare it against the
     * default metadata field.
     *
     * @param type The type to compare
     * @param metadata The metadata to compare against default
     * @return Whether or not they are equal
     */
    private boolean isDefaultMetadata(MetadataType type) {
        return isDefaultMetadata(type, getMetadata());
    }

    /**
     * Checks if the metadata has been modified from the default settings.
     *
     * @return Whether or not this is true
     */
    private boolean metadataModified() {
        return !this.metadata.buildMetadata().equals(getDefaultMetadata());
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
        this.metadata.artist(getDefaultMetadata().getArtist());
        this.metadata.title(this.originalFileNameNoEXT);
        return convertToOPUS(this.metadata.getArtist(), this.metadata.getTitle());
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
            return convertToOPUS(this.metadata.getArtist(), this.metadata.getTitle());
        }
        return Optional.of(toOpusFile(bitrate));
    }

    /**
     * Converts the selected file to .OPUS. Makes a best effort to get the title
     * name and artist name from the filename. Works if the filename looks like
     * MyTitle -- MyTrack.mp4. If the metadata has been set, this subroutine
     * defaults to the previously set metadata. Has a default bitrate of 320K.
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
        String title = this.metadata.getTitle() + ".flac";
        //Fast FLAC Audio ripped from video
        //ffmpeg -i "video.m2ts" -vn -sn -acodec flac -compression_level 12 "audio.flac"
        processOP(true, "ffmpeg", "-i", this.originalFilePath.getFileName().toString(), "-ss", start.toString(),
                "-t", end.toString(), "-y", "-vn", "-sn", "-acodec", "flac",
                "-compression_level", "0", title);
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
        String title = this.metadata.getTitle() + ".flac";
        //Fast FLAC Audio ripped from video
        //ffmpeg -i "video.m2ts" -vn -sn -acodec flac -compression_level 12 "audio.flac"
        processOP(true, "ffmpeg", "-i", this.originalFilePath.getFileName().toString(), "-y", "-vn", "-sn", "-acodec", "flac",
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
        //Check the Date Field
        if (isDefaultMetadata(MetadataType.DATE)) {
            this.metadata.date(LocalDate.now());
        }
        //Set Created By
        this.metadata.createdBy(CREATED_BY_TAG + "=" + Main.FULL_PROGRAM_NAME);
        //Set Bitrate if Not Already Set
        this.metadata.bitrate(isDefaultMetadata(MetadataType.BITRATE) ? bitrate : this.metadata.getBitrate());
        //Build Metadata
        ConverterMetadata metadata = this.metadata.buildMetadata();
        processOP(true, "opusenc", flacFile.getFileName().toString(), title,
                "--bitrate", bitrate + "k",
                "--title", metadata.getTitle(),
                "--artist", metadata.getArtist(),
                "--picture", metadata.getAlbumArtPath().toAbsolutePath().toString(),
                "--comment", MetadataType.DATE.toString() + "=" + metadata.getDate().format(DATE_FORMATTER),
                "--comment", metadata.getCreatedBy()
        );
        //If we have metadata title, return that as the filename
        String fileTitle = this.metadata.getTitle().equalsIgnoreCase(getDefaultMetadata().getTitle())
                ? this.opusFileName : this.metadata.getTitle() + ".opus";
        //Copy Back from temp folder
        Files.copy(newPath(StellarDiskManager.getTempDirectory(), this.metadata.getTitle() + ".opus"),
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
        return Optional.of(newPath(StellarDiskManager.getOutputFolder(), title + ".opus"));
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
        this.metadata.addAll(StellarUI.askUserForArtistTitle("Start: " + start + "\nEnd: " + end, ""));
        return convertToOPUS(this.metadata.getArtist(), this.metadata.getTitle(), start, end);
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
    public Optional<Path> convertToOPUS(int bitrate, String artist, String title, StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end)
            throws IOException {
        requireNonNullIterated(artist, title, start, end);
        if (artist.isEmpty() || title.isEmpty()) {
            throw new IllegalArgumentException("One of the fields is empty!");
        }
        if (start.compareTo(end) >= 0) {
            throw new IllegalArgumentException("Start is less than or equal to end!");
        }
        //Were we a part newPath the constructor chain? If not, set metadata
        if (this.metadata.buildMetadata().equals(getDefaultMetadata())) {
            this.metadata.artist(artist);
            this.metadata.title(title);
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
        toOpusFile(bitrate, start, end);
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
            this.metadata.addAll(listToMap(getListFromRegex("[" + dividers.stream().filter(separator
                    -> this.originalFileNameNoEXT.matches(".*[" + separator + "+].*")).findFirst().get() + "*]", this.originalFilePath).stream()
                    .map(string -> preferredTitleFormat(string)).collect(Collectors.toList())));
        } else {
            this.metadata.addAll(StellarUI.askUserForArtistTitle(this.originalFileNameNoEXT));
        }
    }

    /**
     * Gets the title and author from the file name
     *
     * @param map The map to fill with metadata
     * @return Returns true if the metadata generation was successful
     */
    private boolean generateMetadata() {
        generateMetadataFromRegex("-", "|", "/");
        this.opusFileName = preferredTitleFormat(this.metadata.getTitle() + ".opus");
        this.opusFilePath = newPath(this.outputFolder, this.opusFileName);
        //Only Return True if Both Are Not Default
        return !getDefaultMetadata().equals(this.metadata.buildMetadata());
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
        copyOP(operation, preferredTitleFormat(this.metadata.getTitle()) + ".opus");
    }

    /**
     * Gets the filename format for the image conversion process. The metadata
     * must be set before calling this method.
     *
     * @return The filename with .png extension
     */
    private String getImageFileName() {
        return preferredTitleFormat(this.metadata.getArtist()) + " -- "
                + preferredTitleFormat(this.metadata.getTitle()) + ".png";
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
        this.metadata.albumArtPath(imageFilePath);
    }

    /**
     * Gets the original file path.
     *
     * @return The original file path
     */
    public Path getOriginalFilePath() {
        return this.originalFilePath;
    }

    /**
     * Gets the output folder file path.
     *
     * @return The output folder file path
     */
    public Path getOutputFolder() {
        return this.outputFolder;
    }

    /**
     * Gets the metadata. Note that the metadata is only set after
     * {@link StellarOPUSConverter#convertToOPUS()} has been called.
     *
     * @return The metadata in read-only format
     */
    public ConverterMetadata getMetadata() {
        return this.metadata.buildMetadata();
    }

    /**
     * Gets the original file extension.
     *
     * @return The file extension of the original file
     */
    public FileExtension getFileExtension() {
        return this.fileExtension;
    }

    /**
     * Gets the expected .opus file path if before conversion, and the actual
     * file path after conversion. May not actually exist on the disk until
     * after calling one of the convert methods.
     *
     * @return The file path
     */
    public Path getOpusFilePath() {
        return this.opusFilePath;
    }

    /**
     * Gets the name of the .opus file.
     *
     * @return The name of the .opus file
     */
    public String getOpusFileName() {
        return this.opusFileName;
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
         * The bitrate of this track.
         */
        BITRATE,
        /**
         * The title of the track.
         */
        TITLE,
        /**
         * The album art of the track.
         */
        ALBUM_ART,
        /**
         * The date of creation on the track.
         */
        DATE {
            @Override
            public String toString() {
                return "Stellar Index Date";
            }
        },
        /**
         * The comment on a .opus file track.
         */
        CREATED_BY {
            @Override
            public String toString() {
                return "Created By";
            }

        };
    }

    /**
     * A class for building a {@link ConverterMetadata} object.
     */
    public static final class ConverterMetadataBuilder {

        private String artist, title, createdBy;
        private LocalDate date;
        private Path albumArtPath;
        private int bitrate;

        /**
         * Constructs a new builder with the default values.
         */
        public ConverterMetadataBuilder() {
            this(ConverterMetadata.DEFAULT_METADATA);
        }

        /**
         * Copy constructor for taking an already existing metadata object and
         * copying it into the builder.
         *
         * @param metadata The metadata object
         */
        public ConverterMetadataBuilder(ConverterMetadata metadata) {
            this.artist = preferredTitleFormat(Objects.requireNonNull(metadata.getArtist()));
            this.title = preferredTitleFormat(Objects.requireNonNull(metadata.getTitle()));
            this.createdBy = preferredTitleFormat(Objects.requireNonNull(metadata.getCreatedBy()));
            this.date = Objects.requireNonNull(metadata.getDate());
            this.albumArtPath = Objects.requireNonNull(metadata.getAlbumArtPath());
            this.bitrate = metadata.getBitrate();
        }

        /**
         * Copy constructor with a map as an input type.
         *
         * @param map The map of metadata
         */
        public ConverterMetadataBuilder(Map<MetadataType, String> map) {
            this.artist = Objects.requireNonNull(map.get(MetadataType.ARTIST));
            this.title = Objects.requireNonNull(map.get(MetadataType.TITLE));
            if (map.get(MetadataType.CREATED_BY) != null) {
                this.createdBy = map.get(MetadataType.CREATED_BY);
            }
            if (map.get(MetadataType.DATE) != null) {
                this.date = LocalDate.parse(map.get(MetadataType.DATE), DATE_FORMATTER);
            }
            if (map.get(MetadataType.ALBUM_ART) != null) {
                this.albumArtPath = newPath(map.get(MetadataType.ALBUM_ART));
            }
        }

        /**
         * Clears this builder, setting it to the default values.
         *
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder clear() {
            this.artist = ConverterMetadata.DEFAULT_METADATA.getArtist();
            this.title = ConverterMetadata.DEFAULT_METADATA.getTitle();
            this.createdBy = ConverterMetadata.DEFAULT_METADATA.getCreatedBy();
            this.date = ConverterMetadata.DEFAULT_METADATA.getDate();
            this.albumArtPath = ConverterMetadata.DEFAULT_METADATA.getAlbumArtPath();
            return this;
        }

        /**
         * Adds all the metadata with a map as an argument.
         *
         * @param metadata The map of metadata
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder addAll(ConverterMetadata metadata) {
            this.artist = preferredTitleFormat(Objects.requireNonNull(metadata.getArtist()));
            this.title = preferredTitleFormat(Objects.requireNonNull(metadata.getTitle()));
            if (metadata.getCreatedBy() != null) {
                this.createdBy = preferredTitleFormat(metadata.getCreatedBy());
            }
            if (metadata.getDate() != null) {
                this.date = metadata.getDate();
            }
            if (metadata.getAlbumArtPath() != null) {
                this.albumArtPath = metadata.getAlbumArtPath();
            }
            this.bitrate = metadata.getBitrate();
            return this;
        }

        /**
         * Adds all the metadata with a map as an argument.
         *
         * @param map The map of metadata
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder addAll(Map<MetadataType, String> map) {
            this.artist = preferredTitleFormat(Objects.requireNonNull(map.get(MetadataType.ARTIST)));
            this.title = preferredTitleFormat(Objects.requireNonNull(map.get(MetadataType.TITLE)));
            if (map.get(MetadataType.CREATED_BY) != null) {
                this.createdBy = preferredTitleFormat(map.get(MetadataType.CREATED_BY));
            }
            if (map.get(MetadataType.DATE) != null) {
                this.date = LocalDate.parse(map.get(MetadataType.DATE), DATE_FORMATTER);
            }
            if (map.get(MetadataType.ALBUM_ART) != null) {
                this.albumArtPath = newPath(map.get(MetadataType.ALBUM_ART));
            }
            return this;
        }

        /**
         * Builds a new metadata file with the previously specified fields.
         *
         * @return The metadata
         */
        public ConverterMetadata buildMetadata() {
            return new ConverterMetadata(this.artist, this.title, this.createdBy, this.date, this.albumArtPath, this.bitrate);
        }

        /**
         * Sets this metadata field.
         *
         * @param bitrate The bitrate of this file
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder bitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        /**
         * Gets the bitrate of this track
         *
         * @return The bitrate
         */
        public int getBitrate() {
            return this.bitrate;
        }

        /**
         * Sets this metadata field.
         *
         * @param artist The metadata field to set
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder artist(String artist) {
            this.artist = preferredTitleFormat(Objects.requireNonNull(artist));
            return this;
        }

        /**
         * Sets this metadata field.
         *
         * @param title The metadata field to set
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder title(String title) {
            this.title = preferredTitleFormat(Objects.requireNonNull(title));
            return this;
        }

        /**
         * Sets this metadata field.
         *
         * @param createdBy The metadata field to set
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder createdBy(String createdBy) {
            this.createdBy = preferredTitleFormat(Objects.requireNonNull(createdBy));
            return this;
        }

        /**
         * Sets this metadata field.
         *
         * @param date The metadata field to set
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * Sets this metadata field.
         *
         * @param albumArtPath The metadata field to set
         * @return This builder, per <i>the builder pattern</i>
         */
        public ConverterMetadataBuilder albumArtPath(Path albumArtPath) {
            this.albumArtPath = albumArtPath;
            return this;
        }

        /**
         * The getter for this field
         *
         * @return The field
         */
        public String getArtist() {
            return this.artist;
        }

        /**
         * The getter for this field
         *
         * @return The field
         */
        public String getTitle() {
            return this.title;
        }

        /**
         * The getter for this field
         *
         * @return The field
         */
        public String getCreatedBy() {
            return this.createdBy;
        }

        /**
         * The getter for this field
         *
         * @return The field
         */
        public LocalDate getDate() {
            return this.date;
        }

        /**
         * The getter for this field
         *
         * @return The field
         */
        public Path getAlbumArtPath() {
            return this.albumArtPath;
        }

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
