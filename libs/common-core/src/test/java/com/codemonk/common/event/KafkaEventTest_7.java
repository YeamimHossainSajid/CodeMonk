package com.codemonk.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaEventTest_7 {

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-10-11T12:00:00Z");

    private final ObjectMapper reader = new ObjectMapper();

    private JsonEventSerializer serializer;

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
    void shouldWriteTheTimestampAsIsoTextWhenTheMapperDisablesNumericDates() throws Exception {
        ObjectMapper isoMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonEventSerializer isoSerializer = new JsonEventSerializer(isoMapper);

        JsonNode json = reader.readTree(isoSerializer.serialize("codemonk.events", fixedEnvelope()));

        assertTrue(json.get("timestamp").isTextual());
        assertEquals("2026-10-11T12:00:00Z", json.get("timestamp").asText());
    }

    @Test
    void shouldProduceTheSameJsonTreeWhenTheMapperIndentsItsOutput() throws Exception {
        ObjectMapper indenting = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
        JsonEventSerializer indentedSerializer = new JsonEventSerializer(indenting);

        byte[] indented = indentedSerializer.serialize("codemonk.events", fixedEnvelope());
        byte[] compact = serializer.serialize("codemonk.events", fixedEnvelope());

        assertTrue(new String(indented, StandardCharsets.UTF_8).contains("\n"));
        assertEquals(reader.readTree(compact), reader.readTree(indented));
    }

    @Test
    void shouldWrapARuntimeExceptionThrownByTheMapper() {
        IllegalStateException boom = new IllegalStateException("boom");
        ObjectMapper failing = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) {
                throw boom;
            }
        };
        JsonEventSerializer failingSerializer = new JsonEventSerializer(failing);

        SerializationException exception = assertThrows(SerializationException.class,
                () -> failingSerializer.serialize("codemonk.events", fixedEnvelope()));

        assertSame(boom, exception.getCause());
        assertTrue(exception.getMessage().contains("[codemonk.events]"));
    }

    @Test
    void shouldWrapACheckedJsonExceptionThrownByTheMapper() {
        JsonProcessingException boom = new JsonProcessingException("boom") {
        };
        ObjectMapper failing = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
                throw boom;
            }
        };
        JsonEventSerializer failingSerializer = new JsonEventSerializer(failing);

        SerializationException exception = assertThrows(SerializationException.class,
                () -> failingSerializer.serialize("codemonk.events", fixedEnvelope()));

        assertSame(boom, exception.getCause());
    }

    @Test
    void shouldKeepSurroundingSpacesOfAnExplicitNonBlankIdentifier() {
        EventEnvelope<String> envelope = new EventEnvelope<>(
                "  id  ", EventType.CODE_PARSED, FIXED_TIMESTAMP, "  corr  ", "p");

        assertEquals("  id  ", envelope.eventId());
        assertEquals("  corr  ", envelope.correlationId());
    }

    @Test
    void shouldRejectANullPayloadWhenBuiltWithTheCorrelationIdFactory() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> EventEnvelope.of(EventType.CODE_PARSED, "corr-1", null));

        assertEquals("payload must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectANullEventTypeWhenBuiltWithTheCorrelationIdFactory() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> EventEnvelope.of(null, "corr-1", "payload"));

        assertEquals("eventType must not be null", exception.getMessage());
    }

    @Test
    void shouldWriteNonStringMapKeysAsFieldNames() throws Exception {
        Map<Object, String> map = new LinkedHashMap<>();
        map.put(1, "one");
        map.put(EventType.CODE_PARSED, "parsed");
        EventEnvelope<Map<Object, String>> envelope = new EventEnvelope<>(
                "evt-1", EventType.DOCUMENTATION_GENERATED, FIXED_TIMESTAMP, "corr-1", map);

        JsonNode payload = payloadOf(envelope);

        assertEquals("one", payload.get("1").asText());
        assertEquals("parsed", payload.get("CODE_PARSED").asText());
    }

    @Test
    void shouldSerializeASetPayloadAsAnArrayInIterationOrder() throws Exception {
        Set<String> set = new LinkedHashSet<>(List.of("b", "a", "c"));
        EventEnvelope<Set<String>> envelope = new EventEnvelope<>(
                "evt-1", EventType.INDEXING_REQUESTED, FIXED_TIMESTAMP, "corr-1", set);

        JsonNode payload = payloadOf(envelope);

        assertTrue(payload.isArray());
        assertEquals(3, payload.size());
        assertEquals("b", payload.get(0).asText());
        assertEquals("c", payload.get(2).asText());
    }

    @Test
    void shouldSerializeAPrimitiveArrayPayloadAsAJsonArray() throws Exception {
        EventEnvelope<int[]> envelope = new EventEnvelope<>(
                "evt-1", EventType.SEARCH_INDEX_UPDATED, FIXED_TIMESTAMP, "corr-1", new int[] {3, 1, 2});

        JsonNode payload = payloadOf(envelope);

        assertTrue(payload.isArray());
        assertEquals(3, payload.size());
        assertEquals(1, payload.get(1).asInt());
    }

    @Test
    void shouldKeepEveryDigitOfABigDecimalPayload() {
        BigDecimal value = new BigDecimal("12345678901234567890.123456789");
        EventEnvelope<BigDecimal> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", value);

        String json = new String(serializer.serialize("codemonk.events", envelope), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"payload\":12345678901234567890.123456789"));
    }

    @Test
    void shouldSerializeAFiftyLevelNestedListPayload() throws Exception {
        Object nested = "leaf";
        for (int i = 0; i < 50; i++) {
            nested = List.of(nested);
        }
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                "evt-1", EventType.KNOWLEDGE_GRAPH_UPDATED, FIXED_TIMESTAMP, "corr-1", nested);

        JsonNode node = payloadOf(envelope);
        int depth = 0;
        while (node.isArray()) {
            node = node.get(0);
            depth++;
        }

        assertEquals(50, depth);
        assertEquals("leaf", node.asText());
    }
}
