package com.restaurant.kitchen.resources;

import com.restaurant.kitchen.model.SimulationMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Chef implements Runnable {
    private static final Logger logger = LogManager.getLogger(Chef.class);

    private static final String[] CHEF_NAMES = {"Gordon", "Julia", "Marco", "Heston"};

    private static final List<String> DISHES = Arrays.asList(
            "Spaghetti Carbonara",
            "Mushroom Risotto",
            "Grilled Salmon",
            "Tomato Basil Soup",
            "Chocolate Lava Cake",
            "Caesar Salad",
            "Beef Wellington",
            "Pad Thai"
    );

    private final int id;
    private final String name;
    private final Stove stove;
    private final Blender blender;
    private final SimulationMode mode;
    private final AtomicInteger ordersCompleted;
    private final AtomicBoolean running;
    private final Random random = new Random();

    public Chef(int id, Stove stove, Blender blender, SimulationMode mode,
                AtomicInteger ordersCompleted, AtomicBoolean running) {
        this.id = id;
        this.name = CHEF_NAMES[id % CHEF_NAMES.length];
        this.stove = stove;
        this.blender = blender;
        this.mode = mode;
        this.ordersCompleted = ordersCompleted;
        this.running = running;
    }

    public String getName() {
        return name;
    }

    @Override
    public void run() {
        logger.info("[Chef {}] Reporting for duty! Ready to cook.", name);
        while (running.get()) {
            try {
                processOrder();
            } catch (InterruptedException e) {
                logger.info("[Chef {}] Clocking out for the day. Goodbye!", name);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private String pickDish() {
        return DISHES.get(random.nextInt(DISHES.size()));
    }

    private void processOrder() throws InterruptedException {
        String dish = pickDish();
        ReentrantLock stoveLock = stove.getLock();
        ReentrantLock blenderLock = blender.getLock();

        logger.info("[Chef {}] New order received: {}. Heading to the kitchen...", name, dish);

        switch (mode) {
            case DEADLOCK -> {
                if (id % 2 == 0) {
                    // Even chef: grabs Stove first, then Blender
                    logger.info("[Chef {}] Walking towards the STOVE to start cooking {}...", name, dish);
                    stoveLock.lockInterruptibly();
                    logger.info("[Chef {}] Got the STOVE! Now I need the BLENDER for {}...", name, dish);
                    try {
                        blenderLock.lockInterruptibly();
                        logger.info("[Chef {}] Got the BLENDER too! Making {} now!", name, dish);
                        try {
                            stove.use();
                            blender.use();
                            int total = ordersCompleted.incrementAndGet();
                            logger.info("[Chef {}] Ding ding! {} is READY! (Orders served today: {})", name, dish, total);
                        } finally {
                            blenderLock.unlock();
                        }
                    } finally {
                        stoveLock.unlock();
                        logger.info("[Chef {}] Released STOVE and BLENDER. Ready for next order!", name);
                    }
                } else {
                    // Odd chef: grabs Blender first, then Stove (OPPOSITE ORDER = DEADLOCK!)
                    logger.info("[Chef {}] Walking towards the BLENDER to prep {}...", name, dish);
                    blenderLock.lockInterruptibly();
                    logger.info("[Chef {}] Got the BLENDER! Now I need the STOVE for {}...", name, dish);
                    try {
                        stoveLock.lockInterruptibly();
                        logger.info("[Chef {}] Got the STOVE too! Making {} now!", name, dish);
                        try {
                            blender.use();
                            stove.use();
                            int total = ordersCompleted.incrementAndGet();
                            logger.info("[Chef {}] Ding ding! {} is READY! (Orders served today: {})", name, dish, total);
                        } finally {
                            stoveLock.unlock();
                        }
                    } finally {
                        blenderLock.unlock();
                        logger.info("[Chef {}] Released STOVE and BLENDER. Ready for next order!", name);
                    }
                }
            }

            case SAFE -> {
                // SAFE: All chefs take Stove -> Blender (consistent order, no deadlock)
                logger.info("[Chef {}] Walking towards the STOVE first (safe protocol)...", name);
                stoveLock.lockInterruptibly();
                logger.info("[Chef {}] Got the STOVE! Now grabbing the BLENDER...", name);
                try {
                    blenderLock.lockInterruptibly();
                    logger.info("[Chef {}] Got the BLENDER! Cooking {} now!", name, dish);
                    try {
                        stove.use();
                        blender.use();
                        int total = ordersCompleted.incrementAndGet();
                        logger.info("[Chef {}] Ding ding! {} is READY! (Orders served today: {})", name, dish, total);
                    } finally {
                        blenderLock.unlock();
                    }
                } finally {
                    stoveLock.unlock();
                    logger.info("[Chef {}] Released STOVE and BLENDER. Ready for next order!", name);
                }
            }
        }
    }
}
