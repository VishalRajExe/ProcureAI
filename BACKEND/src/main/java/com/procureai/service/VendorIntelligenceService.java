package com.procureai.service;

import com.procureai.entity.Quote;
import com.procureai.entity.Vendor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-agent procurement intelligence engine.
 *
 * Adapted from the AI-Powered-RFP-Analyzer's sequential 6-agent pattern:
 *   RFP Compliance → Legal Compliance → Vendor Evaluation →
 *   Market Intelligence → Negotiation Strategy → Evaluation Report
 *
 * Each "agent" here is a deterministic rule-based assessor. Scores and risk
 * levels are computed by the backend — the AI may later explain them but
 * never invents the numbers.
 */
@Service
public class VendorIntelligenceService {

    private final MarketIntelligenceService marketIntelligence;

    public VendorIntelligenceService(MarketIntelligenceService marketIntelligence) {
        this.marketIntelligence = marketIntelligence;
    }

    // ── Assessment Records ────────────────────────────────────────────────────

    /**
     * RFP Compliance: Does the quote meet the procurement requirements?
     * Score 1-10: 10 = Fully Compliant, 1 = Major Non-Compliance.
     */
    public record RFPComplianceAssessment(
            int score,                   // 1-10
            String complianceLevel,      // FULL / PARTIAL / NON_COMPLIANT
            List<String> metRequirements,
            List<String> missingRequirements,
            String summary
    ) {}

    /**
     * Vendor Reputation: Historical reliability, trust, and credibility.
     * Score 1-10: 10 = Highly Reputable, 1 = Major Concerns.
     */
    public record VendorReputationAssessment(
            int score,
            String reputationLevel,      // EXCELLENT / GOOD / MODERATE / POOR
            double reliabilityScore,
            boolean trustSealVerified,
            int yearsExperience,
            String notes
    ) {}

    /**
     * Market Intelligence: Industry trends, price positioning, supply risk.
     * Risk: LOW / MEDIUM / HIGH
     */
    public record MarketIntelligenceReport(
            String marketRisk,           // LOW / MEDIUM / HIGH
            String pricePositioning,     // BELOW_MARKET / AT_MARKET / ABOVE_MARKET
            BigDecimal marketMedianPrice,
            BigDecimal savingsVsMarket,
            List<String> industryTrends,
            List<String> supplyChainRisks,
            String summary
    ) {}

    /**
     * Legal & Compliance Check: Payment terms, warranty, GST, delivery SLA.
     * Risk: LOW / MEDIUM / HIGH
     */
    public record LegalComplianceResult(
            String overallRisk,          // LOW / MEDIUM / HIGH
            List<ComplianceCriterion> criteria,
            String summary
    ) {
        public record ComplianceCriterion(String name, String status, String detail) {}
    }

    /**
     * Negotiation Strategy: DEFENSIVE / BALANCED / AGGRESSIVE based on all prior assessments.
     */
    public record NegotiationStrategyReport(
            String approach,             // DEFENSIVE / BALANCED / AGGRESSIVE
            String leveragePoints,
            String riskMitigation,
            BigDecimal suggestedTargetPrice,
            BigDecimal walkAwayPrice,
            String rationale
    ) {}

    /**
     * Full intelligence bundle for one quote.
     */
    public record VendorIntelligenceBundle(
            Long quoteId,
            String vendorName,
            RFPComplianceAssessment rfpCompliance,
            VendorReputationAssessment reputation,
            MarketIntelligenceReport marketIntelligence,
            LegalComplianceResult legalCompliance,
            NegotiationStrategyReport negotiationStrategy,
            double overallScore,         // 1-10
            String overallRecommendation
    ) {}

    // ── Main Assessment Entry Point ───────────────────────────────────────────

