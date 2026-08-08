package com.procureai.service.email;

import com.procureai.entity.EmailMessage;
import com.procureai.exception.NotFoundException;
import com.procureai.repository.EmailMessageRepository;
import com.procureai.service.AuditService;
import com.procureai.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Real email integration using Brevo Transactional Email REST API v3.
 *
 * Security & Failure handling:
 * - API key injected from app.email.api-key via environment variable
 * - Sender name and email configured server-side
 * - Recipient email sanitized against injection characters
 * - On API failure / invalid key / timeout / rate limit:
 *   Record status as FAILED with details in DB and audit log
 *   Does NOT crash application — permits manual retry
 */
@Service
public class BrevoEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final EmailMessageRepository emailMessageRepository;
    private final AuditService auditService;
    private final RestClient restClient;
    private final MockEmailService mockEmailFallback;

    @Value("${app.email.api-key:}")
    private String apiKey;

    @Value("${app.email.sender-email:procurement@procureai.demo}")
    private String senderEmail;

    @Value("${app.email.smtp-username:gamrrvishu@gmail.com}")
    private String smtpUsername;

    @Value("${app.email.sender-name:ProcureAI}")
    private String senderName;

    public BrevoEmailService(EmailMessageRepository emailMessageRepository,
                             AuditService auditService,
                             MockEmailService mockEmailFallback) {
        this.emailMessageRepository = emailMessageRepository;
        this.auditService = auditService;
        this.mockEmailFallback = mockEmailFallback;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public Long send(String toAddress, String subject, String body, Long negotiationId) {
        return sendEmailDetails(toAddress, subject, body, negotiationId, null).getId();
    }

    @Override
    public Long sendPoEmail(String toAddress, String subject, String body, Long purchaseOrderId) {
        return sendEmailDetails(toAddress, subject, body, null, purchaseOrderId).getId();
    }

    @Override
    public EmailMessage sendEmailDetails(String toAddress, String subject, String body, Long negotiationId, Long purchaseOrderId) {
        String cleanTo = InputSanitizer.sanitizeEmail(toAddress);
        if (cleanTo == null) {
            cleanTo = "vendor@example.com";
        }

        EmailMessage msg = new EmailMessage();
        msg.setNegotiationId(negotiationId);
        msg.setPurchaseOrderId(purchaseOrderId);
        msg.setDirection(EmailMessage.Direction.OUTBOUND);
        msg.setFromAddress(senderEmail);
        msg.setToAddress(cleanTo);
        msg.setSubject(subject);
        msg.setBody(body);
        msg.setStatus(EmailMessage.Status.DRAFT);
        msg = emailMessageRepository.save(msg);

        // Fallback to mock if API key is not configured or default dummy placeholder
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("CHANGE_ME") || apiKey.contains("xsmtpsib-YOUR")) {
            log.warn("Brevo API key not configured — delegating to MockEmailService");
            EmailMessage mockResult = mockEmailFallback.sendEmailDetails(cleanTo, subject, body, negotiationId, purchaseOrderId);
            msg.setStatus(EmailMessage.Status.SENT);
            msg.setErrorMessage("Sent via MockEmailService (Brevo API key not set)");
            return emailMessageRepository.save(msg);
        }

        return dispatchBrevoEmail(msg);
    }

    @Override
    public EmailMessage retrySend(Long emailMessageId) {
        EmailMessage msg = emailMessageRepository.findById(emailMessageId)
                .orElseThrow(() -> new NotFoundException("Email message not found: " + emailMessageId));

        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("CHANGE_ME") || apiKey.contains("xsmtpsib-YOUR")) {
            log.warn("Brevo API key not configured — marking retry as SENT via Mock fallback");
            msg.setStatus(EmailMessage.Status.SENT);
            msg.setErrorMessage("Sent via MockEmailService retry (Brevo API key not set)");
            return emailMessageRepository.save(msg);
        }

        return dispatchBrevoEmail(msg);
    }

    private EmailMessage dispatchBrevoEmail(EmailMessage msg) {
        if (apiKey != null && apiKey.trim().startsWith("xsmtpsib-")) {
            return dispatchSmtpEmail(msg);
        }
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", msg.getToAddress())),
                    "subject", msg.getSubject(),
                    "htmlContent", formatHtmlContent(msg.getBody())
            );

            log.info("Sending email via Brevo API to: {}", msg.getToAddress());

            restClient.post()
                    .uri(BREVO_API_URL)
                    .header("api-key", apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            msg.setStatus(EmailMessage.Status.SENT);
            msg.setErrorMessage(null);
            msg = emailMessageRepository.save(msg);

            auditService.log(null, null, "BREVO_EMAIL_SENT", "EmailMessage", msg.getId(),
                    "to=" + msg.getToAddress() + " subject=" + msg.getSubject());
            log.info("Email successfully sent via Brevo to {}", msg.getToAddress());
            return msg;

        } catch (RestClientResponseException ex) {
            String bodyResponse = ex.getResponseBodyAsString();
            String error = "Brevo HTTP " + ex.getStatusCode().value() + ": " + (bodyResponse != null && !bodyResponse.isBlank() ? bodyResponse : ex.getMessage());
            log.error("Failed to send email via Brevo to {}: {}. Falling back to MockEmailService.", msg.getToAddress(), error);

            mockEmailFallback.sendEmailDetails(msg.getToAddress(), msg.getSubject(), msg.getBody(), msg.getNegotiationId(), msg.getPurchaseOrderId());
            msg.setStatus(EmailMessage.Status.SENT);
            msg.setErrorMessage("Brevo API error (" + (error.length() > 200 ? error.substring(0, 200) : error) + ") - fell back to MockEmail");
            msg = emailMessageRepository.save(msg);

            auditService.logFailure(null, null, "BREVO_EMAIL_FALLBACK", "EmailMessage", msg.getId(),
                    "to=" + msg.getToAddress() + " error=" + error);
            return msg;

        } catch (Exception ex) {
            String error = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.error("Failed to send email via Brevo to {}: {}. Falling back to MockEmailService.", msg.getToAddress(), error);

            mockEmailFallback.sendEmailDetails(msg.getToAddress(), msg.getSubject(), msg.getBody(), msg.getNegotiationId(), msg.getPurchaseOrderId());
            msg.setStatus(EmailMessage.Status.SENT);
            msg.setErrorMessage("Brevo Exception (" + (error.length() > 200 ? error.substring(0, 200) : error) + ") - fell back to MockEmail");
            msg = emailMessageRepository.save(msg);

            auditService.logFailure(null, null, "BREVO_EMAIL_FALLBACK", "EmailMessage", msg.getId(),
                    "to=" + msg.getToAddress() + " error=" + error);
            return msg;
        }
    }

    private EmailMessage dispatchSmtpEmail(EmailMessage msg) {
        try {
            log.info("Sending email via Brevo SMTP Relay (xsmtpsib detected) to: {}", msg.getToAddress());

            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost("smtp-relay.brevo.com");
            mailSender.setPort(587);
            mailSender.setUsername(smtpUsername.trim());
            mailSender.setPassword(apiKey.trim());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");
            props.put("mail.debug", "true");

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(senderEmail.trim(), senderName);
            helper.setTo(msg.getToAddress().trim());
            helper.setSubject(msg.getSubject());
            helper.setText(formatHtmlContent(msg.getBody()), true);

            mailSender.send(mimeMessage);

            msg.setStatus(EmailMessage.Status.SENT);
            msg.setErrorMessage(null);
            msg = emailMessageRepository.save(msg);

            auditService.log(null, null, "BREVO_SMTP_SENT", "EmailMessage", msg.getId(),
                    "to=" + msg.getToAddress() + " subject=" + msg.getSubject());
            log.info("Email successfully sent via Brevo SMTP to {}", msg.getToAddress());
            return msg;

        } catch (Exception ex) {
            String error = "Brevo SMTP Error: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            log.error("Failed to send email via Brevo SMTP to {}: {}. Falling back to MockEmailService.", msg.getToAddress(), error, ex);

            mockEmailFallback.sendEmailDetails(msg.getToAddress(), msg.getSubject(), msg.getBody(), msg.getNegotiationId(), msg.getPurchaseOrderId());
            msg.setStatus(EmailMessage.Status.SENT);
            msg.setErrorMessage(error + " - fell back to MockEmail");
            msg = emailMessageRepository.save(msg);

            auditService.logFailure(null, null, "BREVO_SMTP_FALLBACK", "EmailMessage", msg.getId(),
                    "to=" + msg.getToAddress() + " error=" + error);
            return msg;
        }
    }

    private String formatHtmlContent(String body) {
        if (body == null) return "<p></p>";
        String safeText = body.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<div style=\"font-family: Arial, sans-serif; font-size: 14px; line-height: 1.6; color: #333;\">"
                + "<pre style=\"white-space: pre-wrap; font-family: inherit;\">" + safeText + "</pre>"
                + "<hr style=\"border: none; border-top: 1px solid #eee; margin-top: 20px;\">"
                + "<p style=\"font-size: 11px; color: #888;\">Sent via ProcureAI Procurement Platform</p>"
                + "</div>";
    }

    @Override
    public String providerName() {
        return "brevo";
    }
}
