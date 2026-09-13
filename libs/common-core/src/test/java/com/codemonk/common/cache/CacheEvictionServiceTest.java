package com.codemonk.common.cache;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class CacheEvictionServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private CacheEvictionService cacheEvictionService;

    @BeforeEach
    void setUp() {
        cacheEvictionService = new CacheEvictionService(redisTemplate);
    }

    @Test
    void shouldNotEvictWhenCacheNameIsNull() {
        cacheEvictionService.evictSingleCacheEntry(null, "123");

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldNotEvictWhenKeyIsNull() {
        cacheEvictionService.evictSingleCacheEntry("users", null);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldNotDeleteWhenCacheHasNoEntries() {
        when(redisTemplate.keys("users::*")).thenReturn(Set.of());

        cacheEvictionService.evictAllCacheEntries("users");

        verify(redisTemplate).keys("users::*");
        verifyNoMoreInteractions(redisTemplate);
    }
}