    public VendorIntelligenceBundle assess(Quote quote) {
        Vendor vendor = quote.getVendor();

        RFPComplianceAssessment rfp = assessRFPCompliance(quote);
        VendorReputationAssessment reputation = assessReputation(vendor);
        MarketIntelligenceReport market = assessMarket(quote);
        LegalComplianceResult legal = assessLegalCompliance(quote);
        NegotiationStrategyReport strategy = buildNegotiationStrategy(quote, rfp, reputation, market, legal);

        // Weighted overall score: rfp 30%, reputation 25%, market 20%, legal 15%, price 10%
        double overallScore = (
                rfp.score() * 0.30 +
                reputation.score() * 0.25 +
                marketRiskToScore(market.marketRisk()) * 0.20 +
                legalRiskToScore(legal.overallRisk()) * 0.15 +
                (quote.getVendorScore() != null ? quote.getVendorScore() / 10.0 : 5.0) * 0.10
        );
        overallScore = Math.round(overallScore * 10.0) / 10.0;

        String recommendation = overallScore >= 7.5 ? "RECOMMEND"
                : overallScore >= 5.0 ? "CONSIDER_WITH_CONDITIONS"
                : "NOT_RECOMMENDED";

        return new VendorIntelligenceBundle(
                quote.getId(), vendor.getName(),
                rfp, reputation, market, legal, strategy,
                overallScore, recommendation
        );
    }

    // ── Agent 1: RFP Compliance ───────────────────────────────────────────────

    private RFPComplianceAssessment assessRFPCompliance(Quote quote) {
        List<String> met = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        // Check warranty requirement (min 24 months)
        int warrantyMonths = quote.getWarrantyMonths() != null ? quote.getWarrantyMonths() : 0;
        if (warrantyMonths >= 24) met.add("Warranty >= 24 months (" + warrantyMonths + "m)");
        else missing.add("Warranty below 24 months (found: " + warrantyMonths + "m)");

        // Check delivery SLA (max 30 days)
        int deliveryDays = quote.getDeliveryDays() != null ? quote.getDeliveryDays() : 999;
        if (deliveryDays <= 30) met.add("Delivery <= 30 days (" + deliveryDays + "d)");
        else missing.add("Delivery exceeds 30 days (found: " + deliveryDays + "d)");

        // Check payment terms
        String terms = quote.getPaymentTerms();
        if (terms != null && !terms.isBlank()) met.add("Payment terms specified: " + terms);
        else missing.add("Payment terms not specified");

        // Check items populated
        if (quote.getItems() != null && !quote.getItems().isEmpty()) met.add("Line items present");
        else missing.add("No line items in quote");

        // Check unit price > 0
        boolean priceOk = quote.getItems() != null && !quote.getItems().isEmpty()
                && quote.getItems().get(0).getUnitPrice() != null
                && quote.getItems().get(0).getUnitPrice().signum() > 0;
        if (priceOk) met.add("Unit price provided");
        else missing.add("Missing or zero unit price");

        int score;
        String level;
        if (missing.isEmpty()) { score = 9; level = "FULL"; }
        else if (missing.size() == 1) { score = 7; level = "PARTIAL"; }
        else if (missing.size() == 2) { score = 5; level = "PARTIAL"; }
        else { score = 2; level = "NON_COMPLIANT"; }

        return new RFPComplianceAssessment(score, level, met, missing,
                level + " compliance: " + met.size() + "/" + (met.size() + missing.size()) + " criteria met.");
    }

    // ── Agent 2: Vendor Reputation ────────────────────────────────────────────

