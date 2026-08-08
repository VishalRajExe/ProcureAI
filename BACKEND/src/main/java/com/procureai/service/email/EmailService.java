package com.procureai.service.email;

import com.procureai.entity.EmailMessage;

public interface EmailService {

    /** Sends a negotiation email and returns the persisted EmailMessage id. */
    Long send(String toAddress, String subject, String body, Long negotiationId);

    /** Sends a Purchase Order email and returns the persisted EmailMessage id. */
    Long sendPoEmail(String toAddress, String subject, String body, Long purchaseOrderId);

    /** Full email creation and dispatch with support for both negotiation and PO references. */
    EmailMessage sendEmailDetails(String toAddress, String subject, String body, Long negotiationId, Long purchaseOrderId);

    /** Attempts to re-send a previously FAILED email message. */
    EmailMessage retrySend(Long emailMessageId);

    String providerName();
}
