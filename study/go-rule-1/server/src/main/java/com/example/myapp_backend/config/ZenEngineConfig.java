package com.example.myapp_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.gorules.zen_engine.ZenEngine;

@Configuration
public class ZenEngineConfig {

    @Bean
    public ZenEngine zenEngine() {
        return new ZenEngine(null, null);
    }
}