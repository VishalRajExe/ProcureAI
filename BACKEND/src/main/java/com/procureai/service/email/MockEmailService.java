package com.procureai.service.email;

import com.procureai.entity.EmailMessage;
import com.procureai.repository.EmailMessageRepository;
import org.springframework.stereotype.Service;

/**
 * Simulated vendor inbox. No real network calls — the "send" is just persisted so
 * the full negotiation workflow (including a later simulated vendor reply) works
 * without any email credentials, which keeps the demo reliable offline.
 */
@Service
public class MockEmailService implements EmailService {

    private final EmailMessageRepository emailMessageRepository;

    public MockEmailService(EmailMessageRepository emailMessageRepository) {
        this.emailMessageRepository = emailMessageRepository;
    }

    @Override
    public Long send(String toAddress, String subject, String body, Long negotiationId) {
        EmailMessage msg = new EmailMessage();
        msg.setNegotiationId(negotiationId);
        msg.setDirection(EmailMessage.Direction.OUTBOUND);
        msg.setFromAddress("procurement@procureai.demo");
        msg.setToAddress(toAddress);
        msg.setSubject(subject);
        msg.setBody(body);
        msg.setStatus(EmailMessage.Status.SENT);
        return emailMessageRepository.save(msg).getId();
    }

    @Override
    public String providerName() {
        return "mock";
    }
}
