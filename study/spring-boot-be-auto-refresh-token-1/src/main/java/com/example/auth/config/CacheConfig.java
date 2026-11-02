package com.example.auth.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String REVOKED_SIDS = "revoked-sids";

    @Bean
    CacheManager cacheManager(AppProperties props) {
        var manager = new CaffeineCacheManager();
        // An entry must outlive every access token issued before the revoke.
        manager.registerCustomCache(REVOKED_SIDS, Caffeine.newBuilder()
                .expireAfterWrite(props.auth().accessTtl().plusMinutes(1))
                .maximumSize(100_000)
                .build());
        return manager;
    }
}
