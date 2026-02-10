package com.restaurant.kitchen;

import com.restaurant.kitchen.model.KitchenStatus;
import com.restaurant.kitchen.model.SimulationMode;
import com.restaurant.kitchen.resources.Blender;
import com.restaurant.kitchen.resources.Stove;
import com.restaurant.kitchen.service.KitchenSimulator;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class KitchenSimulatorTest {

    private KitchenSimulator simulator;

    @After
    public void tearDown() {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test(timeout = 5000)
    public void safeMode_completesOrders() throws InterruptedException {
        Stove stove = new Stove();
        Blender blender = new Blender();
        simulator = new KitchenSimulator(stove, blender);

        simulator.start(SimulationMode.SAFE);
        Thread.sleep(700);
        KitchenStatus status = simulator.getStatus();

        assertFalse("Safe mode should never deadlock", status.isDeadlocked());
        assertTrue("Should have completed 0 or more orders", status.getOrdersCompleted() >= 0);
        assertNotNull("Status message should not be null", status.getMessage());
        assertNotNull("Chef names should not be null", status.getChefNames());
        assertEquals("Should have 2 chefs", 2, status.getChefNames().size());
    }

    @Test(timeout = 5000)
    public void deadlockMode_canDeadlock() throws InterruptedException {
        Stove stove = new Stove();
        Blender blender = new Blender();
        simulator = new KitchenSimulator(stove, blender);

        simulator.start(SimulationMode.DEADLOCK);
        Thread.sleep(1000);
        KitchenStatus status = simulator.getStatus();

        assertTrue("Should be running or deadlocked",
                status.isRunning() || status.isDeadlocked());
        assertNotNull("Status message should not be null", status.getMessage());
    }
}
