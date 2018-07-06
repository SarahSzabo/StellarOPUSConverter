/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography;

/**
 * An implementation of {@link StellarCartographer} that doesn't display
 * anything using its log methods. Useful for when we want logging disabled.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class VoidCartographer<C> extends StellarCartographer<C> {

    /**
     * Constructs a new {@link VoidCartographer} with the specified logging
     * level.
     *
     * @param level The level to use
     */
    public VoidCartographer(LogLevel level) {
        super(level);
    }

    @Override
    public void log(String log) {
        //Do Nothing
    }

    @Override
    public String toString() {
        return "Void Cartographer";
    }

}
