package com.codemonk.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AIServiceTest {

    @Test
    void shouldGenerateDemoResponse() {
        AIService aiService = new AIService();

        String result = aiService.generateResponse("Hello AI");

        assertEquals("Demo AI response for: Hello AI", result);
    }
}