package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCacheTest_4 {

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
    void shouldGenerateDistinctKeysForDifferentPrefixes() {
        String userKey = redisCache.generateKey("user", "42");
        String productKey = redisCache.generateKey("product", "42");

        assertNotEquals(userKey, productKey);
    }

    @Test
    void shouldStoreValueWithLongTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration ttl = Duration.ofDays(7);

        redisCache.put("product:42", "catalog-entry", ttl);

        verify(valueOperations).set("product:42", "catalog-entry", ttl);
    }

    @Test
    void shouldStoreNullValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration ttl = Duration.ofMinutes(1);

        redisCache.put("product:42", null, ttl);

        verify(valueOperations).set("product:42", null, ttl);
    }

    @Test
    void shouldReturnLatestValueAfterOverwrite() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration ttl = Duration.ofMinutes(10);

        redisCache.put("product:42", "v1", ttl);
        redisCache.put("product:42", "v2", ttl);
        when(valueOperations.get("product:42")).thenReturn("v2");

        InOrder order = inOrder(valueOperations);
        order.verify(valueOperations).set("product:42", "v1", ttl);
        order.verify(valueOperations).set("product:42", "v2", ttl);
        assertEquals("v2", redisCache.get("product:42"));
    }

    @Test
    void shouldNotEvictWhenReadingValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:42")).thenReturn("catalog-entry");

        redisCache.get("product:42");

        verify(redisTemplate, never()).delete("product:42");
    }

    @Test
    void shouldNotFailWhenEvictingMissingKey() {
        when(redisTemplate.delete("product:missing")).thenReturn(false);

        assertDoesNotThrow(() -> redisCache.evict("product:missing"));

        verify(redisTemplate).delete("product:missing");
    }
}
