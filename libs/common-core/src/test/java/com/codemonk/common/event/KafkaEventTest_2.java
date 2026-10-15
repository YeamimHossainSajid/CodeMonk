package com.codemonk.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaEventTest_2 {

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-10-11T12:00:00Z");

    private final ObjectMapper reader = new ObjectMapper();

    private JsonEventSerializer serializer;

    public record CodeFile(String path, int lines) {
    }

    @BeforeEach
    void setUp() {
        serializer = new JsonEventSerializer();
    }

    private static EventEnvelope<String> fixedEnvelope() {
        return new EventEnvelope<>("evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", "payload");
    }

    @Test
    void shouldRejectNullEventTypeWithAClearMessage() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> EventEnvelope.of(null, "payload"));

        assertEquals("eventType must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullPayloadWithAClearMessage() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> EventEnvelope.of(EventType.CODE_PARSED, null));

        assertEquals("payload must not be null", exception.getMessage());
    }

    @Test
    void shouldGenerateDistinctIdentifiersForEveryEnvelope() {
        Set<String> eventIds = new HashSet<>();
        Set<String> correlationIds = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            EventEnvelope<String> envelope = EventEnvelope.of(EventType.CODE_PARSED, "payload");
            eventIds.add(envelope.eventId());
            correlationIds.add(envelope.correlationId());
        }

        assertEquals(50, eventIds.size());
        assertEquals(50, correlationIds.size());
    }

    @Test
    void shouldCompareEnvelopesByValue() {
        EventEnvelope<String> first = fixedEnvelope();
        EventEnvelope<String> second = fixedEnvelope();
        EventEnvelope<String> different = new EventEnvelope<>("evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", "other");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
    }

    @Test
    void shouldReturnNullBytesForNullDataWhenHeadersAreSupplied() {
        assertNull(serializer.serialize("codemonk.events", new RecordHeaders(), null));
    }

    @Test
    void shouldProduceTheSameBytesWithAndWithoutHeaders() {
        EventEnvelope<String> envelope = fixedEnvelope();

        assertArrayEquals(
                serializer.serialize("codemonk.events", envelope),
                serializer.serialize("codemonk.events", new RecordHeaders(), envelope));
    }

    @Test
    void shouldLeaveSuppliedHeadersUntouched() {
        Headers headers = new RecordHeaders();
        headers.add("trace-id", "abc".getBytes(StandardCharsets.UTF_8));

        serializer.serialize("codemonk.events", headers, fixedEnvelope());

        assertEquals(1, headers.toArray().length);
        assertNotNull(headers.lastHeader("trace-id"));
    }

    @Test
    void shouldFallBackToADefaultMapperThatHandlesInstantWhenNullIsSupplied() throws Exception {
        JsonEventSerializer fallback = new JsonEventSerializer(null);

        JsonNode json = reader.readTree(fallback.serialize("codemonk.events", fixedEnvelope()));

        assertEquals(5, json.size());
        assertTrue(json.get("timestamp").isNumber());
    }

    @Test
    void shouldKeepWorkingAfterConfigureAndClose() {
        assertDoesNotThrow(() -> serializer.configure(Map.of(), false));
        assertDoesNotThrow(() -> serializer.configure(Map.of("any", "thing"), true));
        assertDoesNotThrow(() -> serializer.close());

        assertNotNull(serializer.serialize("codemonk.events", "still works"));
    }

    @Test
    void shouldSerializeARecordPayloadAsANestedObject() throws Exception {
        EventEnvelope<CodeFile> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", new CodeFile("src/Main.java", 42));

        JsonNode payload = reader.readTree(serializer.serialize("codemonk.code-parsed", envelope)).get("payload");

        assertEquals("src/Main.java", payload.get("path").asText());
        assertEquals(42, payload.get("lines").asInt());
    }

    @Test
    void shouldSerializeAListPayloadAsAnArray() throws Exception {
        EventEnvelope<List<String>> envelope = new EventEnvelope<>(
                "evt-1", EventType.INDEXING_REQUESTED, FIXED_TIMESTAMP, "corr-1", List.of("a.java", "b.java"));

        JsonNode payload = reader.readTree(serializer.serialize("codemonk.indexing", envelope)).get("payload");

        assertTrue(payload.isArray());
        assertEquals(2, payload.size());
        assertEquals("b.java", payload.get(1).asText());
    }

    @Test
    void shouldAcceptAnEmptyButNonNullPayload() throws Exception {
        EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                "evt-1", EventType.DOCUMENTATION_GENERATED, FIXED_TIMESTAMP, "corr-1", Map.of());

        JsonNode payload = reader.readTree(serializer.serialize("codemonk.docs", envelope)).get("payload");

        assertTrue(payload.isObject());
        assertEquals(0, payload.size());
    }
}