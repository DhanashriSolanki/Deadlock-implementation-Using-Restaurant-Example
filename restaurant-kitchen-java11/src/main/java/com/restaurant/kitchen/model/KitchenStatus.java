package com.restaurant.kitchen.model;

import java.util.List;

public class KitchenStatus {
    private boolean running;
    private boolean deadlocked;
    private int ordersCompleted;
    private int activeChefs;
    private SimulationMode mode;
    private String message;
    private List<String> chefNames;

    public KitchenStatus() {}

    public KitchenStatus(boolean running, boolean deadlocked, int ordersCompleted,
                         int activeChefs, SimulationMode mode, String message,
                         List<String> chefNames) {
        this.running = running;
        this.deadlocked = deadlocked;
        this.ordersCompleted = ordersCompleted;
        this.activeChefs = activeChefs;
        this.mode = mode;
        this.message = message;
        this.chefNames = chefNames;
    }

    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public boolean isDeadlocked() { return deadlocked; }
    public void setDeadlocked(boolean deadlocked) { this.deadlocked = deadlocked; }
    public int getOrdersCompleted() { return ordersCompleted; }
    public void setOrdersCompleted(int ordersCompleted) { this.ordersCompleted = ordersCompleted; }
    public int getActiveChefs() { return activeChefs; }
    public void setActiveChefs(int activeChefs) { this.activeChefs = activeChefs; }
    public SimulationMode getMode() { return mode; }
    public void setMode(SimulationMode mode) { this.mode = mode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getChefNames() { return chefNames; }
    public void setChefNames(List<String> chefNames) { this.chefNames = chefNames; }
}
