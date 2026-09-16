package com.codemonk.common.cache;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around {@link RedisTemplate} that shields callers from Redis
 * failures.
 *
 * <p>The cache is treated as a best-effort optimisation: any
 * {@link DataAccessException} raised by the underlying template (connection
 * refused, timeout, serialization failure, ...) is logged and translated into a
 * miss rather than propagated, so an unavailable Redis never breaks the calling
 * service. Programming errors such as a {@code null} key are still reported as
 * {@link IllegalArgumentException}.
 */
@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private static final String KEY_SEPARATOR = ":";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Builds a namespaced cache key of the form {@code prefix:identifier}.
     */
    public String generateKey(String prefix, String identifier) {
        requireText(prefix, "prefix");
        requireText(identifier, "identifier");
        return prefix + KEY_SEPARATOR + identifier;
    }

    /**
     * Reads the raw value stored under {@code key}.
     *
     * @return the cached value, or an empty {@link Optional} on a miss or Redis
     *         failure
     */
    public Optional<Object> get(String key) {
        requireText(key, "key");
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (DataAccessException ex) {
            log.warn("Cache read failed for key '{}', treating as miss: {}", key, ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    /**
     * Reads the value stored under {@code key} and narrows it to {@code type}.
     *
     * @return the cached value, or an empty {@link Optional} on a miss, a Redis
     *         failure, or when the stored value is not of the requested type
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Cache value type must not be null");
        }
        return get(key).flatMap(value -> {
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            log.warn("Cached value for key '{}' is of type {} but {} was requested, treating as miss",
                    key, value.getClass().getName(), type.getName());
            return Optional.empty();
        });
    }

    /**
     * Stores {@code value} under {@code key} without an expiry.
     *
     * @return {@code true} when the value was written, {@code false} when Redis
     *         was unreachable
     */
    public boolean put(String key, Object value) {
        requireText(key, "key");
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (DataAccessException ex) {
            log.warn("Cache write failed for key '{}': {}", key, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Stores {@code value} under {@code key} with the given time-to-live.
     *
     * @return {@code true} when the value was written, {@code false} when Redis
     *         was unreachable
     */
    public boolean put(String key, Object value, Duration ttl) {
        requireText(key, "key");
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("Cache ttl must be a positive duration");
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            return true;
        } catch (DataAccessException ex) {
            log.warn("Cache write failed for key '{}' with ttl {}: {}", key, ttl, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Removes the entry stored under {@code key}.
     *
     * @return {@code true} when an entry was removed, {@code false} when there
     *         was nothing to remove or Redis was unreachable
     */
    public boolean evict(String key) {
        requireText(key, "key");
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (DataAccessException ex) {
            log.warn("Cache eviction failed for key '{}': {}", key, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Checks whether {@code key} is present in the cache.
     *
     * @return {@code true} only when the key is known to exist; a Redis failure
     *         reports {@code false}
     */
    public boolean hasKey(String key) {
        requireText(key, "key");
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (DataAccessException ex) {
            log.warn("Cache lookup failed for key '{}': {}", key, ex.getMessage(), ex);
            return false;
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Cache " + name + " must not be null or blank");
        }
    }
}
