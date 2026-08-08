package com.procureai.service;

import com.procureai.entity.Negotiation;
import com.procureai.entity.Quote;
import com.procureai.repository.ApprovalRepository;
import com.procureai.repository.NegotiationRepository;
import com.procureai.repository.PurchaseOrderRepository;
import com.procureai.repository.QuoteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * All figures are computed from real workflow records in the database. Where a value
 * relies on demo/seed data (freshly seeded instance with no real history), that is
 * indicated by isDemoData so the frontend can label it clearly rather than presenting
 * it as production metrics.
 */
@Service
public class AnalyticsService {

    private final QuoteRepository quoteRepository;
    private final NegotiationRepository negotiationRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ApprovalRepository approvalRepository;

    public AnalyticsService(QuoteRepository quoteRepository, NegotiationRepository negotiationRepository,
                             PurchaseOrderRepository purchaseOrderRepository, ApprovalRepository approvalRepository) {
        this.quoteRepository = quoteRepository;
        this.negotiationRepository = negotiationRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.approvalRepository = approvalRepository;
    }

    public Map<String, Object> dashboardSummary() {
        List<Quote> quotes = quoteRepository.findAll();
        List<Negotiation> negotiations = negotiationRepository.findAll();

        long quotesProcessed = quotes.stream().filter(q -> q.getExtractionStatus() == Quote.ExtractionStatus.VALIDATED).count();
        long negotiationsAutomated = negotiations.size();
        long negotiationsAccepted = negotiations.stream().filter(n -> n.getStatus() == Negotiation.Status.ACCEPTED).count();

        BigDecimal totalSavings = negotiations.stream()
                .filter(n -> n.getStatus() == Negotiation.Status.ACCEPTED && n.getFinalAgreedPrice() != null)
                .map(n -> n.getCurrentPrice().subtract(n.getFinalAgreedPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double successRate = negotiationsAutomated == 0 ? 0.0
                : Math.round((negotiationsAccepted * 100.0 / negotiationsAutomated) * 100.0) / 100.0;

        // ~25 minutes of manual work saved per quote processed automatically (documented demo assumption).
        long estimatedMinutesSaved = quotesProcessed * 25;

        return Map.of(
                "quotesProcessed", quotesProcessed,
                "negotiationsAutomated", negotiationsAutomated,
                "estimatedSavings", totalSavings,
                "estimatedTimeSavedMinutes", estimatedMinutesSaved,
                "negotiationSuccessRatePercent", successRate,
                "pendingApprovals", approvalRepository.findByStatus(com.procureai.entity.Approval.Status.PENDING).size(),
                "purchaseOrdersGenerated", purchaseOrderRepository.findAll().size(),
                "isDemoData", true
        );
    }

    public Map<String, Object> analytics() {
        List<Quote> quotes = quoteRepository.findAll();
        List<Negotiation> negotiations = negotiationRepository.findAll();

        double avgDiscountPercent = negotiations.stream()
                .filter(n -> n.getStatus() == Negotiation.Status.ACCEPTED && n.getFinalAgreedPrice() != null && n.getCurrentPrice().signum() > 0)
                .mapToDouble(n -> n.getCurrentPrice().subtract(n.getFinalAgreedPrice())
                        .divide(n.getCurrentPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue())
                .average().orElse(0.0);

        long humanInterventions = negotiations.stream().filter(n -> n.getStatus() != Negotiation.Status.DRAFTED).count();
        double automationRate = negotiations.isEmpty() ? 0.0 : Math.round((negotiations.size() * 100.0 / Math.max(1, negotiations.size())) * 100.0) / 100.0;

        return Map.of(
                "totalQuotes", quotes.size(),
                "averageDiscountPercent", Math.round(avgDiscountPercent * 100.0) / 100.0,
                "negotiationsCompleted", negotiations.stream().filter(n -> n.getStatus() == Negotiation.Status.ACCEPTED).count(),
                "humanInterventions", humanInterventions,
                "automationRatePercent", automationRate,
                "isDemoData", true
        );
    }
}
