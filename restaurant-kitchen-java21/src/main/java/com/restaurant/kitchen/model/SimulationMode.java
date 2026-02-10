package com.restaurant.kitchen.model;

public enum SimulationMode {
    DEADLOCK,   // Chefs acquire locks in different order
    SAFE        // Chefs acquire locks in same order (Stove -> Blender)
}
