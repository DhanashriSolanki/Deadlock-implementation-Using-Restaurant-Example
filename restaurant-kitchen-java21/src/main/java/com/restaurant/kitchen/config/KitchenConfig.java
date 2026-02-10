package com.restaurant.kitchen.config;

import com.restaurant.kitchen.resources.Blender;
import com.restaurant.kitchen.resources.Stove;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KitchenConfig {

    @Bean
    public Stove stove() {
        return new Stove();
    }

    @Bean
    public Blender blender() {
        return new Blender();
    }
}
