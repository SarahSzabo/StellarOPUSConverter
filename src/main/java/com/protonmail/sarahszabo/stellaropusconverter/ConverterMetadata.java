/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import com.protonmail.sarahszabo.stellaropusconverter.util.StellarGravitonField;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A representation of .opus metadata. Used in concordance with a
 * {@link StellarOPUSConverter}. All fields are immutable.
 */
public final class ConverterMetadata {

    /**
     * The default metadata instance.
     */
    static final ConverterMetadata DEFAULT_METADATA = new ConverterMetadata("Unknown Artist",
            "Unknown Title", Main.FULL_PROGRAM_NAME, LocalDate.MAX, StellarGravitonField.newPath(""), Integer.MAX_VALUE);
    private final String artist;
    private final String title;
    private final String createdBy;
    private final LocalDate date;
    private final Path albumArtPath;
    private final int bitrate;

    /**
     * Constructs a new {@link ConverterMetadata} with the specified arguments.
     *
     *
     * @param artist The artist for this track
     * @param title The title of this track
     * @param createdBy The program that created this track/last modified this
     * track
     * @param date The date this track was created
     * @param albumArtPath The path to the album art
     */
    public ConverterMetadata(String artist, String title, String createdBy, LocalDate date, Path albumArtPath, int bitrate) {
        this.artist = Objects.requireNonNull(artist);
        this.title = Objects.requireNonNull(title);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.date = Objects.requireNonNull(date);
        this.albumArtPath = Objects.requireNonNull(albumArtPath);
        if (artist.isEmpty() || title.isEmpty()) {
            throw new IllegalArgumentException("Artist or title is empty");
        } else if (createdBy.isEmpty()) {
            throw new IllegalArgumentException("Created By is empty");
        }
        this.bitrate = bitrate;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.artist);
        hash = 67 * hash + Objects.hashCode(this.title);
        hash = 67 * hash + this.bitrate;
        return hash;
    }

    /**
     * {@link ConverterMetadata} objects are equal if and only if their artists
     * and titles are the same.
     *
     * @param obj The other metadata object
     * @return Whether they are equal or not
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConverterMetadata other = (ConverterMetadata) obj;
        if (this.bitrate != other.bitrate) {
            return false;
        }
        if (!Objects.equals(this.artist, other.artist)) {
            return false;
        }
        if (!Objects.equals(this.title, other.title)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the artist metadata field.
     *
     * @return The artist metadata field
     */
    public String getArtist() {
        return this.artist;
    }

    /**
     * Gets the title metadata field.
     *
     * @return The title metadata field
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Gets the created by metadata field.
     *
     * @return The created by metadata field
     */
    public String getCreatedBy() {
        return this.createdBy;
    }

    /**
     * Gets the date metadata field.
     *
     * @return The date metadata field
     */
    public LocalDate getDate() {
        return this.date;
    }

    /**
     * Gets the album art path metadata field.
     *
     * @return The album art path metadata field
     */
    public Path getAlbumArtPath() {
        return this.albumArtPath;
    }

    /**
     * Gets the bitrate associated with this file.
     *
     * @return The bitrate
     */
    public int getBitrate() {
        return this.bitrate;
    }

}
