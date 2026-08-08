package com.procureai.service.ai;

import com.procureai.entity.Quote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, offline AI provider used whenever no real AI API key is configured
 * (and always available as a safe fallback). It performs genuinely useful structured
 * reasoning using rule-based heuristics — it does not return random/fake output, and
 * every result still passes through the same backend validation as a real provider's
 * output would.
 */
@Component
public class MockAIProvider implements AIProvider {

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public ExtractedQuoteData extractQuoteData(String rawDocumentText, String hintedVendorName) {
        // In the real pipeline this text has already been produced by the document/OCR
        // service. The mock provider parses simple "key: value" style lines heuristically,
        // which is sufficient for the demo document formats, and reports missing fields
        // explicitly rather than guessing.
        List<String> missing = new ArrayList<>();
        String vendor = hintedVendorName != null ? hintedVendorName : firstNonNull(extractValue(rawDocumentText, "Vendor"), "Unknown Vendor");
        if (vendor.equals("Unknown Vendor")) missing.add("vendorName");

        String product = firstNonNull(extractValue(rawDocumentText, "Product"), extractValue(rawDocumentText, "Model"));
        if (product == null) { product = "Unknown Product"; missing.add("product"); }

        Integer qty = parseInt(extractValue(rawDocumentText, "Quantity"));
        if (qty == null) { qty = 1; missing.add("quantity"); }

        BigDecimal unitPrice = parseDecimal(extractValue(rawDocumentText, "Unit Price"));
        if (unitPrice == null) { unitPrice = BigDecimal.ZERO; missing.add("unitPrice"); }

        BigDecimal discount = firstNonNullDecimal(parseDecimal(extractValue(rawDocumentText, "Discount")), BigDecimal.ZERO);
        BigDecimal tax = firstNonNullDecimal(parseDecimal(extractValue(rawDocumentText, "GST")), parseDecimal(extractValue(rawDocumentText, "Tax")));
        if (tax == null) { tax = BigDecimal.ZERO; missing.add("taxPercent"); }

        BigDecimal shipping = firstNonNullDecimal(parseDecimal(extractValue(rawDocumentText, "Shipping")), BigDecimal.ZERO);

        Integer warranty = parseWarrantyMonths(extractValue(rawDocumentText, "Warranty"));
        Integer delivery = parseInt(extractValue(rawDocumentText, "Delivery"));

        String paymentTerms = firstNonNull(extractValue(rawDocumentText, "Payment Terms"), "Net 30");

        double confidence = missing.isEmpty() ? 0.95 : Math.max(0.4, 0.95 - (0.12 * missing.size()));

        return new ExtractedQuoteData(
                vendor,
                List.of(new ExtractedQuoteData.Item(product, product, qty, unitPrice)),
                discount,
                tax,
                shipping,
                null, // vendor declared total intentionally not trusted/parsed as authoritative
                warranty,
                delivery,
                paymentTerms,
                null,
                confidence,
                missing
        );
    }

    @Override
    public NegotiationDecision decideNegotiationStrategy(NegotiationContext ctx) {
        BigDecimal current = ctx.currentPrice();
        BigDecimal max = ctx.maxAcceptablePrice();
        BigDecimal target = ctx.targetPrice();

        // Rule-based reasoning: if already at/below target, accept; if above max, reject;
        // otherwise negotiate toward target, bounded strictly by max.
        if (current.compareTo(target) <= 0) {
            return new NegotiationDecision(
                    NegotiationDecision.Action.ACCEPT, target, max,
                    "Accept immediately",
                    "Current price of " + fmt(current) + " already meets or beats the target of " + fmt(target) + ".",
                    0.92);
        }
        if (current.compareTo(max) > 0 && target.compareTo(max) > 0) {
            return new NegotiationDecision(
                    NegotiationDecision.Action.REJECT, target, max,
                    "Reject — outside budget",
                    "Current price of " + fmt(current) + " exceeds the maximum acceptable price of " + fmt(max) + " with no viable target inside budget.",
                    0.85);
        }

        BigDecimal gap = current.subtract(target);
        BigDecimal askPrice = current.subtract(gap.multiply(new BigDecimal("0.6"))).setScale(2, RoundingMode.HALF_UP);
        if (askPrice.compareTo(max) > 0) askPrice = max;

        String strategy = "Anchor near target price, cite benchmark range and order volume ("
                + ctx.quantity() + " units) as leverage, request response within 3 business days.";
        String reason = "Current price " + fmt(current) + " is above target " + fmt(target)
                + " but a negotiated price is achievable within the approved maximum of " + fmt(max) + ".";

        return new NegotiationDecision(NegotiationDecision.Action.NEGOTIATE, askPrice, max, strategy, reason, 0.8);
    }

