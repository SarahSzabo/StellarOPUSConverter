/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.stellaropusconverter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A space in which conversion tasks can be placed, and they will be completed
 * in a concurrent fashion.
 *
 * @author Sarah Szabo <PhysicistSarah@Gmail.com>
 */
public class StellarHyperspace {

    /**
     * Tasks executed in hyperspace.
     */
    private static final ExecutorService hyperspace = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1,
            new ThreadFactory() {
        private int threadCount = 0;

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Stellar Hyperspace Thread " + threadCount++);
        }
    });

    /**
     * Causes a false vacuum, clearing the hyperspace of any new requests.
     */
    public static void initiateFalseVacuum() {
        hyperspace.shutdown();
    }

    /**
     * Runs the selected conversion tasks in hyperspace.
     *
     * @param tasks The tasks to run
     * @return The future paths
     */
    public static List<Future<Path>> runConversionTasks(Collection<Path> tasks) {
        List<Callable<Path>> hyperspaceTasks = tasks.stream().map(path -> {
            return (Callable<Path>) () -> {
                StellarOPUSConverter converter = new StellarOPUSConverter(path);
                return converter.convertToOPUS();
            };
        }).collect(Collectors.toList());
        try {
            return hyperspace.invokeAll(hyperspaceTasks);
        } catch (InterruptedException ex) {
            Logger.getLogger(StellarHyperspace.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Hyperspace Conversions Interrupted");
        }
    }

    /**
     * Utility class, no instances required.
     */
    private StellarHyperspace() {
        throw new AssertionError("Utility Class");
    }
}
