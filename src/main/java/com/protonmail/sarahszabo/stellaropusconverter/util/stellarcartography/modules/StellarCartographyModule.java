/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.modules;

import com.protonmail.sarahszabo.stellaropusconverter.util.stellarcartography.LogEntry;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A marker interface for cartography modules
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public interface StellarCartographyModule {

    /**
     * Enters the entry into the module. The functionality of this method is
     * module dependant.
     *
     * @param entry The entry to modify
     */
    void input(LogEntry entry);

    /**
     * Bulk Input Method
     *
     * @param entries The entries to add
     */
    default void input(Collection<LogEntry> entries) {
        entries.stream().forEachOrdered(entry -> input(entry));
    }

    /**
     * Gets the output from either a single or buffered module.
     *
     * @return The output list
     */
    default List<LogEntry> getOutput() {
        if (this instanceof BufferedStellarCartographyModule) {
            BufferedStellarCartographyModule module = (BufferedStellarCartographyModule) this;
            return module.output();
        } else if (this instanceof SingleModule) {
            SingleModule module = (SingleModule) this;
            return Stream.of(module.getOutputLogEntry().get()).collect(Collectors.toList());
        } else {
            throw new RuntimeException("Module is Neither Single Nor Buffered!");
        }
    }
}
