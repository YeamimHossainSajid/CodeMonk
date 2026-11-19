package com.codemonk.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.codemonk.common.test.AbstractBaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.KafkaContainer;

/**
 * Sends an event to a real broker with the shared producer settings. Skipped when Docker is not available.
 */
class KafkaProducerConfigIntegrationTest extends AbstractBaseIntegrationTest {

    @Autowired
    private KafkaContainer kafka;

    @Test
    void shouldDeliverAnEnvelopeToARealBrokerAsJsonKeyedByCorrelationId() throws Exception {
        String topic = "producer-config-" + UUID.randomUUID();
        EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, Instant.parse("2026-10-11T12:00:00Z"), "corr-1", Map.of("file", "Main.java"));

        try (KafkaProducer<String, Object> producer =
                     new KafkaProducer<>(KafkaProducerConfig.producerProperties(kafka.getBootstrapServers()))) {
            RecordMetadata metadata =
                    producer.send(new ProducerRecord<>(topic, envelope.correlationId(), envelope)).get(30, TimeUnit.SECONDS);
            assertEquals(topic, metadata.topic());
        }

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "producer-config-test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConsumerRecord<String, String> received = null;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (received == null && System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    received = record;
                }
            }
        }

        assertNotNull(received);
        assertEquals("corr-1", received.key());
        JsonNode json = new ObjectMapper().readTree(received.value());
        assertEquals("evt-1", json.get("eventId").asText());
        assertEquals("CODE_PARSED", json.get("eventType").asText());
        assertEquals("Main.java", json.get("payload").get("file").asText());
    }
}