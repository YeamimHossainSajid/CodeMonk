package com.codemonk.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class KafkaTopicConfigTest {

    private static final Set<String> STANDARD_NAMES =
            Set.of("repository-events", "analysis-events", "knowledge-events", "search-events");

    private Map<String, NewTopic> topicBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(KafkaTopicConfig.class)) {
            return context.getBeansOfType(NewTopic.class);
        }
    }

    @Test
    void shouldDefineTheFourStandardTopics() {
        Set<String> names = topicBeans().values().stream().map(NewTopic::name).collect(Collectors.toSet());

        assertEquals(STANDARD_NAMES, names);
    }

    @Test
    void shouldRegisterOneBeanPerTopicUnderItsMethodName() {
        Map<String, NewTopic> beans = topicBeans();

        assertEquals(4, beans.size());
        assertEquals(
                Set.of("repositoryEventsTopic", "analysisEventsTopic", "knowledgeEventsTopic", "searchEventsTopic"),
                beans.keySet());
    }

    @Test
    void shouldUseThreePartitionsAndOneReplicaForEveryTopic() {
        for (NewTopic topic : topicBeans().values()) {
            assertEquals(3, topic.numPartitions(), topic.name());
            assertEquals((short) 1, topic.replicationFactor(), topic.name());
        }
    }

    @Test
    void shouldNameEveryTopicInLowerKebabCaseEndingInEvents() {
        for (NewTopic topic : topicBeans().values()) {
            assertTrue(topic.name().matches("[a-z]+-events"), topic.name());
        }
    }

    @Test
    void shouldExposeConstantsThatMatchTheTopicNames() {
        KafkaTopicConfig config = new KafkaTopicConfig();

        assertEquals(KafkaTopicConfig.REPOSITORY_EVENTS, config.repositoryEventsTopic().name());
        assertEquals(KafkaTopicConfig.ANALYSIS_EVENTS, config.analysisEventsTopic().name());
        assertEquals(KafkaTopicConfig.KNOWLEDGE_EVENTS, config.knowledgeEventsTopic().name());
        assertEquals(KafkaTopicConfig.SEARCH_EVENTS, config.searchEventsTopic().name());
    }
}