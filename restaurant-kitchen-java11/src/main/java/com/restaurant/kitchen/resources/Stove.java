package com.restaurant.kitchen.resources;

import java.util.concurrent.locks.ReentrantLock;

public class Stove {
    private final ReentrantLock lock = new ReentrantLock();

    public ReentrantLock getLock() {
        return lock;
    }

    public void use() throws InterruptedException {
        Thread.sleep(50); // Simulate cooking time
    }
}
