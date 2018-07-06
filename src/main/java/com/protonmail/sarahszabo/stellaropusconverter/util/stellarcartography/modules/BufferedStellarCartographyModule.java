/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules;

import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.LogEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An expansion on {@link StellarCartographyModule} for buffered modules.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public abstract class BufferedStellarCartographyModule implements StellarCartographyModule {

    protected List<LogEntry> entries;

    /**
     * The default constructor for buffered modules.
     */
    protected BufferedStellarCartographyModule() {
        this.entries = new ArrayList<>(5);
    }

    /**
     * Enters the entry into the module. The functionality of this method is
     * module dependant.
     *
     * @param entry The entry to modify
     */
    @Override
    public void input(LogEntry entry) {
        this.entries.add(Objects.requireNonNull(entry));
    }

    @Override
    public void input(Collection<LogEntry> entries) {
        this.entries.addAll(Objects.requireNonNull(entries));
    }

    /**
     * Bulk output method. Applies module logic to results.
     *
     * @return The entries
     */
    public List<LogEntry> output() {
        return Collections.unmodifiableList(this.entries);
    }
}
