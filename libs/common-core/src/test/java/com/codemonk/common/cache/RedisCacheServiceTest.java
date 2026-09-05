package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    private static final RedisConnectionFailureException REDIS_DOWN =
            new RedisConnectionFailureException("connection refused");

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new RedisCacheService(redisTemplate);
    }

    @Nested
    @DisplayName("generateKey")
    class GenerateKey {

        @Test
        void shouldJoinPrefixAndIdentifierWithColon() {
            assertEquals("user:123", cacheService.generateKey("user", "123"));
        }

        @Test
        void shouldRejectBlankPrefix() {
            assertThrows(IllegalArgumentException.class, () -> cacheService.generateKey("  ", "123"));
        }

        @Test
        void shouldRejectNullIdentifier() {
            assertThrows(IllegalArgumentException.class, () -> cacheService.generateKey("user", null));
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        void shouldReturnCachedValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("user:123")).thenReturn("monk");

            assertEquals(Optional.of("monk"), cacheService.get("user:123"));
        }

        @Test
        void shouldReturnEmptyOnMiss() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("user:404")).thenReturn(null);

            assertTrue(cacheService.get("user:404").isEmpty());
        }

        @Test
        void shouldReturnEmptyWhenRedisIsUnavailable() {
            when(redisTemplate.opsForValue()).thenThrow(REDIS_DOWN);

            assertTrue(cacheService.get("user:123").isEmpty());
        }

        @Test
        void shouldRejectBlankKey() {
            assertThrows(IllegalArgumentException.class, () -> cacheService.get(""));
        }
    }

    @Nested
    @DisplayName("get with type")
    class GetTyped {

        @Test
        void shouldReturnValueOfRequestedType() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("user:123")).thenReturn("monk");

            assertEquals(Optional.of("monk"), cacheService.get("user:123", String.class));
        }

        @Test
        void shouldReturnEmptyWhenStoredValueHasAnotherType() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("user:123")).thenReturn(42);

            assertTrue(cacheService.get("user:123", String.class).isEmpty());
        }

        @Test
        void shouldRejectNullType() {
            assertThrows(IllegalArgumentException.class, () -> cacheService.get("user:123", null));
        }
    }

    @Nested
    @DisplayName("put")
    class Put {

        @Test
        void shouldStoreValueWithoutTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            assertTrue(cacheService.put("user:123", "monk"));

            verify(valueOperations).set("user:123", "monk");
        }

        @Test
        void shouldStoreValueWithTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            Duration ttl = Duration.ofMinutes(5);

            assertTrue(cacheService.put("user:123", "monk", ttl));

            verify(valueOperations).set("user:123", "monk", ttl);
        }

        @Test
        void shouldReportFailureWhenRedisIsUnavailable() {
            when(redisTemplate.opsForValue()).thenThrow(REDIS_DOWN);

            assertFalse(cacheService.put("user:123", "monk"));
        }

        @Test
        void shouldReportFailureWhenRedisIsUnavailableWithTtl() {
            when(redisTemplate.opsForValue()).thenThrow(REDIS_DOWN);

            assertFalse(cacheService.put("user:123", "monk", Duration.ofMinutes(5)));
        }

        @Test
        void shouldRejectNonPositiveTtl() {
            assertThrows(IllegalArgumentException.class,
                    () -> cacheService.put("user:123", "monk", Duration.ZERO));
            assertThrows(IllegalArgumentException.class,
                    () -> cacheService.put("user:123", "monk", Duration.ofSeconds(-1)));
            assertThrows(IllegalArgumentException.class,
                    () -> cacheService.put("user:123", "monk", null));
        }
    }

    @Nested
    @DisplayName("evict")
    class Evict {

        @Test
        void shouldReportRemovedEntry() {
            when(redisTemplate.delete("user:123")).thenReturn(Boolean.TRUE);

            assertTrue(cacheService.evict("user:123"));
        }

        @Test
        void shouldReportMissingEntry() {
            when(redisTemplate.delete("user:404")).thenReturn(Boolean.FALSE);

            assertFalse(cacheService.evict("user:404"));
        }

        @Test
        void shouldReportFailureWhenRedisIsUnavailable() {
            when(redisTemplate.delete("user:123")).thenThrow(REDIS_DOWN);

            assertFalse(cacheService.evict("user:123"));
        }
    }

    @Nested
    @DisplayName("hasKey")
    class HasKey {

        @Test
        void shouldReportPresentKey() {
            when(redisTemplate.hasKey("user:123")).thenReturn(Boolean.TRUE);

            assertTrue(cacheService.hasKey("user:123"));
        }

        @Test
        void shouldReportAbsentKey() {
            when(redisTemplate.hasKey("user:404")).thenReturn(Boolean.FALSE);

            assertFalse(cacheService.hasKey("user:404"));
        }

        @Test
        void shouldReportFalseWhenRedisIsUnavailable() {
            when(redisTemplate.hasKey("user:123")).thenThrow(REDIS_DOWN);

            assertFalse(cacheService.hasKey("user:123"));
        }
    }
}
