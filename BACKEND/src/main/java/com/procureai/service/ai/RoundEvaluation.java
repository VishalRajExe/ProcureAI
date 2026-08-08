package com.procureai.service.ai;

/**
 * AI's structured evaluation of a vendor's counter-offer during a negotiation round.
 * The "accept" recommendation is advisory only — the backend independently verifies
 * the counter price against maxApprovedPrice before any acceptance is finalized.
 */
public record RoundEvaluation(
        boolean recommendAccept,
        String notes,
        double confidence
) {}
