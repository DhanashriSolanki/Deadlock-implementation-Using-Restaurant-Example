package com.restaurant.kitchen.model;

import java.util.List;

public record KitchenStatus (boolean running,boolean deadlocked,int ordersCompleted,int activeChefs,SimulationMode mode,String message,List<String> chefNames){

}
