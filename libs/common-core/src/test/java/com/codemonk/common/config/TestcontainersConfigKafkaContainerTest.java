package com.codemonk.common.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

//CI - Fix - 9-11-26
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Starts the real Kafka container. Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
class TestcontainersConfigKafkaContainerTest {

    @Container
    static final KafkaContainer kafka = TestcontainersConfig.kafkaContainer();

    @Test
    @DisplayName("Should accept Kafka admin operations")
    void shouldAcceptKafkaAdminOperations() throws Exception {
        String topic = "testcontainers-" + UUID.randomUUID();

        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(properties)) {
            adminClient.createTopics(List.of(new NewTopic(topic, 1, (short) 1)))
                    .all()
                    .get(30, TimeUnit.SECONDS);

            Set<String> topics = adminClient.listTopics()
                    .names()
                    .get(30, TimeUnit.SECONDS);

            assertTrue(topics.contains(topic));
        }
    }

    @Test
    @DisplayName("Should create an unstarted Kafka container")
    void shouldCreateKafkaContainer() {
        try (KafkaContainer container = TestcontainersConfig.kafkaContainer()) {
            assertFalse(container.isRunning());
        }
    }
}
