package com.codemonk.common.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaEventTest_5 {

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-10-11T12:00:00Z");

    private final ObjectMapper reader = new ObjectMapper().registerModule(new JavaTimeModule());

    private JsonEventSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JsonEventSerializer();
    }

    private static EventEnvelope<String> fixedEnvelope() {
        return new EventEnvelope<>("evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", "payload");
    }

    private String json(Object value) {
        return new String(serializer.serialize("codemonk.events", value), StandardCharsets.UTF_8);
    }

    @Test
    void shouldWriteCompactJsonInAFixedShape() {
        String expected = "\\{\"eventId\":\"evt-1\",\"eventType\":\"CODE_PARSED\",\"timestamp\":\\d+\\.\\d+,"
                + "\"correlationId\":\"corr-1\",\"payload\":\"payload\"\\}";

        assertTrue(json(fixedEnvelope()).matches(expected));
    }

    @Test
    void shouldKeepNanosecondPrecisionOfTheTimestampThroughARoundTrip() throws Exception {
        Instant precise = Instant.parse("2026-10-11T12:00:00.123456789Z");
        EventEnvelope<String> original = new EventEnvelope<>("evt-1", EventType.CODE_PARSED, precise, "corr-1", "payload");

        EventEnvelope<String> restored = reader.readValue(
                serializer.serialize("codemonk.events", original), new TypeReference<EventEnvelope<String>>() { });

        assertEquals(precise, restored.timestamp());
    }

    @Test
    void shouldSerializePlainValuesAsCompactJson() {
        assertEquals("\"text\"", json("text"));
        assertEquals("42", json(42));
        assertEquals("true", json(true));
        assertEquals("[1,2]", json(List.of(1, 2)));
    }

    @Test
    void shouldShowTheIdentifiersInToStringForLogging() {
        String text = fixedEnvelope().toString();

        assertTrue(text.contains("evt-1"));
        assertTrue(text.contains("corr-1"));
        assertTrue(text.contains("CODE_PARSED"));
    }

    @Test
    void shouldTreatEveryEnvelopeFieldAsPartOfEquality() {
        EventEnvelope<String> base = fixedEnvelope();

        assertNotEquals(base, new EventEnvelope<>("evt-2", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", "payload"));
        assertNotEquals(base, new EventEnvelope<>("evt-1", EventType.SEARCH_INDEX_UPDATED, FIXED_TIMESTAMP, "corr-1", "payload"));
        assertNotEquals(base, new EventEnvelope<>("evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP.plusSeconds(1), "corr-1", "payload"));
        assertNotEquals(base, new EventEnvelope<>("evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-2", "payload"));
    }
}