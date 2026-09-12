package com.codemonk.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.testcontainers.containers.GenericContainer;

/**
 * Checks the container definition without starting Docker.
 */
class TestcontainersConfigTest {

    @Test
    @DisplayName("Should define a Redis Stack container")
    void shouldUseRedisStackImage() {

        // GenericContainer#getDockerImageName resolves the image through Docker, so check the name directly
        assertEquals("redis/redis-stack-server", TestcontainersConfig.REDIS_STACK_IMAGE.getRepository());
        assertEquals("7.2.0-v10", TestcontainersConfig.REDIS_STACK_IMAGE.getVersionPart());
    }

    @Test
    @DisplayName("Should expose the Redis port")
    void shouldExposeRedisPort() {

        try (GenericContainer<?> container = TestcontainersConfig.redisContainer()) {
            assertEquals(List.of(6379), container.getExposedPorts());
        }
    }

    @Test
    @DisplayName("Should create a new unstarted container on every call")
    void shouldCreateNewContainerEachCall() {

        try (GenericContainer<?> first = TestcontainersConfig.redisContainer();
             GenericContainer<?> second = TestcontainersConfig.redisContainer()) {
            assertNotSame(first, second);
            assertFalse(first.isRunning());
        }
    }

    @Test
    @DisplayName("Should be a test configuration without bean method proxying")
    void shouldBeTestConfiguration() {

        TestConfiguration annotation = AnnotatedElementUtils.findMergedAnnotation(
                TestcontainersConfig.class, TestConfiguration.class);

        assertNotNull(annotation);
        assertFalse(annotation.proxyBeanMethods());
    }

    @Test
    @DisplayName("Should publish the container as a Redis service connection bean")
    void shouldPublishRedisServiceConnection() throws NoSuchMethodException {

        Method beanMethod = TestcontainersConfig.class.getDeclaredMethod("redisStackContainer");
        ServiceConnection serviceConnection = AnnotatedElementUtils.findMergedAnnotation(beanMethod, ServiceConnection.class);

        assertNotNull(AnnotatedElementUtils.findMergedAnnotation(beanMethod, Bean.class));
        assertNotNull(serviceConnection);
        assertEquals("redis", serviceConnection.name());
    }
}
