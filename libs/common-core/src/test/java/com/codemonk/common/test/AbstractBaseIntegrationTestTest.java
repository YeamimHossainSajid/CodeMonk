package com.codemonk.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.KafkaContainer;

/**
 * Checks that a subclass of {@link AbstractBaseIntegrationTest} gets a working context and containers.
 * Skipped when Docker is not available.
 */
class AbstractBaseIntegrationTestTest extends AbstractBaseIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private KafkaContainer kafka;

    @Test
    @DisplayName("Should start the Spring context")
    void shouldStartTheSpringContext() {
        assertNotNull(context);
    }

    @Test
    @DisplayName("Should reach Redis through the container")
    void shouldReachRedisThroughTheContainer() {
        String key = "integration-" + UUID.randomUUID();

        redisTemplate.opsForValue().set(key, "value");

        assertEquals("value", redisTemplate.opsForValue().get(key));
        redisTemplate.delete(key);
    }

    @Test
    @DisplayName("Should connect to the mapped Redis port, not the default one")
    void shouldUseTheMappedRedisPort() {
        LettuceConnectionFactory lettuce = assertInstanceOf(LettuceConnectionFactory.class, redisConnectionFactory);

        assertNotEquals(6379, lettuce.getPort());
    }

    @Test
    @DisplayName("Should start the Kafka container")
    void shouldStartTheKafkaContainer() {
        assertTrue(kafka.isRunning());
        assertFalse(kafka.getBootstrapServers().isBlank());
    }

    @Test
    @DisplayName("Should not create a DataSource")
    void shouldNotCreateADataSource() {
        assertTrue(context.getBeansOfType(DataSource.class).isEmpty());
    }
}