    @Override
    public String draftNegotiationEmail(NegotiationContext ctx, NegotiationDecision decision) {
        return """
                Subject: Quotation Discussion — %s (Qty: %d)

                Dear %s Team,

                Thank you for your quotation for %s. We have reviewed the offer of %s per the terms provided.

                Based on current market conditions and reference pricing, we would like to request a revised price of %s per unit for a confirmed order of %d units. We remain committed to a quick decision and a long-term relationship with your organization.

                Could you please confirm your best possible price by return? We look forward to finalizing this order promptly.

                Best regards,
                Procurement Team
                """.formatted(
                        ctx.productSummary(), ctx.quantity(),
                        ctx.vendorName(),
                        ctx.productSummary(), fmt(ctx.currentPrice()),
                        fmt(decision.targetPrice()), ctx.quantity()
                );
    }

    @Override
    public RoundEvaluation evaluateVendorResponse(NegotiationContext ctx, BigDecimal vendorCounterPrice, int roundNumber) {
        boolean withinMax = vendorCounterPrice.compareTo(ctx.maxAcceptablePrice()) <= 0;
        boolean nearTarget = vendorCounterPrice.subtract(ctx.targetPrice()).abs()
                .compareTo(ctx.targetPrice().multiply(new BigDecimal("0.03"))) <= 0;

        String notes;
        boolean recommend;
        if (withinMax && (nearTarget || roundNumber >= 2)) {
            recommend = true;
            notes = "Vendor counter of " + fmt(vendorCounterPrice) + " is within the approved maximum of "
                    + fmt(ctx.maxAcceptablePrice()) + "; recommending acceptance to close the negotiation.";
        } else if (!withinMax) {
            recommend = false;
            notes = "Vendor counter of " + fmt(vendorCounterPrice) + " exceeds the approved maximum of "
                    + fmt(ctx.maxAcceptablePrice()) + "; cannot recommend acceptance.";
        } else {
            recommend = false;
            notes = "Vendor counter of " + fmt(vendorCounterPrice) + " is within budget but still above target ("
                    + fmt(ctx.targetPrice()) + "); recommending one more negotiation round.";
        }
        return new RoundEvaluation(recommend, notes, 0.82);
    }

    @Override
    public String explainRecommendation(Quote recommendedQuote, String comparisonSummary) {
        return "Recommended vendor " + recommendedQuote.getVendor().getName()
                + " based on the lowest verified actual total cost combined with acceptable warranty and delivery terms. "
                + comparisonSummary;
    }

    // ---- small parsing helpers (heuristic, demo-grade) ----

    private String extractValue(String text, String label) {
        if (text == null) return null;
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith(label.toLowerCase() + ":")) {
                return trimmed.substring(label.length() + 1).trim();
            }
        }
        return null;
    }

    private Integer parseInt(String s) {
        if (s == null) return null;
        try {
            String digits = s.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? null : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseWarrantyMonths(String s) {
        if (s == null) return null;
        Integer n = parseInt(s);
        if (n == null) return null;
        return s.toLowerCase().contains("year") ? n * 12 : n;
    }

    private BigDecimal parseDecimal(String s) {
        if (s == null) return null;
        try {
            String cleaned = s.replaceAll("[^0-9.]", "");
            return cleaned.isEmpty() ? null : new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private BigDecimal firstNonNullDecimal(BigDecimal a, BigDecimal b) {
        return a != null ? a : b;
    }

    private String fmt(BigDecimal v) {
        return "₹" + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
