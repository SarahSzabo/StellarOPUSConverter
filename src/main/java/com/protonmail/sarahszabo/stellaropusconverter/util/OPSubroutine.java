/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util;

/**
 * A functional interface representing a segment of code that can be run.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
@FunctionalInterface
public interface OPSubroutine {

    /**
     * Runs this subroutine.
     */
    void init();
}
