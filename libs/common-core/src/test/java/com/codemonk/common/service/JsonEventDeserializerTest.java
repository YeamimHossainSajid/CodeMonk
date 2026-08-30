package com.codemonk.common.service;

import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonEventDeserializerTest {

    private JsonEventDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new JsonEventDeserializer();
    }

    @Test
    void shouldDeserializeFlatJsonObjectIntoMap() {
        byte[] payload = """
                {"eventId": "evt-1", "userId": 42, "active": true}
                """.getBytes(StandardCharsets.UTF_8);

        Map<String, Object> result = deserializer.deserialize("codemonk.events", payload);

        assertEquals("evt-1", result.get("eventId"));
        assertEquals(42, result.get("userId"));
        assertEquals(true, result.get("active"));
    }

    @Test
    void shouldDeserializeNestedJsonStructures() {
        byte[] payload = """
                {
                  "eventType": "repository.indexed",
                  "metadata": {"branch": "main", "commit": "abc123"},
                  "tags": ["java", "kafka"]
                }
                """.getBytes(StandardCharsets.UTF_8);

        Map<String, Object> result = deserializer.deserialize("codemonk.events", payload);

        assertEquals("repository.indexed", result.get("eventType"));
        assertTrue(result.get("metadata") instanceof Map);
        assertEquals("main", ((Map<?, ?>) result.get("metadata")).get("branch"));
        assertEquals(List.of("java", "kafka"), result.get("tags"));
    }

    @Test
    void shouldReturnNullWhenDataIsNull() {
        assertNull(deserializer.deserialize("codemonk.events", null));
    }

    @Test
    void shouldThrowSerializationExceptionOnMalformedJson() {
        byte[] payload = "not-valid-json".getBytes(StandardCharsets.UTF_8);

        SerializationException exception = assertThrows(SerializationException.class,
                () -> deserializer.deserialize("codemonk.events", payload));

        assertTrue(exception.getMessage().contains("codemonk.events"));
    }

    @Test
    void shouldThrowSerializationExceptionOnEmptyPayload() {
        byte[] payload = new byte[0];

        assertThrows(SerializationException.class,
                () -> deserializer.deserialize("codemonk.events", payload));
    }

    @Test
    void shouldThrowSerializationExceptionWhenJsonIsNotAnObject() {
        byte[] payload = "[1, 2, 3]".getBytes(StandardCharsets.UTF_8);

        assertThrows(SerializationException.class,
                () -> deserializer.deserialize("codemonk.events", payload));
    }
}
