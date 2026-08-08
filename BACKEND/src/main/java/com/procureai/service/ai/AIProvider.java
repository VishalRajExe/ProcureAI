package com.procureai.service.ai;

import com.procureai.entity.Quote;

import java.math.BigDecimal;

/**
 * Abstraction over the AI backend. Implementations must return structured,
 * schema-valid data only. No implementation may execute code, call external
 * systems, or take direct backend actions — output is always routed through
 * backend validation before it can affect business state.
 */
public interface AIProvider {

    /** Extract structured quote data from raw document text (already OCR'd if needed). */
    ExtractedQuoteData extractQuoteData(String rawDocumentText, String hintedVendorName);

    /** Produce a structured negotiation decision, bounded by the caller-provided constraints. */
    NegotiationDecision decideNegotiationStrategy(NegotiationContext context);

    /** Draft a professional negotiation email using only the provided factual data. */
    String draftNegotiationEmail(NegotiationContext context, NegotiationDecision decision);

    /** Evaluate a vendor's counter-offer against context; recommendation only, never authoritative. */
    RoundEvaluation evaluateVendorResponse(NegotiationContext context, BigDecimal vendorCounterPrice, int roundNumber);

    /** Short natural-language explanation of why a vendor was recommended, from real computed data. */
    String explainRecommendation(Quote recommendedQuote, String comparisonSummary);

    String providerName();
}
