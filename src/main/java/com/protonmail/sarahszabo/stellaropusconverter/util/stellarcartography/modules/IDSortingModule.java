/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules;

import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.LogEntry;
import java.util.Collections;
import java.util.List;

/**
 * A module for sorting IDs.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class IDSortingModule extends BufferingModule {

    @Override
    public List<LogEntry> output() {
        Collections.sort(this.entries);
        return Collections.unmodifiableList(entries);
    }

}
