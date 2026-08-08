package com.procureai.service.email;

public interface EmailService {
    /** Sends (or simulates sending) an email and returns the persisted EmailMessage id. */
    Long send(String toAddress, String subject, String body, Long negotiationId);

    String providerName();
}
