package com.procureai.service;

import com.procureai.entity.*;
import com.procureai.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a complete structured procurement evaluation report for a workflow.
 *
 * Adapted from the AI-Powered-RFP-Analyzer's "Evaluation Report Generator Agent"
 * which consolidates all prior agent assessments into a final recommendation.
 * In our Java implementation this consolidates quotes, scoring, negotiation outcome,
 * savings calculation, and the multi-agent intelligence bundle into one report.
 */
@Service
public class EvaluationReportService {

    private final QuoteRepository quoteRepository;
    private final NegotiationRepository negotiationRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AuditLogRepository auditLogRepository;
    private final VendorIntelligenceService intelligenceService;

    public EvaluationReportService(
            QuoteRepository quoteRepository,
            NegotiationRepository negotiationRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            AuditLogRepository auditLogRepository,
            VendorIntelligenceService intelligenceService
    ) {
        this.quoteRepository = quoteRepository;
        this.negotiationRepository = negotiationRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.auditLogRepository = auditLogRepository;
        this.intelligenceService = intelligenceService;
    }

    // ── Report DTOs ───────────────────────────────────────────────────────────

    public record VendorScorecardEntry(
            Long quoteId,
            String vendorName,
            BigDecimal unitPrice,
            BigDecimal totalCost,
            int warrantyMonths,
            int deliveryDays,
            String paymentTerms,
            String benchmarkStatus,
            Double vendorScore,
            String recommendation,
            double intelligenceScore
    ) {}

    public record NegotiationSummary(
            String status,
            BigDecimal originalPrice,
            BigDecimal finalPrice,
            BigDecimal savingsAmount,
            double savingsPercent,
            int roundsCompleted
    ) {}

    public record EvaluationReport(
            Long workflowId,
            String workflowTitle,
            LocalDateTime generatedAt,
            String overallOutcome,         // COMPLETED / IN_PROGRESS / FAILED
            String executiveSummary,
            int totalVendorsEvaluated,
            String recommendedVendor,
            Double recommendedVendorScore,
            List<VendorScorecardEntry> vendorScorecard,
            NegotiationSummary negotiationSummary,
            BigDecimal totalSpend,
            BigDecimal estimatedSavings,
            double savingsPercent,
            List<String> keyFindings,
            List<String> riskFlags,
            String poNumber,
            Long auditEventCount
    ) {}

    // ── Main Report Generation ────────────────────────────────────────────────

