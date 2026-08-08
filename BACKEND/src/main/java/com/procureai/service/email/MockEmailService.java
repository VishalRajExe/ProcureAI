package com.procureai.service.email;

import com.procureai.entity.EmailMessage;
import com.procureai.exception.NotFoundException;
import com.procureai.repository.EmailMessageRepository;
import org.springframework.stereotype.Service;

/**
 * Deterministic mock email service for offline/demo operation.
 * Persists email messages directly without calling external APIs.
 */
@Service
public class MockEmailService implements EmailService {

    private final EmailMessageRepository emailMessageRepository;

    public MockEmailService(EmailMessageRepository emailMessageRepository) {
        this.emailMessageRepository = emailMessageRepository;
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
        EmailMessage msg = new EmailMessage();
        msg.setNegotiationId(negotiationId);
        msg.setPurchaseOrderId(purchaseOrderId);
        msg.setDirection(EmailMessage.Direction.OUTBOUND);
        msg.setFromAddress("procurement@procureai.demo");
        msg.setToAddress(toAddress != null ? toAddress : "vendor@example.com");
        msg.setSubject(subject);
        msg.setBody(body);
        msg.setStatus(EmailMessage.Status.SENT);
        msg.setErrorMessage(null);
        return emailMessageRepository.save(msg);
    }

    @Override
    public EmailMessage retrySend(Long emailMessageId) {
        EmailMessage msg = emailMessageRepository.findById(emailMessageId)
                .orElseThrow(() -> new NotFoundException("Email message not found: " + emailMessageId));
        msg.setStatus(EmailMessage.Status.SENT);
        msg.setErrorMessage(null);
        return emailMessageRepository.save(msg);
    }

    @Override
    public String providerName() {
        return "mock";
    }
}
