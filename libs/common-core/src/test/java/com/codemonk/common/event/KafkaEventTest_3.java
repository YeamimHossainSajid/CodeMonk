package com.codemonk.common.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaEventTest_3 {

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-10-11T12:00:00Z");

    private final ObjectMapper reader = new ObjectMapper().registerModule(new JavaTimeModule());

    private JsonEventSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JsonEventSerializer();
    }

    private static <T> EventEnvelope<T> envelopeOf(T payload) {
        return new EventEnvelope<>("evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", payload);
    }

    @Test
    void shouldRoundTripAnEnvelopeBackIntoAnEqualEnvelope() throws Exception {
        EventEnvelope<Map<String, Object>> original = envelopeOf(Map.of("file", "Main.java", "lines", 42));

        byte[] bytes = serializer.serialize("codemonk.code-parsed", original);
        EventEnvelope<Map<String, Object>> restored =
                reader.readValue(bytes, new TypeReference<EventEnvelope<Map<String, Object>>>() { });

        assertEquals(original, restored);
    }

    @Test
    void shouldWriteNonAsciiTextAsUtf8() {
        String text = "\u00dcn\u00efc\u00f6d\u00e9 \u65e5\u672c\u8a9e";

        String json = new String(serializer.serialize("codemonk.events", envelopeOf(text)), StandardCharsets.UTF_8);

        assertTrue(json.contains(text));
    }

    @Test
    void shouldEscapeQuotesAndNewlinesSoTheJsonStaysValid() throws Exception {
        String awkward = "say \"hi\"\nthen\ttab and back\\slash";

        JsonNode json = reader.readTree(serializer.serialize("codemonk.events", envelopeOf(awkward)));

        assertEquals(awkward, json.get("payload").asText());
    }

    @Test
    void shouldKeepNullValuesInsideAMapPayload() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("note", null);
        payload.put("file", "Main.java");

        JsonNode node = reader.readTree(serializer.serialize("codemonk.events", envelopeOf(payload))).get("payload");

        assertTrue(node.has("note"));
        assertTrue(node.get("note").isNull());
        assertEquals("Main.java", node.get("file").asText());
    }

    @Test
    void shouldSerializeALargePayload() throws Exception {
        List<String> files = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            files.add("file-" + i + ".java");
        }

        JsonNode payload = reader.readTree(serializer.serialize("codemonk.indexing", envelopeOf(files))).get("payload");

        assertEquals(10_000, payload.size());
        assertEquals("file-9999.java", payload.get(9_999).asText());
    }

    @Test
    void shouldSerializeTheSameEnvelopeFromManyThreadsAtOnce() throws Exception {
        EventEnvelope<String> envelope = envelopeOf("payload");
        byte[] expected = serializer.serialize("codemonk.events", envelope);
        List<Callable<byte[]>> tasks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            tasks.add(() -> serializer.serialize("codemonk.events", envelope));
        }

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            for (Future<byte[]> result : executor.invokeAll(tasks)) {
                assertArrayEquals(expected, result.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @EnumSource(EventType.class)
    void shouldNameEveryEventTypeInUpperSnakeCaseAndResolveItByName(EventType eventType) {
        assertTrue(eventType.name().matches("[A-Z]+(_[A-Z]+)*"));
        assertEquals(eventType, EventType.valueOf(eventType.name()));
    }

    @Test
    void shouldRejectALowerCaseEventTypeName() {
        assertThrows(IllegalArgumentException.class, () -> EventType.valueOf("code_parsed"));
    }

    @Test
    void shouldRejectAnUnknownEventTypeName() {
        assertThrows(IllegalArgumentException.class, () -> EventType.valueOf("NOT_AN_EVENT"));
    }
}