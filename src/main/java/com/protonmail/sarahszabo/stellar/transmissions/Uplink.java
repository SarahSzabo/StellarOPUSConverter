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
package com.protonmail.sarahszabo.stellar.transmissions;

import java.io.IOException;
import java.nio.file.Path;

/**
 * An interface specifying that this class can recieve a new data point for
 * conversion.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public interface Uplink {

    /**
     * Gets a file from the source and saves it to the disk.Depending on the
     * strategy used, this may buffer for a while. All implementations of this
     * method block until they finish if they don't return instantly.
     *
     * @return Whether or not the operation was successful
     * @throws java.io.IOException If something went wrong
     */
    boolean recieveTransmission() throws IOException;

    /**
     * Gets the progress of the download in percent if applicable. Returns 100
     * if complete and 0 if just started.
     *
     * @return The progress
     * @throws java.io.IOException If something went wrong
     */
    double getProgress() throws IOException;

    /**
     * Gets the estimated time of completion.
     *
     * @return The ETA
     * @throws IOException If something went wrong
     */
    String getEstimatedFinishTime() throws IOException;

    /**
     * Gets the completed file path, if available. Keep in mind that the file
     * may or may not exist.
     *
     * @return The optional with the path
     * @throws java.io.IOException If something went wrong
     */
    Path getPath() throws IOException;

    /**
     * Gets the title of the download.
     *
     * @return The title of the download
     * @throws java.io.IOException If something went wrong
     */
    String getTitle() throws IOException;
}
