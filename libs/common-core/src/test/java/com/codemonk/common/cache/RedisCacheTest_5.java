package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCacheTest_5 {

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
    void shouldNotTouchRedisWhenGeneratingKey() {
        redisCache.generateKey("order", "789");

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldReturnValueStoredUnderGeneratedKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Object value = new Object();
        String key = redisCache.generateKey("order", "789");
        when(valueOperations.get("order:789")).thenReturn(value);

        assertSame(value, redisCache.get(key));
    }

    @Test
    void shouldReturnNullOnCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:404")).thenReturn(null);

        assertNull(redisCache.get("order:404"));
    }

    @Test
    void shouldPassExactTtlToRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        redisCache.put("order:789", "pending", Duration.ofSeconds(90));

        verify(valueOperations).set(eq("order:789"), eq("pending"), ttlCaptor.capture());
        assertEquals(90, ttlCaptor.getValue().toSeconds());
    }

    @Test
    void shouldEvictOnlyTheGivenKey() {
        redisCache.evict("order:789");

        verify(redisTemplate).delete("order:789");
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void shouldNotWriteValueWhenEvicting() {
        redisCache.evict("order:789");

        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }
}
