package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCacheTest_3 {

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
    void shouldGenerateCacheKeyWithNullPrefix() {
        String result = redisCache.generateKey(null, "123");

        assertEquals("null:123", result);
    }

    @Test
    void shouldGenerateCacheKeyWithNullIdentifier() {
        String result = redisCache.generateKey("user", null);

        assertEquals("user:null", result);
    }

    @Test
    void shouldPutAndRetrieveUsingTheSameGeneratedKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> putKeyCaptor = ArgumentCaptor.forClass(String.class);
        String key = redisCache.generateKey("cart", "99");

        redisCache.put(key, "two-items", Duration.ofMinutes(15));
        when(valueOperations.get("cart:99")).thenReturn("two-items");
        Object result = redisCache.get(key);

        verify(valueOperations).set(putKeyCaptor.capture(), eq("two-items"), eq(Duration.ofMinutes(15)));
        assertEquals("cart:99", putKeyCaptor.getValue());
        assertEquals("two-items", result);
    }

    @Test
    void shouldPassThroughNegativeTtlWithoutValidation() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration negativeTtl = Duration.ofSeconds(-5);

        redisCache.put("stale:1", "already-expired", negativeTtl);

        verify(valueOperations).set("stale:1", "already-expired", negativeTtl);
    }

    @Test
    void shouldEvictMultipleKeysIndependently() {
        redisCache.evict("user:1");
        redisCache.evict("user:2");

        InOrder order = inOrder(redisTemplate);
        order.verify(redisTemplate).delete("user:1");
        order.verify(redisTemplate).delete("user:2");
    }
}