package com.codemonk.common.service;

import com.codemonk.common.constant.JsonConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class JsonEventDeserializer implements Deserializer<Map<String, Object>> {

    private static final TypeReference<Map<String, Object>> EVENT_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public JsonEventDeserializer() {
        this.objectMapper = JsonConstants.OBJECT_MAPPER;
    }

    @Override
    public Map<String, Object> deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.readValue(data, EVENT_TYPE);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize JSON event from topic '" + topic + "'", e);
        }
    }
}
