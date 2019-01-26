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

import com.protonmail.sarahszabo.stellar.StellarDiskManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that is capable of downloading Youtube videos using YoutubeDL. Saves
 * them to the file specified in the constructor.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public final class YoutubeUplink implements Uplink {

    private final String url;
    private String title, ETA = "Estimaing...";
    private double progress = 0;

    /**
     * Constructs a new {@link YoutubeUplink} with the specified url.Saves the
     * completed file in the disk manager's output directory.
     *
     * @param url
     * @throws java.io.IOException If something went wrong
     */
    public YoutubeUplink(String url) throws IOException {
        this.url = url;
        this.title = getTitle();
    }

    /*
    /usr/local/bin/youtube-dl -f best -o "/home/sarah/Desktop/Indexing/Indexing Temp/%(title)s.%(ext)s"
    --newline
    --console-title
    --get-title
    youtube-dl --get-filename -o '%(title)s.mp4' 'https://www.youtube.com/watch?reload=9&v=qSjGouBmo0M'
     */
    @Override
    public boolean recieveTransmission() throws IOException {
        Process process = new ProcessBuilder("youtube-dl", "-f", "best", "--newline", "-o", getPath().toString() + ".mp4",
                this.url).inheritIO().start();
        try ( Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                var str = scanner.nextLine();
                System.out.println(str);
                //Check that we're on the right line that contains the ETA and percent
                if (str.contains("[download]") && str.contains("%") && str.contains("ETA")) {
                    //Set Progress & ETA
                    Pattern pattern = Pattern.compile("\\d+(\\.\\d{1,2})?%");
                    Matcher matcher = pattern.matcher(str);
                    this.progress = Double.parseDouble(matcher.group().replace("%", ""));
                    pattern = Pattern.compile("/d/d:/d/d");
                    matcher = pattern.matcher(str);
                    this.ETA = matcher.group();
                }
            }
        }
        try {
            process.waitFor();
            System.out.println("Progress: " + this.progress + " ETA: " + this.ETA);
        } catch (InterruptedException ex) {
            Logger.getLogger(YoutubeUplink.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    @Override
    public double getProgress() {
        return this.progress;
    }

    @Override
    public Path getPath() {
        return StellarDiskManager.getOutputFolder().resolve(title + ".mp4");
    }

    @Override
    public String getTitle() throws IOException {
        String title;
        ProcessBuilder builder = new ProcessBuilder("youtube-dl", "--get-filename", "-o", "%(title)s", url);
        Process proc = builder.start();
        try ( BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            title = reader.readLine();
        }
        return title;
    }

    @Override
    public String getEstimatedFinishTime() throws IOException {
        return this.ETA;
    }
}
