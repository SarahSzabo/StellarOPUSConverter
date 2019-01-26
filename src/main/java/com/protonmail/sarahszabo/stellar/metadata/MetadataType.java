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

/**
 * An enum representing .opus metadata information
 */
public enum MetadataType {
    /**
     * The artist of the track.
     */
    ARTIST, /**
     * The bitrate of this track.
     */ BITRATE, /**
     * The title of the track.
     */ TITLE, /**
     * The album art of the track.
     */ ALBUM_ART, /**
     * The date of creation on the track.
     */ DATE {
        @Override
        public String toString() {
            return "Stellar Index Date";
        }
    }, /**
     * The comment on a .opus file track.
     */ CREATED_BY {
        @Override
        public String toString() {
            return "Created By";
        }
    }

}
