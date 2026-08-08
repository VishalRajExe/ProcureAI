package com.procureai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.procureai.entity.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * ProcureAI Python AI Service Bridge.
 *
 * Wraps PythonAIClient with ProcureAI business objects.
 * All methods return Optional — callers fall back to GeminiAIProvider when empty.
 *
 * Architecture:
 *   Controller → Service → PythonAIService → PythonAIClient → FastAPI → Gemini
 *                                               (empty)↓
 *                                         GeminiAIProvider (existing)
 *
 * This service NEVER:
 *  - Directly writes to the database
 *  - Sends emails
 *  - Generates Purchase Orders
 *  - Bypasses business rules
 *
 * It ONLY:
 *  - Calls FastAPI for AI intelligence
 *  - Returns advisory results for Spring Boot to validate
 */
@Service
public class PythonAIService {

    private static final Logger log = LoggerFactory.getLogger(PythonAIService.class);

    @Value("${app.python-ai.enabled:false}")
    private boolean enabled;

    private final PythonAIClient client;

    public PythonAIService(PythonAIClient client) {
        this.client = client;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vendor evaluation — returns AI score breakdown
    // Adapted from vendor_evaluation_plugin.py scoring (1-10 per dimension)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get AI vendor evaluation score (1-10) with Defensive/Balanced/Aggressive risk assessment.
     * Returns null if FastAPI unavailable — caller uses heuristic fallback.
     */
    public VendorEvaluationResult evaluateVendor(Quote quote, BigDecimal budgetCeiling) {
        if (!enabled) return null;

        String vendorName = quote.getVendor() != null ? quote.getVendor().getName() : "Unknown";
        BigDecimal total = quote.getCalculatedTotal() != null ? quote.getCalculatedTotal() : BigDecimal.ZERO;
        int warranty = quote.getWarrantyMonths() != null ? quote.getWarrantyMonths() : 12;
        int delivery = quote.getDeliveryDays() != null ? quote.getDeliveryDays() : 30;
        String terms = quote.getPaymentTerms();

        Optional<JsonNode> result = client.evaluateVendor(
                vendorName, total, warranty, delivery, terms,
                null, budgetCeiling, 12, 30
        );

        return result.map(node -> new VendorEvaluationResult(
                node.path("overall_score").asDouble(5.0),
                node.path("risk_level").asText("Medium"),
                node.path("recommendation").asText("Acceptable"),
                node.path("ai_evaluation_summary").asText(""),
                node.path("ai_mode").asText("demo")
        )).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Quote comparison — enhanced narrative ranking
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compare quotes with AI market intelligence context.
     * Returns null if FastAPI unavailable — caller uses heuristic ComparisonService.
     */
    public QuoteComparisonResult compareQuotes(List<Quote> quotes, String category) {
        if (!enabled || quotes == null || quotes.size() < 2) return null;

        List<PythonAIClient.QuoteInput> inputs = quotes.stream()
                .map(q -> new PythonAIClient.QuoteInput(
                        q.getId(),
                        q.getVendor() != null ? q.getVendor().getName() : "Unknown",
                        q.getCalculatedTotal() != null ? q.getCalculatedTotal() : BigDecimal.ZERO,
                        q.getWarrantyMonths() != null ? q.getWarrantyMonths() : 12,
                        q.getDeliveryDays() != null ? q.getDeliveryDays() : 30,
                        q.getPaymentTerms()
                )).toList();

        Optional<JsonNode> result = client.compareQuotes(inputs, category, null);
        return result.map(node -> new QuoteComparisonResult(
                node.path("recommended_vendor_name").asText(""),
                node.path("ai_rationale").asText(""),
                node.path("ai_mode").asText("demo")
        )).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Negotiation strategy — Defensive / Balanced / Aggressive framing
    // Adapted from negotiation_strategy agent (agent_prompts.jinja)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Enrich negotiation strategy with AI approach framing.
     * Spring Boot enforces all business rules; this is advisory context only.
     */
    public NegotiationStrategyResult negotiationStrategy(NegotiationContext ctx, BigDecimal benchmarkMin, BigDecimal benchmarkMax) {
        if (!enabled) return null;

        PythonAIClient.NegotiationInput input = new PythonAIClient.NegotiationInput(
                ctx.vendorName(), ctx.productSummary(),
                ctx.currentPrice(), ctx.targetPrice(), ctx.maxAcceptablePrice(),
                ctx.quantity() != null ? ctx.quantity() : 1,
                ctx.warrantyMonths() != null ? ctx.warrantyMonths() : 12,
                ctx.deliveryDays() != null ? ctx.deliveryDays() : 30,
                ctx.minWarrantyMonths() != null ? ctx.minWarrantyMonths() : 24,
                ctx.maxDeliveryDays() != null ? ctx.maxDeliveryDays() : 30,
                0,
                benchmarkMin, benchmarkMax
        );

        Optional<JsonNode> result = client.negotiationStrategy(input);
        return result.map(node -> new NegotiationStrategyResult(
                node.path("approach").asText("Balanced"),
                node.path("key_leverage_points"),
                node.path("risk_mitigation"),
                node.path("ai_mode").asText("demo")
        )).orElse(null);
    }

    /**
     * Generate an enhanced negotiation email using FastAPI.
     * Returns null if unavailable — caller uses GeminiAIProvider.draftNegotiationEmail.
     */
    public String generateEnhancedEmail(NegotiationContext ctx, NegotiationDecision decision,
                                         String approach, int round) {
        if (!enabled) return null;

        PythonAIClient.NegotiationInput input = new PythonAIClient.NegotiationInput(
                ctx.vendorName(), ctx.productSummary(),
                ctx.currentPrice(), decision.targetPrice(), ctx.maxAcceptablePrice(),
                ctx.quantity() != null ? ctx.quantity() : 1,
                ctx.warrantyMonths() != null ? ctx.warrantyMonths() : 12,
                ctx.deliveryDays() != null ? ctx.deliveryDays() : 30,
                24, 30, round, null, null
        );

        Optional<JsonNode> result = client.generateNegotiationEmail(input, approach, decision.strategy(), round);
        return result.map(node -> node.path("email_body").asText(null)).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vendor response analysis
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Enhanced vendor counter-offer evaluation with AI reasoning notes.
     * Spring Boot enforces the actual ACCEPT/REJECT decision based on business rules.
     */
    public VendorResponseResult analyzeVendorResponse(NegotiationContext ctx,
                                                       BigDecimal counterPrice, int round, int maxRounds) {
        if (!enabled) return null;

        String product = ctx.productSummary() != null ? ctx.productSummary() : "items";
        Optional<JsonNode> result = client.analyzeVendorResponse(
                ctx.vendorName(), product,
                ctx.currentPrice(), counterPrice,
                ctx.targetPrice(), ctx.maxAcceptablePrice(),
                round, maxRounds
        );

        return result.map(node -> new VendorResponseResult(
                node.path("recommend_accept").asBoolean(false),
                node.path("decision_reason").asText(""),
                node.path("notes").asText(""),
                node.path("savings_vs_original").asDouble(0),
                node.path("savings_percent").asDouble(0),
                node.path("ai_mode").asText("demo")
        )).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result record types
    // ─────────────────────────────────────────────────────────────────────────

    public record VendorEvaluationResult(double overallScore, String riskLevel,
                                          String recommendation, String summary, String aiMode) {}

    public record QuoteComparisonResult(String recommendedVendorName, String aiRationale, String aiMode) {}

    public record NegotiationStrategyResult(String approach, JsonNode leveragePoints,
                                             JsonNode riskMitigation, String aiMode) {}

    public record VendorResponseResult(boolean recommendAccept, String decisionReason,
                                        String notes, double savingsVsOriginal,
                                        double savingsPercent, String aiMode) {}
}
