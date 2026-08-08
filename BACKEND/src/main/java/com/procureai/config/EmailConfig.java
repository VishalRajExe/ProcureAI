package com.procureai.config;

import com.procureai.service.email.BrevoEmailService;
import com.procureai.service.email.EmailService;
import com.procureai.service.email.MockEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for selecting the active EmailService implementation.
 * Defaults to MockEmailService when app.email.provider is not set or Brevo key is absent.
 */
@Configuration
public class EmailConfig {

    @Value("${app.email.provider:mock}")
    private String configuredProvider;

    @Value("${app.email.api-key:}")
    private String apiKey;

    @Bean
    @Primary
    public EmailService emailService(MockEmailService mockEmailService, BrevoEmailService brevoEmailService) {
        if ("brevo".equalsIgnoreCase(configuredProvider) && apiKey != null && !apiKey.isBlank()) {
            return brevoEmailService;
        }
        return mockEmailService;
    }
}
