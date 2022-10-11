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
package com.protonmail.sarahszabo.stellar.metadata;

import com.protonmail.sarahszabo.stellar.StellarDiskManager;
import com.protonmail.sarahszabo.stellar.conversions.converters.StellarOPUSConverter;
import com.protonmail.sarahszabo.stellar.util.StellarGravitonField;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * A class for building a {@link ConverterMetadata} object.
 */
public class ConverterMetadataBuilder {

    private String artist;
    private String title;
    private String createdBy;
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
        this.artist = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(metadata.getArtist()));
        this.title = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(metadata.getTitle()));
        this.createdBy = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(metadata.getCreatedBy()));
        this.date = Objects.requireNonNull(metadata.getStellarIndexDate());
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
            this.date = LocalDate.parse(map.get(MetadataType.DATE), StellarOPUSConverter.DATE_FORMATTER);
        }
        if (map.get(MetadataType.ALBUM_ART) != null) {
            this.albumArtPath = StellarGravitonField.newPath(map.get(MetadataType.ALBUM_ART));
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
        this.date = ConverterMetadata.DEFAULT_METADATA.getStellarIndexDate();
        this.albumArtPath = ConverterMetadata.DEFAULT_METADATA.getAlbumArtPath();
        return this;
    }

    /**
     * Adds all the metadata with a converter metadata as an argument.
     *
     * @param metadata The map of metadata
     * @return This builder, per <i>the builder pattern</i>
     */
    public ConverterMetadataBuilder addAll(ConverterMetadata metadata) {
        this.artist = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(metadata.getArtist()));
        this.title = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(metadata.getTitle()));
        if (metadata.getCreatedBy() != null && !ConverterMetadata.isDefaultMetadata(MetadataType.CREATED_BY, metadata)) {
            this.createdBy = StellarGravitonField.preferredTitleFormat(metadata.getCreatedBy());
        }
        if (metadata.getStellarIndexDate() != null && !ConverterMetadata.isDefaultMetadata(MetadataType.DATE, metadata)) {
            this.date = metadata.getStellarIndexDate();
        }
        if (metadata.getAlbumArtPath() != null && !ConverterMetadata.isDefaultMetadata(MetadataType.ALBUM_ART, metadata)) {
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
        this.artist = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(map.get(MetadataType.ARTIST)));
        this.title = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(map.get(MetadataType.TITLE)));
        if (map.get(MetadataType.CREATED_BY) != null) {
            this.createdBy = StellarGravitonField.preferredTitleFormat(map.get(MetadataType.CREATED_BY));
        }
        if (map.get(MetadataType.DATE) != null) {
            this.date = LocalDate.parse(map.get(MetadataType.DATE), StellarOPUSConverter.DATE_FORMATTER);
        }
        if (map.get(MetadataType.ALBUM_ART) != null) {
            this.albumArtPath = StellarGravitonField.newPath(map.get(MetadataType.ALBUM_ART));
        }
        return this;
    }

    /**
     * Builds a new metadata file with the previously specified fields.
     * Artist/Title fields are set to proper formats. If the album art is not
     * set, it will be randomly generated from a pre-selected stock of generic
     * pictures.
     *
     * @return The built metadata
     */
    public ConverterMetadata buildMetadata() {
        return new ConverterMetadata(StellarGravitonField.preferredTitleFormat(this.artist),
                StellarGravitonField.preferredTitleFormat(this.title), StellarGravitonField.preferredTitleFormat(this.createdBy),
                this.date, this.albumArtPath.equals(ConverterMetadata.DEFAULT_METADATA.getAlbumArtPath())
                ? StellarDiskManager.getGenericPicture() : this.albumArtPath, this.bitrate);
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
     * Compares whether or not the metadata when built is equal to
     * {@link ConverterMetadata#DEFAULT_METADATA}.
     *
     * @return The boolean value
     */
    public boolean isDefaultMetadata() {
        return this.buildMetadata().equals(ConverterMetadata.DEFAULT_METADATA);
    }

    /**
     * Sets this metadata field.
     *
     * @param artist The metadata field to set
     * @return This builder, per <i>the builder pattern</i>
     */
    public ConverterMetadataBuilder artist(String artist) {
        this.artist = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(artist));
        return this;
    }

    /**
     * Sets this metadata field.
     *
     * @param title The metadata field to set
     * @return This builder, per <i>the builder pattern</i>
     */
    public ConverterMetadataBuilder title(String title) {
        this.title = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(title));
        return this;
    }

    /**
     * Sets this metadata field.
     *
     * @param createdBy The metadata field to set
     * @return This builder, per <i>the builder pattern</i>
     */
    public ConverterMetadataBuilder createdBy(String createdBy) {
        this.createdBy = StellarGravitonField.preferredTitleFormat(Objects.requireNonNull(createdBy));
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