    private VendorReputationAssessment assessReputation(Vendor vendor) {
        double reliability = vendor.getReliabilityScore() != null ? vendor.getReliabilityScore() : 75.0;
        boolean trustSeal = vendor.getTrustSealVerified() != null && vendor.getTrustSealVerified();
        int yearsExp = vendor.getYearsExperience() != null ? vendor.getYearsExperience() : 0;

        int score;
        String level;
        if (reliability >= 90 && trustSeal) { score = 9; level = "EXCELLENT"; }
        else if (reliability >= 75) { score = 7; level = "GOOD"; }
        else if (reliability >= 60) { score = 5; level = "MODERATE"; }
        else { score = 3; level = "POOR"; }

        if (yearsExp >= 10) score = Math.min(10, score + 1);
        if (trustSeal) score = Math.min(10, score + 1);

        String notes = String.format(
                "Reliability: %.0f%%. TrustSeal: %s. Experience: %d years.",
                reliability, trustSeal ? "Verified" : "Not verified", yearsExp
        );

        return new VendorReputationAssessment(score, level, reliability, trustSeal, yearsExp, notes);
    }

    // ── Agent 3: Market Intelligence ──────────────────────────────────────────

    private MarketIntelligenceReport assessMarket(Quote quote) {
        String category = quote.getItems() != null && !quote.getItems().isEmpty()
                ? quote.getItems().get(0).getProductName()
                : "general";

        MarketIntelligenceService.MarketData market = marketIntelligence.getMarketData(category);

        BigDecimal quoteUnitPrice = (quote.getItems() != null && !quote.getItems().isEmpty())
                ? quote.getItems().get(0).getUnitPrice()
                : BigDecimal.ZERO;

        String positioning = "AT_MARKET";
        BigDecimal savings = BigDecimal.ZERO;
        String marketRisk = "LOW";

        if (market != null) {
            BigDecimal median = market.medianPrice();
            if (quoteUnitPrice.compareTo(median.multiply(new BigDecimal("0.95"))) < 0) {
                positioning = "BELOW_MARKET";
                savings = median.subtract(quoteUnitPrice);
            } else if (quoteUnitPrice.compareTo(median.multiply(new BigDecimal("1.05"))) > 0) {
                positioning = "ABOVE_MARKET";
                savings = quoteUnitPrice.subtract(median).negate();
            }
            marketRisk = market.supplyChainRisk();
        }

        List<String> trends = market != null ? market.trends() : List.of("Market data unavailable");
        List<String> risks = market != null ? market.supplyChainRisks() : List.of("Unable to assess supply chain risk");

        String summary = String.format("Quote is %s. Market risk: %s.", positioning, marketRisk);

        return new MarketIntelligenceReport(marketRisk, positioning,
                market != null ? market.medianPrice() : BigDecimal.ZERO,
                savings, trends, risks, summary);
    }

    // ── Agent 4: Legal & Compliance ───────────────────────────────────────────

