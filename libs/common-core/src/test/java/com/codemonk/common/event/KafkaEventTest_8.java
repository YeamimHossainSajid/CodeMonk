package com.codemonk.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaEventTest_8 {

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-10-11T12:00:00Z");

    private final ObjectMapper reader = new ObjectMapper();

    private JsonEventSerializer serializer;

    public static class BrokenBean {
        public String getName() {
            throw new IllegalStateException("getter failed");
        }
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
    void shouldUseAnExactErrorMessageThatNamesTheTopic() {
        SerializationException exception = assertThrows(SerializationException.class,
                () -> serializer.serialize("codemonk.broken", new Object()));

        assertEquals("Failed to serialize event payload for topic [codemonk.broken]", exception.getMessage());
    }

    @Test
    void shouldNotCallTheMapperWhenTheDataIsNull() {
        AtomicInteger calls = new AtomicInteger();
        ObjectMapper counting = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) {
                calls.incrementAndGet();
                return new byte[0];
            }
        };
        JsonEventSerializer countingSerializer = new JsonEventSerializer(counting);

        assertNull(countingSerializer.serialize("codemonk.events", null));
        assertEquals(0, calls.get());

        countingSerializer.serialize("codemonk.events", fixedEnvelope());
        assertEquals(1, calls.get());
    }

    @Test
    void shouldReturnTheBytesProducedByTheMapperUnchanged() {
        byte[] fixed = {9, 8, 7};
        ObjectMapper fixedMapper = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) {
                return fixed;
            }
        };

        byte[] result = new JsonEventSerializer(fixedMapper).serialize("codemonk.events", fixedEnvelope());

        assertSame(fixed, result);
    }

    @Test
    void shouldSerializeAnEmptyBeanWhenTheMapperAllowsIt() {
        ObjectMapper lenient = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        byte[] bytes = new JsonEventSerializer(lenient).serialize("codemonk.events", new Object());

        assertEquals("{}", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void shouldWrapAFailureThrownByAPayloadGetter() {
        EventEnvelope<BrokenBean> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", new BrokenBean());

        SerializationException exception = assertThrows(SerializationException.class,
                () -> serializer.serialize("codemonk.events", envelope));

        assertTrue(exception.getCause() instanceof JsonProcessingException);
        assertTrue(exception.getCause().getCause() instanceof IllegalStateException);
    }

    @Test
    void shouldRejectAnOptionalPayloadBecauseTheJdk8ModuleIsNotRegistered() {
        EventEnvelope<Optional<String>> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", Optional.of("value"));

        SerializationException exception = assertThrows(SerializationException.class,
                () -> serializer.serialize("codemonk.events", envelope));

        assertTrue(exception.getCause() instanceof JsonProcessingException);
    }

    @Test
    void shouldSerializeTheStateOfAMutablePayloadAtTheMomentOfSerialization() throws Exception {
        List<String> files = new ArrayList<>();
        EventEnvelope<List<String>> envelope = EventEnvelope.of(EventType.INDEXING_REQUESTED, files);

        files.add("late.java");
        JsonNode payload = payloadOf(envelope);

        assertEquals(1, payload.size());
        assertEquals("late.java", payload.get(0).asText());
    }

    @Test
    void shouldWriteNaNAsAQuotedString() throws Exception {
        EventEnvelope<Double> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", Double.NaN);

        JsonNode payload = payloadOf(envelope);

        assertTrue(payload.isTextual());
        assertEquals("NaN", payload.asText());
    }

    @Test
    void shouldWriteALocalDatePayloadAsAnArrayOfNumbers() throws Exception {
        EventEnvelope<LocalDate> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", LocalDate.of(2026, 10, 11));

        JsonNode payload = payloadOf(envelope);

        assertTrue(payload.isArray());
        assertEquals(3, payload.size());
        assertEquals(2026, payload.get(0).asInt());
        assertEquals(10, payload.get(1).asInt());
        assertEquals(11, payload.get(2).asInt());
    }

    @Test
    void shouldWriteATimestampBeforeTheEpochAsANegativeNumber() throws Exception {
        Instant beforeEpoch = Instant.ofEpochSecond(-86_400);
        EventEnvelope<String> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, beforeEpoch, "corr-1", "payload");

        JsonNode json = reader.readTree(serializer.serialize("codemonk.events", envelope));

        assertTrue(json.get("timestamp").isNumber());
        assertEquals(-86_400.0, json.get("timestamp").asDouble(), 1e-9);
    }

    @Test
    void shouldWriteAUuidPayloadAsText() throws Exception {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        EventEnvelope<UUID> envelope = new EventEnvelope<>(
                "evt-1", EventType.CODE_PARSED, FIXED_TIMESTAMP, "corr-1", id);

        JsonNode payload = payloadOf(envelope);

        assertTrue(payload.isTextual());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", payload.asText());
    }

    @Test
    void shouldRejectANullNameWhenResolvingAnEventType() {
        assertThrows(NullPointerException.class, () -> EventType.valueOf(null));
    }
}