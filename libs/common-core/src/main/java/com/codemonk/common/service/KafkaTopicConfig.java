package com.codemonk.common.service;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the standard CodeMonk Kafka topics as {@link NewTopic} beans.
 *
 * <p>An application with a Kafka admin (for example Spring Kafka's {@code KafkaAdmin}) creates every
 * {@code NewTopic} bean at startup, so importing this class is enough to get all four topics. The topic
 * names are public constants so that producers and consumers refer to the same string.
 *
 * <p>Every topic gets {@link #PARTITIONS} partitions and {@link #REPLICAS} replica. The replica count
 * matches the single broker in {@code docker-compose.yml}, where every replication factor is 1. A
 * deployment with more brokers should raise it.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String REPOSITORY_EVENTS = "repository-events";

    public static final String ANALYSIS_EVENTS = "analysis-events";

    public static final String KNOWLEDGE_EVENTS = "knowledge-events";

    public static final String SEARCH_EVENTS = "search-events";

    public static final int PARTITIONS = 3;

    public static final short REPLICAS = 1;

    @Bean
    public NewTopic repositoryEventsTopic() {
        return topic(REPOSITORY_EVENTS);
    }

    @Bean
    public NewTopic analysisEventsTopic() {
        return topic(ANALYSIS_EVENTS);
    }

    @Bean
    public NewTopic knowledgeEventsTopic() {
        return topic(KNOWLEDGE_EVENTS);
    }

    @Bean
    public NewTopic searchEventsTopic() {
        return topic(SEARCH_EVENTS);
    }

    private static NewTopic topic(String name) {
        return new NewTopic(name, PARTITIONS, REPLICAS);
    }
}