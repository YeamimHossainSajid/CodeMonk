package com.codemonk.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaEventTest_1 {

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-10-11T12:00:00Z");

    private final ObjectMapper reader = new ObjectMapper();

    private JsonEventSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JsonEventSerializer();
    }

    @Test
    void shouldReplaceBlankIdentifiersWithGeneratedUuids() {
        EventEnvelope<String> envelope = new EventEnvelope<>("  ", EventType.INDEXING_REQUESTED, FIXED_TIMESTAMP, "", "payload");

        assertFalse(envelope.eventId().isBlank());
        assertFalse(envelope.correlationId().isBlank());
        assertDoesNotThrow(() -> UUID.fromString(envelope.eventId()));
        assertDoesNotThrow(() -> UUID.fromString(envelope.correlationId()));
        assertNotEquals(envelope.eventId(), envelope.correlationId());
    }

    @Test
    void shouldReplaceNullIdentifiersAndTimestampWithGeneratedValues() {
        EventEnvelope<String> envelope = new EventEnvelope<>(null, EventType.CODE_PARSED, null, null, "payload");

        assertDoesNotThrow(() -> UUID.fromString(envelope.eventId()));
        assertDoesNotThrow(() -> UUID.fromString(envelope.correlationId()));
        assertNotNull(envelope.timestamp());
    }

    @Test
    void shouldKeepExplicitValuesUnchanged() {
        EventEnvelope<String> envelope = new EventEnvelope<>("evt-1", EventType.REPOSITORY_INGESTED, FIXED_TIMESTAMP, "corr-1", "payload");

        assertEquals("evt-1", envelope.eventId());
        assertEquals(FIXED_TIMESTAMP, envelope.timestamp());
        assertEquals("corr-1", envelope.correlationId());
    }

    @Test
    void shouldSerializeEnvelopeWithExactlyTheFiveWireFields() throws Exception {
        EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", Map.of("file", "Main.java"));

        JsonNode json = reader.readTree(serializer.serialize("codemonk.code-parsed", envelope));

        assertEquals(5, json.size());
        assertEquals("evt-1", json.get("eventId").asText());
        assertEquals("CODE_PARSED", json.get("eventType").asText());
        assertEquals("corr-1", json.get("correlationId").asText());
        assertEquals("Main.java", json.get("payload").get("file").asText());
        assertTrue(json.has("timestamp"));
    }

    @ParameterizedTest
    @EnumSource(EventType.class)
    void shouldSerializeEveryEventTypeUnderItsOwnName(EventType eventType) throws Exception {
        EventEnvelope<String> envelope = EventEnvelope.of(eventType, "payload");

        JsonNode json = reader.readTree(serializer.serialize("codemonk.events", envelope));

        assertEquals(eventType.name(), json.get("eventType").asText());
    }

    @Test
    void shouldProduceTheSameBytesRegardlessOfTopicName() {
        EventEnvelope<String> envelope = new EventEnvelope<>("evt-1", EventType.KNOWLEDGE_GRAPH_UPDATED, FIXED_TIMESTAMP, "corr-1", "graph");

        assertArrayEquals(
                serializer.serialize("codemonk.topic-a", envelope),
                serializer.serialize("codemonk.topic-b", envelope));
    }

    @Test
    void shouldNameTheTopicWhenThePayloadCannotBeSerialized() {
        Object unserializable = new Object();

        SerializationException exception = assertThrows(SerializationException.class,
                () -> serializer.serialize("codemonk.broken", unserializable));

        assertTrue(exception.getMessage().contains("codemonk.broken"));
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldUseTheCustomObjectMapperWhenOneIsSupplied() throws Exception {
        ObjectMapper isoMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonEventSerializer isoSerializer = new JsonEventSerializer(isoMapper);
        EventEnvelope<String> envelope = new EventEnvelope<>("evt-2", EventType.SEARCH_INDEX_UPDATED, FIXED_TIMESTAMP, "corr-2", "reindexed");

        JsonNode json = reader.readTree(isoSerializer.serialize("codemonk.search", envelope));

        assertEquals("2026-10-11T12:00:00Z", json.get("timestamp").asText());
    }
}