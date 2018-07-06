/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography;

/**
 * An enum representing the levels that a logger can log at.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public enum LogLevel {
    /**
     * The default level.
     */
    DEFAULT {
        @Override
        public String toString() {
            return "Default";
        }

        @Override
        public int levelAura() {
            return 1;
        }
    },
    /**
     * Used for exceptions.
     */
    EXCEPTION {
        @Override
        public String toString() {
            return "Exception";
        }

        @Override
        public int levelAura() {
            return Integer.MAX_VALUE;
        }
    },
    /**
     * Only used during debugging.
     */
    DEBUG {
        @Override
        public String toString() {
            return "Debug";
        }

        @Override
        public int levelAura() {
            return 0;
        }
    },
    /**
     * Used when warnings are to be issued.
     */
    WARNING {
        @Override
        public String toString() {
            return "Warning";
        }

        @Override
        public int levelAura() {
            return 2;
        }
    },
    /**
     * A high priority warning.
     */
    HIGH {
        @Override
        public String toString() {
            return "High";
        }

        @Override
        public int levelAura() {
            return Integer.MAX_VALUE;
        }
    },
    /**
     * A low priority warning.
     */
    LOW {
        @Override
        public String toString() {
            return "Low";
        }

        @Override
        public int levelAura() {
            return 0;
        }
    };

    @Override
    public abstract String toString();

    /**
     * The level aura of this log level. If the number is high, it is a high
     * priority. Used to check if a level is greater than another.
     *
     * @return The log level number
     */
    public abstract int levelAura();
}
