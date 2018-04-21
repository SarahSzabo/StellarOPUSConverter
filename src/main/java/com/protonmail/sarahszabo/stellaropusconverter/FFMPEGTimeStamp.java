/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

/**
 * A class representing an FFMPEG timestamp. Displays time in the format
 * HH:MM:SS.MM.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class FFMPEGTimeStamp implements Comparable<FFMPEGTimeStamp> {

    /**
     * Constructs a new {@link FFMPEGTimeStamp} with the already existing string
     * timestamp.
     *
     * @param ffmpegTimeStamp The string version of the timestamp
     * @return The newly constructed timestamp
     */
    public static FFMPEGTimeStamp fromString(String ffmpegTimeStamp) {
        String[] times = ffmpegTimeStamp.split(":");
        if (times.length != 3) {
            throw new IllegalArgumentException("The number of arguments is not 3. Check the way you entered the hours-minutes-seconds");
        }
        return new FFMPEGTimeStamp(Integer.parseInt(times[0]), Integer.parseInt(times[1]),
                Integer.parseInt(times[2]));
    }

    private final int hour, minute, second, millisecond;
    private final String timestamp;

    /**
     * A new Timestamp with hour = 0, minute = 0.
     *
     * @param second The timestamp
     */
    public FFMPEGTimeStamp(int second) {
        this(0, 0, second, 0);
    }

    /**
     * A new Timestamp with hour = 0, millisecond = 0.
     *
     * @param minute THe number of minutes
     * @param second The seconds
     */
    public FFMPEGTimeStamp(int minute, int second) {
        this(0, minute, second, 0);
    }

    /**
     * A new Timestamp with millisecond = 0.
     *
     * @param hour The hours
     * @param minute The minutes
     * @param second The seconds
     */
    public FFMPEGTimeStamp(int hour, int minute, int second) {
        this(hour, minute, second, 0);
    }

    /**
     * Creates a new {@link FFMPEGTimeStamp} with the specified times.
     *
     * @param hour The number of hours
     * @param minute The number of minutes
     * @param second The number of seconds
     * @param millisecond The number of milliseconds
     */
    public FFMPEGTimeStamp(int hour, int minute, int second, int millisecond) {
        if (hour < 0 || minute < 0 || second < 0 || millisecond < 0) {
            throw new IllegalArgumentException("One of the fields is less than zero!");
        } else if (minute >= 60) {
            throw new IllegalArgumentException("Minutes greater than or equal to 60");
        } else if (second >= 60) {
            throw new IllegalArgumentException("Seconds greater than or equal to 60");
        }
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
        timestamp = correctFormat(hour) + ":" + correctFormat(minute) + ":" + correctFormat(second) + "." + correctFormat(millisecond);
    }

    /**
     * If the time is less than 10, adds a zero, so: 1 -> 01
     */
    private String correctFormat(int number) {
        return number < 10 ? "0" + number : "" + number;
    }

    @Override
    public String toString() {
        return this.timestamp;
    }

    /**
     * Gets the "Absolute time" by converting everything to seconds.
     *
     * @return The absolute time
     */
    public double getAbsoluteTime() {
        return this.hour * 60 + this.minute * 60 + this.second + this.millisecond / 1000;
    }

    @Override
    public int compareTo(FFMPEGTimeStamp other) {
        return Double.compare(getAbsoluteTime(), other.getAbsoluteTime());
    }

    /**
     * Getter for hours
     *
     * @return The hours
     */
    public int getHour() {
        return hour;
    }

    /**
     * Getter for minutes.
     *
     * @return The minutes
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Getter for seconds.
     *
     * @return The seconds
     */
    public int getSecond() {
        return second;
    }

    /**
     * Getter for milliseconds.
     *
     * @return The number of milliseconds
     */
    public int getMillisecond() {
        return millisecond;
    }

    /**
     * Getter for the timestamp.
     *
     * @return The properly formatted timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

}
