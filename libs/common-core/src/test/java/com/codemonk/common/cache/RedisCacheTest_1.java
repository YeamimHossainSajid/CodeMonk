package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCacheTest_1 {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCache redisCache;

    @BeforeEach
    void setUp() {
        redisCache = new RedisCache(redisTemplate);
    }

    @Test
    void shouldGenerateCacheKey() {
        String result = redisCache.generateKey("user", "123");

        assertEquals("user:123", result);
    }

    @Test
    void shouldStoreValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Object value = new Object();
        Duration ttl = Duration.ofMinutes(5);

        redisCache.put("user:123", value, ttl);

        verify(valueOperations).set("user:123", value, ttl);
    }

    @Test
    void shouldEvictCacheEntry() {
        redisCache.evict("user:123");

        verify(redisTemplate).delete("user:123");
    }
}