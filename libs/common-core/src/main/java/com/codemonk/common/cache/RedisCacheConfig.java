package com.codemonk.common.cache;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Spring cache setup backed by Redis.
 *
 * <p>Registers a {@link RedisCacheManager} so {@code @Cacheable} and the other cache annotations store
 * their entries in Redis. Keys are plain strings, values are JSON written with the application's
 * {@link ObjectMapper} (the same serializer as {@link RedisTemplateConfig}), every entry expires after
 * {@link #DEFAULT_TTL}, and {@code null} results are not cached.
 *
 * <p>Like {@link RedisTemplateConfig}, values are stored as plain JSON without class information, so a
 * cached object is read back as the JSON structure (a Map or a List) unless the caller converts it.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * How long a cache entry lives when the cache has no TTL of its own.
     */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .build();
    }
}