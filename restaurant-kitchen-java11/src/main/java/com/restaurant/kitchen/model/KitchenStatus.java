package com.restaurant.kitchen.model;

import java.util.List;

/**
 * Java 17 migration: Converted from traditional POJO (with boilerplate getters/setters/constructor)
 * to a Record (Java 16 feature). Records automatically generate:
 * - constructor, equals(), hashCode(), toString()
 * - accessor methods: running(), deadlocked(), ordersCompleted(), etc.
 *
 * Before (Java 11): ~42 lines of boilerplate code
 * After  (Java 17): 3 lines with identical functionality
 */
public record KitchenStatus(boolean running, boolean deadlocked, int ordersCompleted,
                            int activeChefs, SimulationMode mode, String message,
                            List<String> chefNames) {}
