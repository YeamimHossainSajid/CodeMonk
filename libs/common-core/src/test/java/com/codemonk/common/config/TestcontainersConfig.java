package com.codemonk.common.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable Testcontainers definitions for integration tests.
 *
 * <p>Import it into a Spring Boot test to get a Redis Stack container whose connection
 * details are applied to {@code spring.data.redis.*} automatically:
 *
 * <pre>{@code
 * @SpringBootTest
 * @Import(TestcontainersConfig.class)
 * class CacheIntegrationTest { ... }
 * }</pre>
 *
 * <p>Or manage the container with the Testcontainers JUnit 5 extension:
 *
 * <pre>{@code
 * @Testcontainers(disabledWithoutDocker = true)
 * class RedisIntegrationTest {
 *
 *     @Container
 *     static GenericContainer<?> redis = TestcontainersConfig.redisContainer();
 * }
 * }</pre>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    public static final DockerImageName REDIS_STACK_IMAGE =
            DockerImageName.parse("redis/redis-stack-server:7.2.0-v10");

    public static final int REDIS_PORT = 6379;

    /**
     * Creates a new, not yet started, Redis Stack container.
     */
    public static GenericContainer<?> redisContainer() {
        return new GenericContainer<>(REDIS_STACK_IMAGE)
                .withExposedPorts(REDIS_PORT)
                .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
    }

    /**
     * The name is required because Spring Boot 3.3 only matches the plain {@code redis} image to Redis connection details.
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisStackContainer() {
        return redisContainer();
    }
}
