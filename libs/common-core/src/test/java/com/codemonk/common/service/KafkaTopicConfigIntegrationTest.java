package com.codemonk.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.codemonk.common.test.AbstractBaseIntegrationTest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.KafkaContainer;

/**
 * Creates the standard topics on a real broker. Skipped when Docker is not available.
 */
class KafkaTopicConfigIntegrationTest extends AbstractBaseIntegrationTest {

    @Autowired
    private KafkaContainer kafka;

    @Test
    void shouldCreateEveryStandardTopicWithThreePartitionsAndOneReplica() throws Exception {
        KafkaTopicConfig config = new KafkaTopicConfig();
        List<NewTopic> topics = List.of(
                config.repositoryEventsTopic(),
                config.analysisEventsTopic(),
                config.knowledgeEventsTopic(),
                config.searchEventsTopic());
        List<String> names = topics.stream().map(NewTopic::name).toList();

        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(topics).all().get(30, TimeUnit.SECONDS);

            Map<String, TopicDescription> described = admin.describeTopics(names).allTopicNames().get(30, TimeUnit.SECONDS);

            for (String name : names) {
                TopicDescription description = described.get(name);
                assertNotNull(description, name);
                assertEquals(3, description.partitions().size(), name);
                assertTrue(description.partitions().stream().allMatch(p -> p.replicas().size() == 1), name);
            }
        }
    }
}