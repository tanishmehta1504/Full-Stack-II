package com.example.dashboard.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Cache backend + regions are declared in ehcache.xml (see resources/ehcache.xml).
    // spring.cache.type=jcache in application.properties wires Spring's cache
    // abstraction to Ehcache 3 through the JSR-107 (JCache) API.
}
