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
class RedisCacheTest_2 {

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
    void shouldGenerateCacheKeyWithEmptyPrefix() {
        String result = redisCache.generateKey("", "123");

        assertEquals(":123", result);
    }

    @Test
    void shouldGenerateCacheKeyWithEmptyIdentifier() {
        String result = redisCache.generateKey("user", "");

        assertEquals("user:", result);
    }

    @Test
    void shouldGenerateCacheKeyWhenIdentifierContainsColon() {
        String result = redisCache.generateKey("user", "123:profile");

        assertEquals("user:123:profile", result);
    }

    @Test
    void shouldStoreValueWithZeroTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Object value = new Object();
        Duration ttl = Duration.ZERO;

        redisCache.put("session:456", value, ttl);

        verify(valueOperations).set("session:456", value, ttl);
    }
}