    private LegalComplianceResult assessLegalCompliance(Quote quote) {
        List<LegalComplianceResult.ComplianceCriterion> criteria = new ArrayList<>();
        int warnCount = 0;
        int failCount = 0;

        // Payment terms policy: no 100% advance payment allowed
        String terms = quote.getPaymentTerms() != null ? quote.getPaymentTerms().toLowerCase() : "";
        if (terms.contains("advance") && !terms.contains("30") && !terms.contains("net")) {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "Payment Terms", "WARN", "Advance payment terms detected — procurement policy prefers Net 30+"));
            warnCount++;
        } else {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "Payment Terms", "PASS", "Acceptable payment terms: " + (terms.isBlank() ? "Not specified" : terms)));
        }

        // Warranty compliance: minimum 24 months required
        int months = quote.getWarrantyMonths() != null ? quote.getWarrantyMonths() : 0;
        if (months == 0) {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "Warranty", "FAIL", "No warranty information provided"));
            failCount++;
        } else if (months < 12) {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "Warranty", "WARN", "Warranty below 12 months (" + months + "m) — below minimum recommendation"));
            warnCount++;
        } else {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "Warranty", "PASS", "Warranty: " + months + " months — compliant"));
        }

        // Tax/GST presence
        BigDecimal tax = quote.getTaxPercent();
        if (tax == null || tax.signum() == 0) {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "GST/Tax", "WARN", "No GST/tax declared — verify if GST is included in unit price"));
            warnCount++;
        } else {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "GST/Tax", "PASS", "GST declared at " + tax + "%"));
        }

        // Delivery SLA
        int delivery = quote.getDeliveryDays() != null ? quote.getDeliveryDays() : -1;
        if (delivery < 0) {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "Delivery SLA", "WARN", "Delivery timeframe not specified"));
            warnCount++;
        } else if (delivery > 45) {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "Delivery SLA", "FAIL", "Delivery > 45 days (" + delivery + "d) — exceeds acceptable SLA"));
            failCount++;
        } else {
            criteria.add(new LegalComplianceResult.ComplianceCriterion(
                    "Delivery SLA", "PASS", "Delivery: " + delivery + " days"));
        }

        String overall = failCount > 0 ? "HIGH" : warnCount >= 2 ? "MEDIUM" : "LOW";
        String summary = String.format("Overall legal risk: %s. %d issues, %d warnings.", overall, failCount, warnCount);

        return new LegalComplianceResult(overall, criteria, summary);
    }

    // ── Agent 5: Negotiation Strategy ─────────────────────────────────────────

    private NegotiationStrategyReport buildNegotiationStrategy(
            Quote quote, RFPComplianceAssessment rfp, VendorReputationAssessment reputation,
            MarketIntelligenceReport market, LegalComplianceResult legal) {

        BigDecimal currentPrice = quote.getCalculatedTotal() != null
                ? quote.getCalculatedTotal() : BigDecimal.ZERO;

        // Approach based on combined risk signals
        boolean vendorStrong = reputation.score() >= 8;
        boolean priceAboveMarket = "ABOVE_MARKET".equals(market.pricePositioning());
        boolean highRisk = "HIGH".equals(legal.overallRisk()) || "HIGH".equals(market.marketRisk());
        boolean alternatives = true; // In a real system, check if there are competing quotes

        String approach;
        String leveragePoints;
        String riskMitigation;

        if (highRisk) {
            approach = "DEFENSIVE";
            leveragePoints = "Significant compliance/risk flags identified. Require vendor clarification before committing.";
            riskMitigation = "Demand performance bonds, milestone-based payment, and legal compliance certificates.";
        } else if (vendorStrong && !priceAboveMarket) {
            approach = "BALANCED";
            leveragePoints = "Strong vendor but price is competitive. Use order volume and long-term relationship as leverage.";
            riskMitigation = "Lock in warranty and delivery terms contractually. Request price validity extension.";
        } else {
            approach = "AGGRESSIVE";
            leveragePoints = "Multiple competing vendors available. Price is above market median. " +
                    (alternatives ? "Alternative vendors identified." : "");
            riskMitigation = "Set firm walk-away price. Use competitor quotes as leverage in negotiation.";
        }

        // Target: 8% below current; walk-away: 5% below current (if above market)
        BigDecimal targetDiscount = priceAboveMarket ? new BigDecimal("0.08") : new BigDecimal("0.05");
        BigDecimal suggestedTarget = currentPrice.multiply(BigDecimal.ONE.subtract(targetDiscount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal walkAway = currentPrice.multiply(new BigDecimal("0.95"))
                .setScale(2, RoundingMode.HALF_UP);

        String rationale = String.format(
                "%s approach selected. Vendor reputation: %s. Price positioning: %s. Legal risk: %s.",
                approach, reputation.reputationLevel(), market.pricePositioning(), legal.overallRisk()
        );

        return new NegotiationStrategyReport(approach, leveragePoints, riskMitigation,
                suggestedTarget, walkAway, rationale);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double marketRiskToScore(String risk) {
        return switch (risk) {
            case "LOW" -> 9.0;
            case "MEDIUM" -> 6.0;
            default -> 3.0;
        };
    }

    private double legalRiskToScore(String risk) {
        return switch (risk) {
            case "LOW" -> 9.0;
            case "MEDIUM" -> 6.0;
            default -> 2.0;
        };
    }
}