    public EvaluationReport generateReport(WorkflowExecution workflow) {
        Long workflowId = workflow.getId();

        List<Quote> quotes = quoteRepository.findByWorkflowId(workflowId).stream()
                .filter(q -> q.getExtractionStatus() == Quote.ExtractionStatus.VALIDATED)
                .sorted(Comparator.comparingDouble(q -> q.getVendorScore() == null ? 0 : -q.getVendorScore()))
                .collect(Collectors.toList());

        List<Negotiation> negotiations = negotiationRepository.findByQuoteWorkflowId(workflowId);
        Negotiation latestNeg = negotiations.stream()
                .max(Comparator.comparing(Negotiation::getCreatedAt))
                .orElse(null);

        List<PurchaseOrder> pos = purchaseOrderRepository.findByWorkflowId(workflowId);
        PurchaseOrder po = pos.isEmpty() ? null : pos.get(0);

        long auditCount = auditLogRepository.countByWorkflowId(workflowId);

        // Build vendor scorecard
        List<VendorScorecardEntry> scorecard = quotes.stream().map(q -> {
            var bundle = intelligenceService.assess(q);
            BigDecimal unitPrice = q.getItems().isEmpty() ? BigDecimal.ZERO : q.getItems().get(0).getUnitPrice();
            int qty = q.getItems().isEmpty() ? 0 : q.getItems().get(0).getQuantity();
            return new VendorScorecardEntry(
                    q.getId(),
                    q.getVendor().getName(),
                    unitPrice,
                    q.getCalculatedTotal() != null ? q.getCalculatedTotal() : BigDecimal.ZERO,
                    q.getWarrantyMonths() != null ? q.getWarrantyMonths() : 0,
                    q.getDeliveryDays() != null ? q.getDeliveryDays() : 0,
                    q.getPaymentTerms() != null ? q.getPaymentTerms() : "Not specified",
                    q.getBenchmarkStatus() != null ? q.getBenchmarkStatus().name() : "UNKNOWN",
                    q.getVendorScore(),
                    bundle.overallRecommendation(),
                    bundle.overallScore()
            );
        }).collect(Collectors.toList());

        // Best quote
        Quote best = quotes.isEmpty() ? null : quotes.get(0);
        String recommendedVendor = best != null ? best.getVendor().getName() : "N/A";
        Double bestScore = best != null ? best.getVendorScore() : null;

        // Negotiation summary
        NegotiationSummary negSummary = buildNegotiationSummary(latestNeg);

        // Savings calculation
        BigDecimal originalTotal = quotes.stream()
                .map(q -> q.getCalculatedTotal() != null ? q.getCalculatedTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal selectedTotal = best != null && best.getCalculatedTotal() != null
                ? best.getCalculatedTotal() : BigDecimal.ZERO;

        BigDecimal competitorMax = quotes.stream()
                .filter(q -> best == null || !q.getId().equals(best.getId()))
                .map(q -> q.getCalculatedTotal() != null ? q.getCalculatedTotal() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(selectedTotal);

        BigDecimal savedVsWorst = competitorMax.subtract(selectedTotal);
        BigDecimal negotiationSavings = negSummary != null ? negSummary.savingsAmount() : BigDecimal.ZERO;
        BigDecimal totalSavings = savedVsWorst.add(negotiationSavings).max(BigDecimal.ZERO);

        double savingsPct = selectedTotal.signum() > 0
                ? totalSavings.divide(competitorMax, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        // Key findings
        List<String> findings = buildKeyFindings(quotes, best, latestNeg, negSummary);
        List<String> riskFlags = buildRiskFlags(quotes, latestNeg);

        // Executive summary
        String executiveSummary = buildExecutiveSummary(quotes, best, latestNeg, po, totalSavings, savingsPct);

        String outcome = po != null ? "COMPLETED"
                : (latestNeg != null && latestNeg.getStatus() == Negotiation.Status.FAILED) ? "NEGOTIATION_FAILED"
                : "IN_PROGRESS";

        return new EvaluationReport(
                workflowId,
                workflow.getTitle(),
                LocalDateTime.now(),
                outcome,
                executiveSummary,
                quotes.size(),
                recommendedVendor,
                bestScore,
                scorecard,
                negSummary,
                selectedTotal,
                totalSavings,
                Math.round(savingsPct * 10.0) / 10.0,
                findings,
                riskFlags,
                po != null ? po.getPoNumber() : null,
                auditCount
        );
    }

    private NegotiationSummary buildNegotiationSummary(Negotiation neg) {
        if (neg == null) return null;
        BigDecimal original = neg.getCurrentPrice() != null ? neg.getCurrentPrice() : BigDecimal.ZERO;
        BigDecimal finalPrice = neg.getFinalAgreedPrice() != null ? neg.getFinalAgreedPrice() : original;
        BigDecimal savings = original.subtract(finalPrice);
        double savingsPct = original.signum() > 0
                ? savings.divide(original, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
        int rounds = neg.getCurrentRound() != null ? neg.getCurrentRound() : 0;
        return new NegotiationSummary(
                neg.getStatus().name(), original, finalPrice, savings,
                Math.round(savingsPct * 10.0) / 10.0, rounds
        );
    }

    private List<String> buildKeyFindings(List<Quote> quotes, Quote best, Negotiation neg, NegotiationSummary negSum) {
        List<String> findings = new ArrayList<>();
        findings.add(quotes.size() + " vendor quotes evaluated and normalized");
        if (best != null) {
            findings.add("Best vendor: " + best.getVendor().getName() +
                    " (score: " + best.getVendorScore() + "/100)");
            findings.add("Benchmark status: " + best.getBenchmarkStatus());
        }
        if (negSum != null && negSum.savingsAmount().signum() > 0) {
            findings.add(String.format("Negotiation savings: ₹%.2f (%.1f%%)",
                    negSum.savingsAmount(), negSum.savingsPercent()));
        }
        if (neg != null) {
            findings.add("Negotiation completed in " + neg.getCurrentRound() + " round(s) with status: " + neg.getStatus());
        }
        return findings;
    }

    private List<String> buildRiskFlags(List<Quote> quotes, Negotiation neg) {
        List<String> flags = new ArrayList<>();
        for (Quote q : quotes) {
            if (q.getWarrantyMonths() != null && q.getWarrantyMonths() < 12) {
                flags.add("⚠️ " + q.getVendor().getName() + ": Warranty below 12 months");
            }
            if (q.getDeliveryDays() != null && q.getDeliveryDays() > 45) {
                flags.add("⚠️ " + q.getVendor().getName() + ": Delivery exceeds 45 days");
            }
            if (q.getBenchmarkStatus() == Quote.BenchmarkStatus.ABOVE) {
                flags.add("⚠️ " + q.getVendor().getName() + ": Priced above market benchmark");
            }
        }
        if (neg != null && neg.getStatus() == Negotiation.Status.FAILED) {
            flags.add("❌ Negotiation failed — vendor did not meet budget requirements");
        }
        return flags;
    }

    private String buildExecutiveSummary(List<Quote> quotes, Quote best, Negotiation neg,
                                          PurchaseOrder po, BigDecimal savings, double savingsPct) {
        StringBuilder sb = new StringBuilder();
        sb.append(quotes.size()).append(" vendor quotes were evaluated. ");
        if (best != null) {
            sb.append(best.getVendor().getName()).append(" was selected as the recommended vendor ")
                    .append("with a composite score of ").append(best.getVendorScore()).append("/100. ");
        }
        if (savings.signum() > 0) {
            sb.append(String.format("Estimated savings of ₹%.2f (%.1f%%) achieved through competitive evaluation",
                    savings, savingsPct));
            if (neg != null && neg.getFinalAgreedPrice() != null) sb.append(" and AI-assisted negotiation");
            sb.append(". ");
        }
        if (po != null) {
            sb.append("Purchase Order ").append(po.getPoNumber()).append(" generated for ₹").append(po.getTotalAmount()).append(".");
        }
        return sb.toString();
    }
}
