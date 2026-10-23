package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class RedisCacheConfigTest {

    private RedisCacheManager cacheManager;

    private RedisCacheConfiguration config;

    @BeforeEach
    void setUp() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        cacheManager = new RedisCacheConfig().cacheManager(connectionFactory, new ObjectMapper());
        cacheManager.afterPropertiesSet();
        config = assertInstanceOf(RedisCache.class, cacheManager.getCache("users")).getCacheConfiguration();
    }

    @Test
    void shouldCreateRedisCachesOnDemand() {
        assertInstanceOf(RedisCache.class, cacheManager.getCache("orders"));
    }

    @Test
    void shouldApplyTheTenMinuteDefaultTtl() {
        Duration ttl = config.getTtlFunction().getTimeToLive("key", "value");

        assertEquals(Duration.ofMinutes(10), ttl);
        assertEquals(RedisCacheConfig.DEFAULT_TTL, ttl);
    }

    @Test
    void shouldWriteKeysAsPlainStrings() {
        ByteBuffer bytes = config.getKeySerializationPair().write("user:1");

        assertEquals("user:1", StandardCharsets.UTF_8.decode(bytes).toString());
    }

    @Test
    void shouldWriteValuesAsJson() {
        ByteBuffer bytes = config.getValueSerializationPair().write(Map.of("name", "Zoya"));

        assertEquals("{\"name\":\"Zoya\"}", StandardCharsets.UTF_8.decode(bytes).toString());
    }

    @Test
    void shouldReadJsonBackIntoAMap() {
        ByteBuffer bytes = config.getValueSerializationPair().write(Map.of("name", "Zoya"));

        assertEquals(Map.of("name", "Zoya"), config.getValueSerializationPair().read(bytes));
    }

    @Test
    void shouldNotCacheNullValues() {
        assertFalse(config.getAllowCacheNullValues());
    }

    @Test
    void shouldPrefixKeysWithTheCacheName() {
        assertEquals("users::", config.getKeyPrefixFor("users"));
    }
}