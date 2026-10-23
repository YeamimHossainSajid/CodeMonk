package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.codemonk.common.test.AbstractBaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Runs {@link RedisCacheConfig} against a real Redis. Skipped when Docker is not available.
 */
@Import({RedisCacheConfig.class, RedisCacheConfigIntegrationTest.Greeter.class})
class RedisCacheConfigIntegrationTest extends AbstractBaseIntegrationTest {

    public static class Greeter {

        private final AtomicInteger calls = new AtomicInteger();

        @Cacheable("greetings")
        public String greet(String name) {
            calls.incrementAndGet();
            return "Hello, " + name;
        }

        public int callCount() {
            return calls.get();
        }
    }

    @Autowired
    private Greeter greeter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldComputeOnceAndServeRepeatCallsFromRedis() {
        String name = "once-" + UUID.randomUUID();
        int before = greeter.callCount();

        String first = greeter.greet(name);
        String second = greeter.greet(name);

        assertEquals("Hello, " + name, first);
        assertEquals(first, second);
        assertEquals(before + 1, greeter.callCount());
    }

    @Test
    void shouldStoreTheEntryAsJsonWithAtMostTheDefaultTtl() {
        String name = "ttl-" + UUID.randomUUID();
        String key = "greetings::" + name;

        greeter.greet(name);

        assertEquals("\"Hello, " + name + "\"", redisTemplate.opsForValue().get(key));
        Long ttlSeconds = redisTemplate.getExpire(key);
        assertNotNull(ttlSeconds);
        assertTrue(ttlSeconds > 0 && ttlSeconds <= RedisCacheConfig.DEFAULT_TTL.toSeconds());
    }
}