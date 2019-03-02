/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar.conversions.converters;

import com.protonmail.sarahszabo.stellar.Main;
import com.protonmail.sarahszabo.stellar.StellarDiskManager;
import com.protonmail.sarahszabo.stellar.conversions.StellarFFMPEGTimeStamp;
import com.protonmail.sarahszabo.stellar.metadata.ConverterMetadata;
import com.protonmail.sarahszabo.stellar.metadata.ConverterMetadataBuilder;
import com.protonmail.sarahszabo.stellar.metadata.MetadataType;
import com.protonmail.sarahszabo.stellar.util.FileExtension;
import com.protonmail.sarahszabo.stellar.util.StellarCLIUtils;
import com.protonmail.sarahszabo.stellar.util.StellarGravitonField;
import static com.protonmail.sarahszabo.stellar.util.StellarGravitonField.*;
import com.protonmail.sarahszabo.stellar.util.StellarGreatFilter;
import com.protonmail.sarahszabo.stellar.util.StellarLoggingFormatter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
     * Gets the filename format for the image conversion process. The metadata
     * must be set before calling this method.
     *
     * @param metadata The metadata list to generate the filename from
     * @return The filename with .png extension
     */
    public static String getImageFileName(Map<MetadataType, String> metadata) {
        return preferredTitleFormat(metadata.get(MetadataType.ARTIST)) + " -- "
                + preferredTitleFormat(metadata.get(MetadataType.TITLE)) + ".png";
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
     * Applies the file name filters to the string and returns a nicely /*
     * formatted string. Removes unwanted information, such as (HQ 320K) from
     * the filename.
     *
     * @param str The string filename
     * @return The nicely formatted filename
     */
    private static String applyFileNameFilters(String str) {
        return str.replaceFirst("\\(HQ.*\\)", "").trim();
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
        ConverterMetadataBuilder builder = new ConverterMetadataBuilder(ConverterMetadata.getDefaultMetadata());
        //Apply Filter to Remove (HQ 192K) or some other such annoyance from filename
        String originalFileNameNoEXT = applyFileNameFilters(stripFileExtension(filePath));
        if (dividers.stream().anyMatch(separator -> originalFileNameNoEXT.matches(".*[" + separator + "]+.*"))) {
            //Find the Divider that Matches
            for (var separator : dividers) {
                if (originalFileNameNoEXT.matches(".*[" + separator + "]+.*")) {
                    //We've found the seperator that matches
                    var split = originalFileNameNoEXT.split(separator);
                    builder.artist(split[0].trim());
                    builder.title(split[1].trim());
                    break;
                }
            }
            //Maps to proper metadata format

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
     * Generates the title and artist from the filename format, or else asks the
     * user for the information.
     */
    private void generateTitleArtist() {
        //Artist/Title Cannot be Null
        if (isDefaultMetadata(MetadataType.ARTIST) || isDefaultMetadata(MetadataType.TITLE)) {
            if (!generateMetadata()) {
                this.metadata.addAll(StellarCLIUtils.askUserForArtistTitle(applyFileNameFilters(this.originalFileNameNoEXTPreferred)));
            }
        }
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
        return ConverterMetadata.isDefaultMetadata(type, getMetadata());
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
        this.metadata.artist(ConverterMetadata.getDefaultMetadata().getArtist());
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
        //Or If this matches the ARTIST -- FILENAME pattern, ignore artist/title tags then proceed.
        if ((isDefaultMetadata(MetadataType.TITLE) && isDefaultMetadata(MetadataType.ARTIST))
                || this.originalFileNameNoEXT.matches(".*/s*-/s*.*")) {
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
                "-to", end.toString(), "-y", "-vn", "-sn", "-acodec", "flac",
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
        //Check Album Art
        if (isDefaultMetadata(MetadataType.ALBUM_ART)) {
            this.metadata.albumArtPath(StellarDiskManager.getGenericPicture());
        }//Set Created By
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
                "--comment", MetadataType.DATE.toString() + "=" + metadata.getStellarIndexDate().format(DATE_FORMATTER),
                "--comment", metadata.getCreatedBy()
        );
        //If we have metadata title, return that as the filename
        String fileTitle = this.metadata.getTitle().equalsIgnoreCase(ConverterMetadata.getDefaultMetadata().getTitle())
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
            } catch (IOException ex) {
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
        this.metadata.addAll(StellarCLIUtils.askUserForArtistTitle("Filename: " + this.originalFilePath.getFileName()
                + "\nStart: " + start + "\nEnd: " + end, ""));
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
        if (this.metadata.buildMetadata().equals(ConverterMetadata.getDefaultMetadata())) {
            this.metadata.artist(artist);
            this.metadata.title(title);
        }
        //ffmpeg -ss 00:00:24.0 -i 40.mp4 -t 00:01:20.0 -y -b:a 320k 40.opus
        copyOP(() -> {
            try {
                processImage();
                toOpusFile(bitrate, start, end);
            } catch (IOException ex) {
                Logger.getLogger(StellarOPUSConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        toOpusFile(bitrate, start, end);
        return Optional.of(newPath(StellarDiskManager.getOutputFolder(), title + ".opus"));
    }

    /**
     * Gets the title and author from the file name
     *
     * @param map The map to fill with metadata
     * @return Returns true if the metadata generation was successful
     */
    private boolean generateMetadata() {
        ConverterMetadata metadata = generateMetadataFromRegex(this.originalFilePath, "-", "|", "/");
        this.metadata.artist(metadata.getArtist());
        this.metadata.title(metadata.getTitle());
        this.opusFileName = preferredTitleFormat(this.metadata.getTitle() + ".opus");
        this.opusFilePath = this.outputFolder.resolve(this.opusFileName);
        //Only Return True if Both Are Not Default
        return !ConverterMetadata.getDefaultMetadata().equals(this.metadata.buildMetadata());
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
     * to the working directory and attaches the image to the .opus file. Adds
     * the album art path to metadata.
     *
     * @throws IOException If something went wrong
     */
    private void processImage() throws IOException {
        //If we're pointing at a video file, get it's image at 25s
        if (FileExtension.isVideo(this.fileExtension)) {
            Path imageFilePath = StellarDiskManager.getPictureOutputFolder().resolve(getImageFileName());

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
            this.metadata.albumArtPath(imageFilePath);
        }
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

}
