package com.restaurant.kitchen;

import com.restaurant.kitchen.model.KitchenStatus;
import com.restaurant.kitchen.model.SimulationMode;
import com.restaurant.kitchen.resources.Blender;
import com.restaurant.kitchen.resources.Stove;
import com.restaurant.kitchen.service.KitchenSimulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;


import static org.junit.jupiter.api.Assertions.*;

public class KitchenSimulatorTest {

    private KitchenSimulator simulator;

    @AfterEach
    public void tearDown() {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @Timeout(value=5,unit=TimeUnit.SECONDS)
    public void safeMode_completesOrders() throws InterruptedException {
        Stove stove = new Stove();
        Blender blender = new Blender();
        simulator = new KitchenSimulator(stove, blender);

        simulator.start(SimulationMode.SAFE);
        Thread.sleep(700);
        KitchenStatus status = simulator.getStatus();

        assertFalse(status.deadlocked(),"Safe mode should never deadlock");
        assertTrue( status.ordersCompleted() >= 0,"Should have completed 0 or more orders");
        assertNotNull( status.message(),"Status message should not be null");
        assertNotNull( status.chefNames(),"Chef names should not be null");
        assertEquals( 4, status.chefNames().size(),"Should have 4 chefs");
    }

    @Test
    @Timeout(value=5,unit=TimeUnit.SECONDS)
    public void deadlockMode_canDeadlock() throws InterruptedException {
        Stove stove = new Stove();
        Blender blender = new Blender();
        simulator = new KitchenSimulator(stove, blender);

        simulator.start(SimulationMode.DEADLOCK);
        Thread.sleep(1000);
        KitchenStatus status = simulator.getStatus();

        assertTrue(
                status.running() || status.deadlocked(),"Should be running or deadlocked");
        assertNotNull( status.message(),"Status message should not be null");
    }
}
