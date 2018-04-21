/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An interface representing the mode that the converter is in.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public enum StellarMode {
    /**
     * Used when only a direct link to the file is chosen, and nothing else.
     */
    DIRECT_LINK {
        @Override
        public void start(String... args) {
            try {
                StellarOPUSConverter converter = new StellarOPUSConverter(Paths.get(args[0]));
                converter.convertToOPUS();
            } catch (IOException ex) {
                Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
                printIOExceptionMessage();
            }
        }
    },
    /**
     * Used when a URL, author, and start and end times are specified.
     */
    LINK_AUTHOR_TIMESTAMPS {
        @Override
        public void start(String... args) {
            FFMPEGTimeStamp start = FFMPEGTimeStamp.fromString(args[1]),
                    end = FFMPEGTimeStamp.fromString(args[2]);
            try {
                StellarOPUSConverter converter = new StellarOPUSConverter(Paths.get(args[0]));
                converter.convertToOPUS(start, end);
            } catch (IOException ex) {
                Logger.getLogger(StellarMode.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }, GRAPHICAL_FILE_CHOICE_DIRECT_LINKS {
        @Override
        public void start(String... args) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };

    public abstract void start(String... args);

    private static void printIOExceptionMessage() {
        System.err.println("We've encountered an I/O error. Check your disk capacity.");
    }
}
