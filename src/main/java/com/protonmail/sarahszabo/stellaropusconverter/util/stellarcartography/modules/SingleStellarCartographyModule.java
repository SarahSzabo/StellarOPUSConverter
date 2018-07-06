/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules;

import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.LogEntry;
import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.StellarCartographer;
import java.util.Objects;
import java.util.Optional;

/**
 * A module enabling advanced features such as buffering to
 * {@link StellarCartographer}s
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public abstract class SingleStellarCartographyModule implements StellarCartographyModule {

    protected LogEntry entry;

    /**
     * Enters the entry into the module. The functionality of this method is
     * module dependant.
     *
     * @param entry The entry to modify
     */
    @Override
    public void input(LogEntry entry) {
        this.entry = Objects.requireNonNull(entry);
    }

    /**
     * Gets an output after calling {@link SingleStellarCartographyModule#input(com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.LogEntry)
     * }.
     *
     * @return The output of this {@link SingleStellarCartographyModule}
     */
    public Optional<LogEntry> getOutputLogEntry() {
        return Optional.of(this.entry);
    }
}
