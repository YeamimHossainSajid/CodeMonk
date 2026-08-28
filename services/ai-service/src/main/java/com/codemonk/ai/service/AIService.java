package com.codemonk.ai.service;

import org.springframework.stereotype.Service;

@Service
public class AIService {

    public String generateResponse(String prompt) {
        return "Demo AI response for: " + prompt;
    }
}