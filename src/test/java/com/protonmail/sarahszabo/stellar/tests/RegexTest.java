/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellar.tests;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class RegexTest {

    public RegexTest() {
    }

    @Test
    public void testRegex() {
        String test = "Mitch Murder -- Lit";
        System.out.println("REGEX: " + test.matches(".*[-+].*"));
        List<String> list = Arrays.asList(test.split("[-+]")).stream().filter(string -> !string.isEmpty())
                .map(string -> string.trim()).collect(Collectors.toList());
        System.out.println(list);
    }
}
