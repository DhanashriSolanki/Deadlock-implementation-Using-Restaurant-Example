package com.restaurant.kitchen.service;

import com.restaurant.kitchen.model.KitchenStatus;
import com.restaurant.kitchen.model.SimulationMode;
import com.restaurant.kitchen.resources.Blender;
import com.restaurant.kitchen.resources.Chef;
import com.restaurant.kitchen.resources.Stove;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class KitchenSimulator {

    private static final Logger logger = LogManager.getLogger(KitchenSimulator.class);

    private final Stove stove;
    private final Blender blender;

    private ExecutorService executor;
    private volatile boolean running = false;
    private AtomicBoolean runningFlag;
    private final AtomicInteger ordersCompleted = new AtomicInteger(0);
    private SimulationMode mode = SimulationMode.DEADLOCK;
    private int chefCount = 4;
    private final List<String> chefNames = new ArrayList<>();
    /**
     * Java 21 migration: Added progress-tracking fields for deadlock detection.
     * ThreadMXBean.findDeadlockedThreads() cannot detect deadlocks involving virtual threads
     * (virtual threads are not platform threads). Instead, we monitor order progress over time:
     * if no new orders are completed within a time window, a deadlock is inferred.
     */
    private int lastOrderCount = 0;
    private long lastCheckTime = 0;

    public KitchenSimulator(Stove stove, Blender blender) {
        this.stove = stove;
        this.blender = blender;
    }

    public void start(SimulationMode mode) {
        if (running) {
            logger.warn("=== Kitchen is already buzzing! Stop the current simulation first. ===");
            return;
        }
        this.mode = mode;
        this.ordersCompleted.set(0);
        this.runningFlag = new AtomicBoolean(true);
        this.chefNames.clear();
        /**
         * Java 21 migration: Replaced Executors.newFixedThreadPool(chefCount) with
         * Executors.newVirtualThreadPerTaskExecutor() (JEP 444 - Virtual Threads).
         *
         * Virtual threads are lightweight threads managed by the JVM, not the OS.
         * They are ideal for I/O-bound and blocking tasks:
         * - Platform threads: ~1MB stack each, limited by OS (thousands max)
         * - Virtual threads: ~few KB each, millions possible
         *
         * Each chef now runs on its own virtual thread instead of a pooled platform thread.
         */
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        logger.info("""
        ========================================
          RESTAURANT KITCHEN SIMULATOR
          Mode  : {}
          Chefs : {}
        ========================================""", mode.name(), chefCount);

        if (mode == SimulationMode.DEADLOCK) {
            logger.info("""
            WARNING: Deadlock mode enabled!
              -> Even chefs grab STOVE first, then BLENDER
              -> Odd chefs grab BLENDER first, then STOVE
              -> This WILL cause a deadlock!""");
        } else {
            logger.info("""
              Safe mode enabled.
              -> All chefs follow the same lock order: STOVE -> BLENDER
              -> No deadlock possible.""");
        }

        logger.info("""
        ========================================
          Opening the kitchen doors...
        ========================================""");

        for (int i = 0; i < chefCount; i++) {
            var chef = new Chef(i, stove, blender, mode, ordersCompleted, runningFlag);
            chefNames.add(chef.getName());
            executor.submit(chef);
        }
        running = true;
        logger.info("Kitchen is OPEN! {} chefs are cooking!", chefCount);
    }

    public void stop() {
        if (!running) {
            logger.info("Kitchen is already closed.");
            return;
        }
        running = false;
        if (runningFlag != null) {
            runningFlag.set(false);
        }
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("""
        ========================================
          KITCHEN CLOSED
          Total orders served : {}
        ========================================""", ordersCompleted.get());
    }

    public KitchenStatus getStatus() {
        boolean deadlocked = detectDeadlock();
        String message;

        if (!running) {
            message = "Kitchen is closed. Start a simulation to begin cooking!";
        } else if (deadlocked) {
            message = "DEADLOCK DETECTED! Both chefs are stuck waiting for each other's equipment. "
                    + "No orders can be completed. The kitchen is frozen!";
        } else {
            message = "Kitchen is running smoothly. " + ordersCompleted.get()
                    + " orders served so far. Chefs are cooking happily!";
        }

        return new KitchenStatus(
                running,
                deadlocked,
                ordersCompleted.get(),
                chefCount,
                mode,
                message,
                new ArrayList<>(chefNames)
        );
    }

    /**
     * Java 21 migration: Replaced ThreadMXBean-based deadlock detection with progress-based detection.
     *
     * Before (Java 17): ThreadMXBean.findDeadlockedThreads() - only works with platform threads.
     * After  (Java 21): Monitor order progress - if no orders complete within 2 seconds, infer deadlock.
     *
     * This approach works with virtual threads and is actually a more practical detection strategy
     * for real-world applications where "no progress" is the observable symptom of a deadlock.
     */
    private boolean detectDeadlock() {
        if (!running) return false;
        long now = System.currentTimeMillis();
        int currentOrders = ordersCompleted.get();

        if (lastCheckTime == 0) {
            lastCheckTime = now;
            lastOrderCount = currentOrders;
            return false;
        }
        if (now - lastCheckTime > 2000) {
            if (currentOrders == lastOrderCount) {
                return true;
            }
            lastOrderCount = currentOrders;
            lastCheckTime = now;
        }
        return false;
    }

    @PreDestroy
    public void cleanup() {
        stop();
    }
}
