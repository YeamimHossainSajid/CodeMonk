package com.codemonk.common.test;

import com.codemonk.common.config.TestcontainersConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real Spring context and real infrastructure.
 *
 * <p>It starts a Spring Boot test context and imports {@link TestcontainersConfig}, so a Redis Stack
 * and a Kafka container are started and their connection details are applied to
 * {@code spring.data.redis.*} and {@code spring.kafka.*} automatically. Tests are skipped when Docker
 * is not available.
 *
 * <p>common-core has no application class of its own, so the context is built from a minimal
 * configuration nested in this class. The DataSource and JPA auto-configurations are excluded because
 * no database container is defined here. A subclass that needs more can add its own
 * {@code @Import} or {@code @SpringBootTest(classes = ...)}.
 *
 * <pre>{@code
 * class CacheIntegrationTest extends AbstractBaseIntegrationTest {
 *
 *     @Autowired
 *     private StringRedisTemplate redisTemplate;
 * }
 * }</pre>
 *
 * <p>Spring caches the context, so every subclass with the same configuration shares one set of
 * containers.
 */
@SpringBootTest(classes = AbstractBaseIntegrationTest.IntegrationTestApplication.class)
@Import(TestcontainersConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractBaseIntegrationTest {

    /**
     * Minimal application that gives {@code @SpringBootTest} something to start.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class
    })
    public static class IntegrationTestApplication {
    }
}