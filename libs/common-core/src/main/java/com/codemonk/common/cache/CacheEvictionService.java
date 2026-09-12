package com.codemonk.common.cache;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheEvictionService {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheEvictionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Evicts a single entry from a specific cache by key.
     */
    public void evictSingleCacheEntry(String cacheName, String key) {
        if (cacheName != null && key != null) {
            redisTemplate.delete(cacheName + "::" + key);
        }
    }

    /**
     * Clears all entries from a specified cache namespace.
     */
    public void evictAllCacheEntries(String cacheName) {
        if (cacheName != null) {
            Set<String> keys = redisTemplate.keys(cacheName + "::*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }
}
