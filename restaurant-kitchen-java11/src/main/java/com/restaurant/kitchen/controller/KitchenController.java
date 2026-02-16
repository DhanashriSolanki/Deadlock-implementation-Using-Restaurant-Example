package com.restaurant.kitchen.controller;

import com.restaurant.kitchen.model.KitchenStatus;
import com.restaurant.kitchen.model.SimulationMode;
import com.restaurant.kitchen.service.KitchenSimulator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {

    private final KitchenSimulator simulator;

    public KitchenController(KitchenSimulator simulator) {
        this.simulator = simulator;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        var welcome = new LinkedHashMap<String,Object>();
        welcome.put("app", "Restaurant Kitchen Resource Manager");
        welcome.put("description", "A Java 21 deadlock demonstration using Spring Boot");
        welcome.put("version", "1.0.0");

        var endpoints = new LinkedHashMap<String,Object>();
        endpoints.put("POST /api/kitchen/start?mode=DEADLOCK", "Start simulation in DEADLOCK mode (chefs will get stuck!)");
        endpoints.put("POST /api/kitchen/start?mode=SAFE", "Start simulation in SAFE mode (no deadlock)");
        endpoints.put("GET  /api/kitchen/status", "Check kitchen status, deadlock detection, orders served");
        endpoints.put("POST /api/kitchen/stop", "Stop the current simulation");
        welcome.put("endpoints", endpoints);

        var howItWorks = new LinkedHashMap<String,Object>();
        howItWorks.put("DEADLOCK mode", "Chef Gordon grabs STOVE first, Chef Julia grabs BLENDER first. "
                + "Both wait forever for each other's equipment. Classic deadlock!");
        howItWorks.put("SAFE mode", "Both chefs always grab STOVE first, then BLENDER. "
                + "Consistent lock ordering prevents deadlock.");
        welcome.put("howItWorks", howItWorks);

        return ResponseEntity.ok(welcome);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestParam(defaultValue = "DEADLOCK") String mode) {
        SimulationMode simMode = SimulationMode.valueOf(mode.toUpperCase());
        simulator.start(simMode);

        var response = new LinkedHashMap<String,Object>();
        response.put("status", "STARTED");
        response.put("mode", simMode.name());

        if (simMode == SimulationMode.DEADLOCK) {
            response.put("message", "Kitchen is open in DEADLOCK mode! "
                    + "Chef Gordon will grab the Stove first, while Chef Julia grabs the Blender first. "
                    + "Check /api/kitchen/status to see the deadlock happen!");
            response.put("warning", "Chefs will get stuck! This demonstrates a real deadlock scenario.");
        } else {
            response.put("message", "Kitchen is open in SAFE mode! "
                    + "Both chefs follow the same lock order (Stove -> Blender). "
                    + "Check /api/kitchen/status to see orders being completed!");
            response.put("tip", "Compare the ordersCompleted count with DEADLOCK mode (which will be 0).");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        KitchenStatus statusBeforeStop = simulator.getStatus();
        simulator.stop();

        var response = new LinkedHashMap<String,Object>();
        response.put("status", "STOPPED");
        response.put("totalOrdersServed", statusBeforeStop.ordersCompleted());
        response.put("wasDeadlocked", statusBeforeStop.deadlocked());
        if (statusBeforeStop.deadlocked()) {
            response.put("message", "Kitchen was DEADLOCKED when stopped. "
                    + "Both chefs were stuck waiting for each other. "
                    + "Zero orders completed - the restaurant lost money today!");
        } else {
            response.put("message", "Kitchen closed after serving "
                    + statusBeforeStop.ordersCompleted() + " orders. Great shift!");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<KitchenStatus> status() {
        return ResponseEntity.ok(simulator.getStatus());
    }
}
