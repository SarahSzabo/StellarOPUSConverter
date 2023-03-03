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

import com.protonmail.sarahszabo.stellar.util.StellarGravitonField;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A controller / builder for KID3 commands.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class KID3CommandBuilder {

    /**
     * The number of newline characters for a tag to be considered existant. If
     * you run select tag 1 + get, it must be greater than this number to be
     * considered existant. Else tag is non-existant.
     */
    public static final int TAG_EXISTS_NEWLINE_CHARACTER_NUMBER = 2;
    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(KID3CommandBuilder.class.getName());

    /**
     * Determines if a String IDV3 tag from KID3 is valid or not. DOes not work
     * for any other field.
     *
     * @param tag The tag retrived from a
     * {@link KID3CommandBuilder#buildAndExecuteString()} run with
     * {@link KID3CommandBuilder#get()}.
     * @return Whether or not a tag is valid or not
     */
    public static boolean isTagIDV3Valid(String tag) {
        return tag.contains("\n") ? StringUtils.countMatches(tag, '\n') > 2 : false;
    }
    /**
     * The list of commands to be concatentated.
     */
    private final List<String> commands = new ArrayList<>(10);

    /**
     * The file that will be modified.
     */
    private final Path inputFile;

    /**
     * The constructor for this builder.
     */
    public KID3CommandBuilder(Path inputFile) {
        //Set Tags: kid3-cli -c "tag 1" -c "set Title "TITLE"" -c "set Artist "ARTIST"" FILENAME.EXT
        this.inputFile = inputFile;
        this.commands.add("kid3-cli");
    }

    /**
     * Ensures that the tags are within range.
     *
     * @param tag The tag to be tested
     */
    private void checkTagDelimiters(int tag) {
        if (tag > 3) {
            throw new IllegalArgumentException("KID3 only supports tags up to 3");
        }
    }

    /**
     * Adds a get command.
     *
     * @return The builder
     */
    public KID3CommandBuilder get() {
        this.commands.add("-c");
        this.commands.add("get");
        return this;
    }

    /**
     * Selects the tag.
     *
     * @param tag
     * @return The builder
     */
    public KID3CommandBuilder selectTag(int tag) {
        checkTagDelimiters(tag);
        this.commands.add("-c");
        this.commands.add("tag " + tag);
        return this;
    }

    /**
     * Converts tag 2 to ID3 V2.4.
     *
     * @return The builder
     */
    public KID3CommandBuilder Tag2To2Point4() {
        selectTag(2);
        this.commands.add("to 24");
        return this;
    }

    /**
     * Selects the tag to be deleted.
     *
     * @param tag
     * @return The builder
     */
    public KID3CommandBuilder deleteTag(int tag) {
        checkTagDelimiters(tag);
        this.commands.add("-c");
        this.commands.add("remove " + tag);
        return this;
    }

    /**
     * Selects the tag to be copied and pasted.
     *
     * @param tag
     * @return The builder
     */
    public KID3CommandBuilder copyTag(int tagSource, int tagDestination) {
        checkTagDelimiters(tagSource);
        checkTagDelimiters(tagDestination);
        this.commands.add("-c");
        this.commands.add("copy " + tagSource);
        //Copy-Paste Operation per KID3 manual
        this.commands.add("-c");
        this.commands.add("paste " + tagDestination);
        return this;
    }

    /**
     * Sets the title of the track.
     *
     * @param title The title of the track
     * @return The builder
     */
    public KID3CommandBuilder setTitle(String title) {
        this.commands.add("-c");
        this.commands.add("set Title \'" + title + "\'");
        return this;
    }

    /**
     * Sets the title of the track.
     *
     * @param artist The artist of the track
     * @return The builder
     */
    public KID3CommandBuilder setArtist(String artist) {
        this.commands.add("-c");
        this.commands.add("set Artist \'" + artist + "\'");
        return this;
    }

    /**
     * Runs the string that would be generated from the builder being built in a
     * process and returns the result in the form of a String.
     *
     * @return The string result from the process after it finishes execution
     */
    public String buildAndExecuteString() {
        try {
            var list = build();
            var builder = StellarGravitonField.processOPBuilder(false, commands.toArray(new String[list.size()]));
            var process = builder.start();
            var string = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
            process.waitFor();
            return string;
        } catch (IOException ex) {
            throw new IllegalStateException("The automatic execution of the process was unsuccessful", ex);
        } catch (InterruptedException ex) {
            throw new IllegalStateException("The automatic execution of the process was unsuccessful", ex);
        }
    }

    /**
     * Builds the commands where each command can be inputted into a
     * {@link ProcessBuilder} for building.
     *
     * @return The list of commands for a {@link ProcessBuilder}
     */
    public List<String> build() {
        //Last step is the input file in the KID3 conversion command
        this.commands.add(this.inputFile.toString());
        LOG.fine("KID3 Command Built: " + buildString());
        return Collections.unmodifiableList(this.commands);
    }

    /**
     * Builds the commands as a string.
     *
     * @return The string
     */
    public String buildString() {
        return this.commands.stream().collect(Collectors.joining(" "));
    }
}
