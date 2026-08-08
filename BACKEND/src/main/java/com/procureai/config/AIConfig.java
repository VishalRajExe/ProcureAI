package com.procureai.config;

import com.procureai.service.ai.AIProvider;
import com.procureai.service.ai.MockAIProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Selects the active AIProvider. Defaults to the deterministic MockAIProvider so the
 * whole application (including the demo workflow) works with zero external AI
 * credentials. A real provider can be wired in later behind the same AIProvider
 * interface without touching any calling code.
 */
@Configuration
public class AIConfig {

    @Value("${app.ai.provider:mock}")
    private String configuredProvider;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Bean
    @Primary
    public AIProvider aiProvider(MockAIProvider mockAIProvider) {
        // Hackathon scope: only the mock provider ships in this build. If a real provider
        // is configured but no key is present, safely fall back to mock rather than fail.
        if (!"mock".equalsIgnoreCase(configuredProvider) && (apiKey == null || apiKey.isBlank())) {
            return mockAIProvider;
        }
        return mockAIProvider;
    }
}
