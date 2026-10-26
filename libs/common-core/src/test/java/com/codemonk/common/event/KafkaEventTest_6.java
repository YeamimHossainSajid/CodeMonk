package com.codemonk.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaEventTest_6 {

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-10-11T12:00:00Z");

    private final ObjectMapper reader = new ObjectMapper();

    private JsonEventSerializer serializer;

    public record Snapshot(String name, Instant takenAt) {
    }

    @BeforeEach
    void setUp() {
        serializer = new JsonEventSerializer();
    }

    private static EventEnvelope<String> fixedEnvelope() {
        return new EventEnvelope<>("evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", "payload");
    }

    private JsonNode payloadOf(EventEnvelope<?> envelope) throws Exception {
        return reader.readTree(serializer.serialize("codemonk.events", envelope)).get("payload");
    }

    @Test
    void shouldGenerateRandomVersion4UuidsForWhitespaceOnlyIdentifiers() {
        EventEnvelope<String> envelope = new EventEnvelope<>(" \t\n", EventType.CODE_PARSED, null, "   ", "p");

        assertEquals(4, UUID.fromString(envelope.eventId()).version());
        assertEquals(4, UUID.fromString(envelope.correlationId()).version());
        assertNotEquals(envelope.eventId(), envelope.correlationId());
    }

    @Test
    void shouldHandBackTheSamePayloadInstance() {
        List<String> payload = List.of("a.java");

        EventEnvelope<List<String>> envelope = EventEnvelope.of(EventType.INDEXING_REQUESTED, payload);

        assertSame(payload, envelope.payload());
    }

    @Test
    void shouldNestAnEnvelopeInsideAnotherEnvelope() throws Exception {
        EventEnvelope<String> inner = new EventEnvelope<>(
                "inner-id", EventType.CODE_PARSED, FIXED_TIMESTAMP, "inner-corr", "inner-payload");
        EventEnvelope<EventEnvelope<String>> outer = new EventEnvelope<>(
                "outer-id", EventType.INDEXING_REQUESTED, FIXED_TIMESTAMP, "outer-corr", inner);

        JsonNode json = reader.readTree(serializer.serialize("codemonk.events", outer));
        JsonNode nested = json.get("payload");

        assertEquals("INDEXING_REQUESTED", json.get("eventType").asText());
        assertEquals(5, nested.size());
        assertEquals("inner-id", nested.get("eventId").asText());
        assertEquals("CODE_PARSED", nested.get("eventType").asText());
        assertEquals("inner-payload", nested.get("payload").asText());
    }

    @Test
    void shouldKeepNullElementsInAListPayload() throws Exception {
        EventEnvelope<List<String>> envelope = new EventEnvelope<>(
                "evt-1", EventType.INDEXING_REQUESTED, FIXED_TIMESTAMP, "corr-1", Arrays.asList("a", null, "c"));

        JsonNode payload = payloadOf(envelope);

        assertEquals(3, payload.size());
        assertTrue(payload.get(1).isNull());
        assertEquals("c", payload.get(2).asText());
    }

    @Test
    void shouldKeepTheInsertionOrderOfAMapPayload() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("zulu", 1);
        map.put("alpha", 2);
        map.put("mike", 3);
        EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                "evt-1", EventType.DOCUMENTATION_GENERATED, FIXED_TIMESTAMP, "corr-1", map);

        List<String> names = new ArrayList<>();
        payloadOf(envelope).fieldNames().forEachRemaining(names::add);

        assertEquals(List.of("zulu", "alpha", "mike"), names);
    }

    @Test
    void shouldWriteAnEnumPayloadAsItsName() throws Exception {
        EventEnvelope<EventType> envelope = new EventEnvelope<>(
                "evt-1", EventType.KNOWLEDGE_GRAPH_UPDATED, FIXED_TIMESTAMP, "corr-1", EventType.SEARCH_INDEX_UPDATED);

        JsonNode payload = payloadOf(envelope);

        assertTrue(payload.isTextual());
        assertEquals("SEARCH_INDEX_UPDATED", payload.asText());
    }

    @Test
    void shouldWriteAnInstantInsideAPayloadAsANumber() throws Exception {
        EventEnvelope<Snapshot> envelope = new EventEnvelope<>(
                "evt-1", EventType.REPOSITORY_INGESTED, FIXED_TIMESTAMP, "corr-1", new Snapshot("main", FIXED_TIMESTAMP));

        JsonNode payload = payloadOf(envelope);

        assertEquals("main", payload.get("name").asText());
        assertTrue(payload.get("takenAt").isNumber());
        assertEquals(FIXED_TIMESTAMP.getEpochSecond(), payload.get("takenAt").asLong());
    }

    @Test
    void shouldWriteAByteArrayPayloadAsBase64Text() throws Exception {
        byte[] bytes = {1, 2, 3, 4, 5};
        EventEnvelope<byte[]> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", bytes);

        JsonNode payload = payloadOf(envelope);

        assertTrue(payload.isTextual());
        assertArrayEquals(bytes, Base64.getDecoder().decode(payload.asText()));
    }

    @Test
    void shouldAcceptAnEmptyStringPayload() throws Exception {
        EventEnvelope<String> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", "");

        JsonNode payload = payloadOf(envelope);

        assertTrue(payload.isTextual());
        assertEquals("", payload.asText());
    }

    @Test
    void shouldWrapAnUnserializablePayloadAndKeepTheJacksonCause() {
        SerializationException exception = assertThrows(SerializationException.class,
                () -> serializer.serialize("codemonk.broken", new Object()));

        assertTrue(exception.getMessage().contains("[codemonk.broken]"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof JsonProcessingException);
    }

    @Test
    void shouldAcceptANullTopicForSuccessAndFailure() {
        assertNotNull(serializer.serialize(null, fixedEnvelope()));

        SerializationException exception = assertThrows(SerializationException.class,
                () -> serializer.serialize(null, new Object()));

        assertTrue(exception.getMessage().contains("[null]"));
    }

    @Test
    void shouldKeepSerializingAfterAFailedSerialization() {
        assertThrows(SerializationException.class, () -> serializer.serialize("codemonk.broken", new Object()));

        assertNotNull(serializer.serialize("codemonk.events", fixedEnvelope()));
    }
}