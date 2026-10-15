package com.codemonk.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaEventTest_4 {

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

    @Test
    void shouldWriteTheEnvelopeFieldsInAFixedOrder() throws Exception {
        JsonNode json = reader.readTree(serializer.serialize("codemonk.events", fixedEnvelope()));

        List<String> fieldNames = new ArrayList<>();
        json.fieldNames().forEachRemaining(fieldNames::add);

        assertEquals(List.of("eventId", "eventType", "timestamp", "correlationId", "payload"), fieldNames);
    }

    @Test
    void shouldStampTheCurrentTimeWhenBuiltWithOf() {
        Instant before = Instant.now();
        EventEnvelope<String> envelope = EventEnvelope.of(EventType.CODE_PARSED, "payload");
        Instant after = Instant.now();

        assertFalse(envelope.timestamp().isBefore(before));
        assertFalse(envelope.timestamp().isAfter(after));
    }

    @Test
    void shouldStampTheCurrentTimeWhenTheTimestampIsNull() {
        Instant before = Instant.now();
        EventEnvelope<String> envelope = new EventEnvelope<>("evt-1", EventType.CODE_PARSED, null, "corr-1", "payload");
        Instant after = Instant.now();

        assertFalse(envelope.timestamp().isBefore(before));
        assertFalse(envelope.timestamp().isAfter(after));
    }

    @Test
    void shouldShareACorrelationIdAcrossOneChainOfEventsButNotTheEventId() {
        EventEnvelope<String> parsed = EventEnvelope.of(EventType.CODE_PARSED, "corr-9", "parsed");
        EventEnvelope<String> indexed = EventEnvelope.of(EventType.SEARCH_INDEX_UPDATED, "corr-9", "indexed");

        assertEquals("corr-9", parsed.correlationId());
        assertEquals("corr-9", indexed.correlationId());
        assertNotEquals(parsed.eventId(), indexed.eventId());
    }

    @Test
    void shouldGenerateACorrelationIdWhenTheFactoryIsGivenABlankOrNullOne() {
        EventEnvelope<String> blank = EventEnvelope.of(EventType.CODE_PARSED, " ", "payload");
        EventEnvelope<String> missing = EventEnvelope.of(EventType.CODE_PARSED, null, "payload");

        assertDoesNotThrow(() -> UUID.fromString(blank.correlationId()));
        assertDoesNotThrow(() -> UUID.fromString(missing.correlationId()));
        assertNotEquals(blank.correlationId(), missing.correlationId());
    }

    @Test
    void shouldKeepA64BitNumberInAPayloadExact() throws Exception {
        long id = 9_007_199_254_740_993L;
        EventEnvelope<Map<String, Long>> envelope =
                new EventEnvelope<>("evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", Map.of("id", id));

        byte[] bytes = serializer.serialize("codemonk.events", envelope);

        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("9007199254740993"));
        assertEquals(id, reader.readTree(bytes).get("payload").get("id").asLong());
    }

    @Test
    void shouldBeUsableAsAKafkaValueSerializerConfiguredByClass() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonEventSerializer.class);
        ProducerConfig config = new ProducerConfig(props);

        JsonEventSerializer created = assertInstanceOf(JsonEventSerializer.class,
                config.getConfiguredInstance(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Serializer.class));
        created.configure(props, false);

        assertNotNull(created.serialize("codemonk.events", fixedEnvelope()));
    }
}