/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public static List<String> getDefaultMetadataList() {
        List<String> metadata = new ArrayList<>(2);
        metadata.add("Unknown Artist");
        metadata.add("Unknown Title");
        return metadata;
    }

    private final String fileNameNoEXT, trueFileName;
    private final Path filePath;
    private final List<String> metadata;
    private final StellarDiskManager diskManager;

    /**
     * Constructs a new {@link StellarOPUSConverter} with the specified file
     * path.
     *
     * @param filePath The file for conversion
     * @throws java.io.IOException If something happened
     */
    public StellarOPUSConverter(Path filePath) throws IOException {
        this.filePath = filePath;
        this.diskManager = StellarDiskManager.DISKMANAGER;
        //Gets only the file name without extension
        System.out.println(filePath.toAbsolutePath());
        this.fileNameNoEXT = filePath.getFileName().toString().replaceFirst("[.][^.]+$", "");
        this.trueFileName = this.fileNameNoEXT + ".opus";
        this.metadata = new ArrayList<>(2);
    }

    /**
     * Turns the given field into the proper format, which is the beginning of
     * each character is capitalised.
     *
     * @param field The field to capitalise
     * @return The formatted string
     */
    private String properMetadataFormat(String field) {
        //Uppercase First Character of Both Titles
        StringBuilder builder = new StringBuilder(field.length());
        String[] spacedTitleCharacters = field.split(" ");
        for (String str : spacedTitleCharacters) {
            builder.append(String.valueOf(str.charAt(0)).toUpperCase()).append(str.substring(1)).append(" ");
        }
        return builder.toString();
    }

    /**
     * Converts the selected file to .OPUS. Makes a best effort to get the title
     * name and artist name from the filename. Works if the filename looks like
     * MyTitle -- MyTrack.mp4.
     *
     * @return The file path of the .opus file.
     * @throws IOException If something went wrong
     */
    public Path convertToOPUS() throws IOException {
        generateMetadata();
        return convertToOPUS(this.metadata.get(0), this.metadata.get(1));
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
        //If Metadata Not Already Generated, Set it
        if (this.metadata.isEmpty()) {
            this.metadata.add(properMetadataFormat(artist));
            this.metadata.add(properMetadataFormat(title));
        }
        copyOP(() -> {
            try {
                processImage();
                processOP("ffmpeg", "-i", this.filePath.getFileName().toString(),
                        "-y", "-b:a", "320k", "-metadata", "title=" + title,
                        "-metadata", "artist=" + artist, "-strict", "-2", properMetadataFormat(title) + ".opus");
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(StellarOPUSConverter.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Error encountered during conversion");
            }
        });
        //Clear Metadata to Restore Default State for next use
        this.metadata.clear();
        return Paths.get(this.diskManager.getOutputFolder().toString(), title + ".opus");
    }

    /**
     * An iterated version of {@link Objects#requireNonNull(java.lang.Object)}
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
     * @param start The start time of the recording window
     * @param end The end of the recording window
     * @return The path to the newly created file
     * @throws java.io.IOException If something happened
     */
    public Path convertToOPUS(StellarFFMPEGTimeStamp start, StellarFFMPEGTimeStamp end) throws IOException {
        //Convert to Proper Format
        this.metadata.addAll(StellarUI.askUserForArtistTitle().stream().map(string -> properMetadataFormat(string))
                .collect(Collectors.toList()));
        return convertToOPUS(this.metadata.get(0), this.metadata.get(1), start, end);
    }

    /**
     * Converts the selected file to .OPUS. Time start and end are expected to
     * be of the form HH:MM:SS. So: 00:01:23 with nothing else in the string.
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
        //Were we a part of the constructor chain? If not, set metadata
        if (this.metadata.isEmpty()) {
            this.metadata.add(artist);
            this.metadata.add(title);
        }
        //ffmpeg -ss 00:00:24.0 -i 40.mp4 -t 00:01:20.0 -y -b:a 320k 40.opus
        copyOP(() -> {
            try {
                processImage();
                processOP("ffmpeg", "-i", this.filePath.getFileName().toString(),
                        "-ss", start.getTimestamp(), "-to", end.getTimestamp(), "-y", "-b:a", "320k", "-metadata", "title=" + title,
                        "-metadata", "artist=" + artist, "-strict", "-2", properMetadataFormat(title) + ".opus");
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(StellarOPUSConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        //Clear Metadata to Restore to Default State
        this.metadata.clear();
        return Paths.get(this.diskManager.getOutputFolder().toString(), title + ".opus");
    }

    /**
     * A utility method for getting metadata from the targetted filename.
     * Specify the regex for the separator. Filename usually take on the form of
     * SONGAUTHOR -- SONGTITLE. Separators usually look like "[-+]".
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
     * Tests to see if the format is matched. Specify the regex for the
     * separator. Filename usually take on the form of SONGAUTHOR -- SONGTITLE.
     * Separators usually look like "+" or "|". Returns a list that, by
     * convention has zeroith element as the artist, and second element as the
     * title.
     *
     * @param separators The regular expressions to use
     * @return The metadata list
     */
    private void generateMetadataFromRegex(String... separators) {
        List<String> dividers = Arrays.asList(separators);
        if (dividers.stream().anyMatch(separator -> this.fileNameNoEXT.matches(".*[" + separator + "]+.*"))) {
            //Maps to proper metadata format
            this.metadata.addAll(getListFromRegex("[" + dividers.stream().filter(separator
                    -> this.fileNameNoEXT.matches(".*[" + separator + "+].*")).findFirst().get() + "*]").stream()
                    .map(string -> properMetadataFormat(string)).collect(Collectors.toList()));
        } else {
            this.metadata.addAll(StellarUI.askUserForArtistTitle(this.fileNameNoEXT));
        }
    }

    /**
     * Gets the title and author from the file name. By convention, the zeroith
     * element is the artist, and the first is the title track.
     *
     * @return The list of information, or empty if none was available
     */
    private void generateMetadata() {
        generateMetadataFromRegex("-");
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param commands The commands to execute
     */
    private void processOP(String... commands) throws InterruptedException, IOException {
        System.out.println("About to Initiate (Temp Folder Path): " + this.diskManager.getTempDirectory());
        //Print out FFMPEG Command
        String s = "";
        for (String st : new ProcessBuilder(commands)
                .directory(this.diskManager.getTempDirectory().toFile()).inheritIO().command()) {
            s += " " + st;
        }//"/home/sarah/Sarah Szabo -- The Fourtyth Divide.mp4"
        System.out.println("COMMAND: " + s);
        //Actually do it
        Process proc = new ProcessBuilder(commands)
                .directory(this.diskManager.getTempDirectory().toFile()).inheritIO().start();
        proc.waitFor();
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param command The command to execute
     */
    private void processOP(String command) throws InterruptedException, IOException {
        Process proc = new ProcessBuilder(command)
                .directory(this.diskManager.getTempDirectory().toFile()).inheritIO().start();
        proc.waitFor();
    }

    /**
     * Performs an operation after first copying the target file over to the
     * temp directory.
     *
     * @param operation The operations to perform
     * @throws IOException If something happened
     */
    private void copyOP(CopyOperation operation) throws IOException {
        this.diskManager.copyToTemp(this.filePath);
        operation.doOperation();
        this.diskManager.copyFromTemp(this.metadata.get(1) + ".opus");
    }

    /**
     * Gets the image from the video, only called after the video has been moved
     * to the working directory.
     *
     * @throws IOException If something went wrong
     */
    private void processImage() throws IOException, InterruptedException {
        synchronized (StellarOPUSConverter.class) {
            //Check if Image Already Exists
            if (Files.exists(Paths.get(this.diskManager.getOutputFolder().toString(), fileNameNoEXT + ".png"))) {
                return;
            }
            //ffmpeg -ss 25 -i input.mp4 -qscale:v 2 -frames:v 1 -huffman optimal output.jpg
            processOP("ffmpeg", "-ss", "30", "-i", filePath.getFileName().toString(), "-y", "-qscale:v", "2",
                    "-frames:v", "1", "-huffman", "optimal", fileNameNoEXT + ".png");
            //Remove This Once This Becomes Automated
            Files.copy(Paths.get(this.diskManager.getTempDirectory().toString(), fileNameNoEXT + ".png"),
                    Paths.get(this.diskManager.getOutputFolder().toString(), fileNameNoEXT + ".png"));
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
}
