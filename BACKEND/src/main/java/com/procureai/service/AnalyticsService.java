package com.procureai.service;

import com.procureai.entity.Negotiation;
import com.procureai.entity.PurchaseOrder;
import com.procureai.entity.Quote;
import com.procureai.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes live, reactive procurement analytics & dashboard metrics from database entities.
 */
@Service
public class AnalyticsService {

    private final QuoteRepository quoteRepository;
    private final NegotiationRepository negotiationRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ApprovalRepository approvalRepository;
    private final WorkflowExecutionRepository workflowRepository;

    public AnalyticsService(QuoteRepository quoteRepository, NegotiationRepository negotiationRepository,
                             PurchaseOrderRepository purchaseOrderRepository, ApprovalRepository approvalRepository,
                             WorkflowExecutionRepository workflowRepository) {
        this.quoteRepository = quoteRepository;
        this.negotiationRepository = negotiationRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.approvalRepository = approvalRepository;
        this.workflowRepository = workflowRepository;
    }

    public Map<String, Object> dashboardSummary() {
        List<Quote> quotes = quoteRepository.findAll();
        List<Negotiation> negotiations = negotiationRepository.findAll();
        List<PurchaseOrder> pos = purchaseOrderRepository.findAll();

        long quotesProcessed = quotes.stream().filter(q -> q.getExtractionStatus() == Quote.ExtractionStatus.VALIDATED).count();
        long negotiationsAutomated = negotiations.size();
        long negotiationsAccepted = negotiations.stream().filter(n -> n.getStatus() == Negotiation.Status.ACCEPTED).count();

        BigDecimal totalSpend = pos.stream()
                .map(PurchaseOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSpend.compareTo(BigDecimal.ZERO) == 0 && !quotes.isEmpty()) {
            totalSpend = quotes.stream()
                    .map(Quote::getCalculatedTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal totalSavings = negotiations.stream()
                .filter(n -> n.getFinalAgreedPrice() != null && n.getCurrentPrice() != null)
                .map(n -> n.getCurrentPrice().subtract(n.getFinalAgreedPrice()))
                .filter(diff -> diff.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSavings.compareTo(BigDecimal.ZERO) == 0 && !negotiations.isEmpty()) {
            totalSavings = negotiations.stream()
                    .filter(n -> n.getCurrentPrice() != null && n.getTargetPrice() != null)
                    .map(n -> n.getCurrentPrice().subtract(n.getTargetPrice()))
                    .filter(diff -> diff.signum() > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        long pendingApprovalsCount = negotiations.stream()
                .filter(n -> n.getStatus() == Negotiation.Status.PENDING_APPROVAL || n.getStatus() == Negotiation.Status.DRAFTED)
                .count() + approvalRepository.findByStatus(com.procureai.entity.Approval.Status.PENDING).size();

        double successRate = negotiationsAutomated == 0 ? 0.0
                : Math.round((negotiationsAccepted * 100.0 / negotiationsAutomated) * 100.0) / 100.0;

        long estimatedMinutesSaved = quotesProcessed * 25;

        // Dynamic category spend computation
        Map<String, BigDecimal> catSpendMap = new HashMap<>();
        for (PurchaseOrder po : pos) {
            String pName = (po.getQuote() != null && po.getQuote().getItems() != null && !po.getQuote().getItems().isEmpty())
                    ? po.getQuote().getItems().get(0).getProductName() : null;
            String vCat = po.getVendor() != null ? po.getVendor().getCategory() : null;
            String cat = extractCategory(pName, vCat);
            catSpendMap.merge(cat, po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO, BigDecimal::add);
        }
        if (catSpendMap.isEmpty()) {
            for (Quote q : quotes) {
                String pName = (q.getItems() != null && !q.getItems().isEmpty()) ? q.getItems().get(0).getProductName() : null;
                String vCat = q.getVendor() != null ? q.getVendor().getCategory() : null;
                String cat = extractCategory(pName, vCat);
                catSpendMap.merge(cat, q.getCalculatedTotal() != null ? q.getCalculatedTotal() : BigDecimal.ZERO, BigDecimal::add);
            }
        }
        if (catSpendMap.isEmpty()) {
            catSpendMap.put("Laptops", BigDecimal.ZERO);
            catSpendMap.put("Displays & TVs", BigDecimal.ZERO);
        }

        List<Map<String, Object>> spendByCategory = catSpendMap.entrySet().stream()
                .map(e -> Map.<String, Object>of("category", e.getKey(), "amount", e.getValue()))
                .toList();

        Map<String, Object> map = new HashMap<>();
        map.put("quotesProcessed", quotesProcessed);
        map.put("negotiationsAutomated", negotiationsAutomated);
        map.put("totalSpend", totalSpend);
        map.put("totalSavings", totalSavings);
        map.put("estimatedSavings", totalSavings);
        map.put("totalWorkflows", workflowRepository.count());
        map.put("completedWorkflows", pos.size());
        map.put("estimatedTimeSavedMinutes", estimatedMinutesSaved);
        map.put("negotiationSuccessRatePercent", successRate);
        map.put("pendingApprovals", pendingApprovalsCount);
        map.put("purchaseOrdersGenerated", pos.size());
        map.put("spendByCategory", spendByCategory);
        map.put("isDemoData", false);
        return map;
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

        Map<String, Object> map = new HashMap<>();
        map.put("totalQuotes", quotes.size());
        map.put("averageDiscountPercent", Math.round(avgDiscountPercent * 100.0) / 100.0);
        map.put("negotiationsCompleted", negotiations.stream().filter(n -> n.getStatus() == Negotiation.Status.ACCEPTED).count());
        map.put("humanInterventions", humanInterventions);
        map.put("automationRatePercent", automationRate);
        map.put("isDemoData", false);
        return map;
    }

    public static String extractCategory(String productName, String vendorCategory) {
        if (vendorCategory != null && !vendorCategory.isBlank() 
                && !vendorCategory.equalsIgnoreCase("General") 
                && !vendorCategory.equalsIgnoreCase("Laptops")) {
            return vendorCategory;
        }
        if (productName == null || productName.isBlank()) {
            return vendorCategory != null && !vendorCategory.isBlank() ? vendorCategory : "Laptops";
        }
        String lower = productName.toLowerCase();
        if (lower.contains("tv") || lower.contains("television") || lower.contains("oled") 
                || lower.contains("led") || lower.contains("display") || lower.contains("screen") 
                || lower.contains("smart tv") || lower.contains("monitor")) {
            return "Displays & TVs";
        }
        if (lower.contains("server") || lower.contains("datacenter") || lower.contains("rack") || lower.contains("infrastructure")) {
            return "Servers";
        }
        if (lower.contains("software") || lower.contains("license") || lower.contains("saas") || lower.contains("cloud")) {
            return "Software";
        }
        if (lower.contains("furniture") || lower.contains("chair") || lower.contains("desk") || lower.contains("table")) {
            return "Furniture";
        }
        if (lower.contains("phone") || lower.contains("mobile") || lower.contains("iphone") || lower.contains("galaxy") || lower.contains("smartphone")) {
            return "Mobile Devices";
        }
        if (lower.contains("laptop") || lower.contains("thinkpad") || lower.contains("latitude") || lower.contains("macbook") || lower.contains("notebook") || lower.contains("computing")) {
            return "Laptops";
        }

        String[] words = productName.trim().split("\\s+");
        if (words.length > 0) {
            String word = words[0].replaceAll("[^a-zA-Z0-9]", "");
            if (!word.isBlank() && word.length() > 2) {
                return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
            }
        }
        return "Electronics";
    }
}
