package com.restaurant.kitchen.model;

import java.util.List;

/**
 * Direct Java 11 -> 21 migration:
 * Converted from 42-line POJO with boilerplate getters/setters/constructors
 * to a Record (Java 16+). Records auto-generate constructor, equals(), hashCode(),
 * toString(), and accessor methods (e.g. running(), deadlocked(), ordersCompleted()).
 */
public record KitchenStatus(boolean running, boolean deadlocked, int ordersCompleted,
                            int activeChefs, SimulationMode mode, String message,
                            List<String> chefNames) {}
