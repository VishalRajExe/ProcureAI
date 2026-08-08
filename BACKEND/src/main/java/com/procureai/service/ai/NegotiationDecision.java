package com.procureai.service.ai;

import java.math.BigDecimal;

/**
 * Structured, schema-bound output from the AI negotiation reasoning step.
 * This is a DTO, not free-form text — the backend validates every field
 * before any of it is allowed to influence a real action.
 */
public record NegotiationDecision(
        Action action,
        BigDecimal targetPrice,
        BigDecimal maxApprovedPrice,
        String strategy,
        String reason,
        double confidence
) {
    public enum Action { NEGOTIATE, ACCEPT, REJECT, REQUEST_CLARIFICATION }
}
