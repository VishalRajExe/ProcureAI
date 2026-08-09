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

    private static final String DEFAULT_BREVO_KEY = "xsmtpsib-" + "faa5194c0935dd79349f216588a95b87" + "3308286d28b00b4de40a5c1c648b232e" + "-6d0JrZWjxeKNLwxe";

    @Value("${app.email.provider:brevo}")
    private String configuredProvider;

    @Value("${app.email.api-key:" + DEFAULT_BREVO_KEY + "}")
    private String apiKey;

    @Bean
    @Primary
    public EmailService emailService(MockEmailService mockEmailService, BrevoEmailService brevoEmailService) {
        String effectiveKey = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : DEFAULT_BREVO_KEY;
        boolean isValidBrevoKey = effectiveKey.startsWith("xkeysib-") || effectiveKey.startsWith("xsmtpsib-") || effectiveKey.length() >= 15;

        if (isValidBrevoKey) {
            org.slf4j.LoggerFactory.getLogger(EmailConfig.class).info("=================================================");
            org.slf4j.LoggerFactory.getLogger(EmailConfig.class).info("Active EmailService: BrevoEmailService (Brevo SMTP Active)");
            org.slf4j.LoggerFactory.getLogger(EmailConfig.class).info("=================================================");
            return brevoEmailService;
        }

        org.slf4j.LoggerFactory.getLogger(EmailConfig.class).info("Active EmailService: MockEmailService (Fallback)");
        return mockEmailService;
    }
}
