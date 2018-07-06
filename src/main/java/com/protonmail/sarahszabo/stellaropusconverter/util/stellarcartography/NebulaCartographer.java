/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography;

/**
 * The default implementation of {@link StellarCartographer}. Prints all logs to
 * STOut.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 * @param <C> The class this logger is logging for
 */
public class NebulaCartographer<C> extends StellarCartographer<C> {

    /**
     * Constructs a new {@link NebulaCartographer} with the specified logging
     * level.
     *
     * @param level The level to use
     */
    public NebulaCartographer(LogLevel level) {
        super(level);
    }

    @Override
    public String toString() {
        return "Stellar Cartographer";
    }

}
