package com.procureai;

import com.procureai.entity.EmailMessage;
import com.procureai.repository.EmailMessageRepository;
import com.procureai.service.ai.AIProvider;
import com.procureai.service.ai.ExtractedQuoteData;
import com.procureai.service.ai.GeminiAIProvider;
import com.procureai.service.ai.NegotiationContext;
import com.procureai.service.ai.NegotiationDecision;
import com.procureai.service.email.BrevoEmailService;
import com.procureai.service.email.EmailService;
import com.procureai.service.email.MockEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
@DisplayName("Gemini AI & Brevo Email Service Integration & Fallback Tests")
class GeminiAndBrevoTest {

    @Autowired
    private AIProvider activeAiProvider;

    @Autowired
    private GeminiAIProvider geminiAiProvider;

    @Autowired
    private EmailService activeEmailService;

    @Autowired
    private BrevoEmailService brevoEmailService;

    @Autowired
    private MockEmailService mockEmailService;

    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Test
    @DisplayName("AIProvider bean is active and non-null")
    void aiProvider_isActive() {
        assertThat(activeAiProvider).isNotNull();
        assertThat(activeAiProvider.providerName()).isNotBlank();
    }

    @Test
    @DisplayName("GeminiAIProvider falls back to MockAIProvider safely when unconfigured")
    void geminiProvider_fallbackSafety() {
        ExtractedQuoteData data = geminiAiProvider.extractQuoteData(
                "Vendor: Acme Tech\nUnit Price: Rs. 50000\nQuantity: 5", "Acme Tech"
        );
        assertThat(data).isNotNull();
        assertThat(data.vendorName()).isEqualTo("Acme Tech");
        assertThat(data.items()).isNotEmpty();
    }

    @Test
    @DisplayName("GeminiAIProvider negotiation decision fallback produces valid decision")
    void geminiProvider_negotiationDecisionFallback() {
        NegotiationContext ctx = new NegotiationContext(
                "Acme Tech", "Laptops", new BigDecimal("60000"),
                new BigDecimal("45000"), new BigDecimal("55000"),
                new BigDecimal("50000"), new BigDecimal("58000"),
                10, 12, 7, 12, 14
        );
        NegotiationDecision decision = geminiAiProvider.decideNegotiationStrategy(ctx);
        assertThat(decision).isNotNull();
        assertThat(decision.action()).isNotNull();
        assertThat(decision.targetPrice()).isNotNull();
        assertThat(decision.maxApprovedPrice()).isEqualByComparingTo("58000");
    }

    @Test
    @DisplayName("EmailService bean is active and non-null")
    void emailService_isActive() {
        assertThat(activeEmailService).isNotNull();
        assertThat(activeEmailService.providerName()).isNotBlank();
    }

    @Test
    @DisplayName("BrevoEmailService processes email request safely")
    void brevoEmailService_fallbackSafety() {
        EmailMessage msg = brevoEmailService.sendEmailDetails(
                "vendor@acme.com", "Test Subject", "Test Body", 1L, null
        );
        assertThat(msg).isNotNull();
        assertThat(msg.getToAddress()).isEqualTo("vendor@acme.com");
        assertThat(msg.getStatus()).isNotNull();
    }

    @Test
    @DisplayName("BrevoEmailService sendPoEmail generates email record linked to purchase order")
    void brevoEmailService_sendPoEmail() {
        Long emailId = brevoEmailService.sendPoEmail(
                "vendor@supplier.com", "PO Issued: PO-2026-1001", "PO Body", 50L
        );
        assertThat(emailId).isNotNull();

        EmailMessage msg = emailMessageRepository.findById(emailId).orElse(null);
        assertThat(msg).isNotNull();
        assertThat(msg.getPurchaseOrderId()).isEqualTo(50L);
        assertThat(msg.getSubject()).contains("PO Issued");
    }

    @Test
    @DisplayName("BrevoEmailService retrySend processes email status correctly")
    void brevoEmailService_retrySend() {
        EmailMessage msg = brevoEmailService.sendEmailDetails(
                "vendor@test.com", "Subject", "Body", null, 10L
        );
        EmailMessage retried = brevoEmailService.retrySend(msg.getId());
        assertThat(retried).isNotNull();
        assertThat(retried.getStatus()).isNotNull();
    }
}
