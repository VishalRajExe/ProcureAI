package com.procureai.config;

import com.procureai.service.ai.AIProvider;
import com.procureai.service.ai.GeminiAIProvider;
import com.procureai.service.ai.MockAIProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for selecting the active AIProvider implementation.
 * Defaults to MockAIProvider when app.ai.provider is 'mock' or Gemini API key is absent.
 */
@Configuration
public class AIConfig {

    @Value("${app.ai.provider:mock}")
    private String configuredProvider;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Bean
    @Primary
    public AIProvider aiProvider(MockAIProvider mockAIProvider, GeminiAIProvider geminiAIProvider) {
        if (("gemini".equalsIgnoreCase(configuredProvider) || "google".equalsIgnoreCase(configuredProvider))
                && apiKey != null && !apiKey.isBlank()) {
            return geminiAIProvider;
        }
        return mockAIProvider;
    }
}
