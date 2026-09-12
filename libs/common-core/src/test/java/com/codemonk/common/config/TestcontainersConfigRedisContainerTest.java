package com.codemonk.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Starts the real Redis Stack container. Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
class TestcontainersConfigRedisContainerTest {

    @Container
    static final GenericContainer<?> redis = TestcontainersConfig.redisContainer();

    @Test
    @DisplayName("Should accept Redis commands on the mapped port")
    void shouldAcceptRedisCommands() {

        RedisClient client = RedisClient.create(
                RedisURI.create(redis.getHost(), redis.getMappedPort(TestcontainersConfig.REDIS_PORT)));
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> commands = connection.sync();

            assertEquals("PONG", commands.ping());
            commands.set("testcontainers:key", "value");
            assertEquals("value", commands.get("testcontainers:key"));
        } finally {
            client.shutdown();
        }
    }

    @Test
    @DisplayName("Should load the RedisJSON module from Redis Stack")
    void shouldLoadRedisJsonModule() throws Exception {

        ExecResult set = redis.execInContainer("redis-cli", "JSON.SET", "testcontainers:doc", "$", "{\"id\":42}");
        ExecResult get = redis.execInContainer("redis-cli", "JSON.GET", "testcontainers:doc", "$.id");

        assertEquals("OK", set.getStdout().trim());
        assertEquals("[42]", get.getStdout().trim());
    }
}
