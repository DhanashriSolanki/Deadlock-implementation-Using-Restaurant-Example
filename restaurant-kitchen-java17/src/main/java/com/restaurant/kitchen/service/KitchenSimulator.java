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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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
    private int chefCount = 2;
    private final List<String> chefNames = new ArrayList<>();

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
        this.executor = Executors.newFixedThreadPool(chefCount);

        logger.info("""
    ========================================
      RESTAURANT KITCHEN SIMULATOR
      Mode  : {}
      Chefs : {}
    ========================================""", mode, chefCount);

        if (mode == SimulationMode.DEADLOCK) {
            logger.info("""
              WARNING: Deadlock mode enabled!
              -> Even chefs grab STOVE first, then BLENDER
              -> Odd chefs grab BLENDER first, then STOVE
              -> This WILL cause a deadlock!""");
        } else {
            logger.info("""
              Safe mode enabled.
              -> All chefs follow the same lock order: STOVE -> BLENDER");
              -> No deadlock possible.""");
        }

        logger.info("""
                        ========================================");
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
        var deadlocked = detectDeadlock();
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

    private boolean detectDeadlock() {
        if (!running) return false;
        var bean = ManagementFactory.getThreadMXBean();
        var deadlockedThreads = bean.findDeadlockedThreads();
        return deadlockedThreads != null && deadlockedThreads.length > 0;
    }

    @PreDestroy
    public void cleanup() {
        stop();
    }
}
