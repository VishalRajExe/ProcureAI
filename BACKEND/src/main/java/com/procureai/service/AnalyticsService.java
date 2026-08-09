package com.procureai.service;

import com.procureai.entity.Negotiation;
import com.procureai.entity.PurchaseOrder;
import com.procureai.entity.Quote;
import com.procureai.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        return dashboardSummary(com.procureai.util.CurrentUser.id());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary(Long userId) {
        List<Quote> quotes = userId != null ? quoteRepository.findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(userId) : quoteRepository.findAll();
        List<Negotiation> negotiations = userId != null ? negotiationRepository.findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(userId) : negotiationRepository.findAll();
        List<PurchaseOrder> pos = userId != null ? purchaseOrderRepository.findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(userId) : purchaseOrderRepository.findAll();
        long totalWorkflows = userId != null ? workflowRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId).size() : workflowRepository.count();

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
                .count();

        double successRate = negotiationsAutomated == 0 ? 0.0
                : Math.round((negotiationsAccepted * 100.0 / negotiationsAutomated) * 100.0) / 100.0;

        long estimatedMinutesSaved = quotesProcessed * 25;

        // Dynamic category spend computation
        Map<String, BigDecimal> catSpendMap = new HashMap<>();
        for (PurchaseOrder po : pos) {
            String pName = null;
            if (po.getItems() != null && !po.getItems().isEmpty()) {
                pName = po.getItems().get(0).getProductName();
            } else if (po.getSourceQuote() != null && po.getSourceQuote().getItems() != null && !po.getSourceQuote().getItems().isEmpty()) {
                pName = po.getSourceQuote().getItems().get(0).getProductName();
            }
            String vCat = po.getVendor() != null ? po.getVendor().getCategory() : null;
            String vName = po.getVendor() != null ? po.getVendor().getName() : null;
            String cat = extractCategory(pName, vCat, vName);
            catSpendMap.merge(cat, po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO, BigDecimal::add);
        }

        for (Quote q : quotes) {
            boolean inPo = pos.stream().anyMatch(po -> po.getSourceQuote() != null && po.getSourceQuote().getId().equals(q.getId()));
            if (!inPo) {
                String pName = (q.getItems() != null && !q.getItems().isEmpty()) ? q.getItems().get(0).getProductName() : null;
                String vCat = q.getVendor() != null ? q.getVendor().getCategory() : null;
                String vName = q.getVendor() != null ? q.getVendor().getName() : null;
                String cat = extractCategory(pName, vCat, vName);
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
        map.put("totalWorkflows", totalWorkflows);
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
        return analytics(com.procureai.util.CurrentUser.id());
    }

    public Map<String, Object> analytics(Long userId) {
        List<Quote> quotes = userId != null ? quoteRepository.findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(userId) : quoteRepository.findAll();
        List<Negotiation> negotiations = userId != null ? negotiationRepository.findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(userId) : negotiationRepository.findAll();

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
        return extractCategory(productName, vendorCategory, null);
    }

    public static String extractCategory(String productName, String vendorCategory, String vendorName) {
        String combined = ((productName != null ? productName : "") + " " + (vendorName != null ? vendorName : "")).toLowerCase();

        if (combined.contains("tv") || combined.contains("television") || combined.contains("oled") 
                || combined.contains("led") || combined.contains("display") || combined.contains("screen") 
                || combined.contains("smart tv") || combined.contains("monitor") || combined.contains("lg")) {
            return "Displays & TVs";
        }
        if (combined.contains("server") || combined.contains("datacenter") || combined.contains("rack") || combined.contains("infrastructure")) {
            return "Servers";
        }
        if (combined.contains("software") || combined.contains("license") || combined.contains("saas") || combined.contains("cloud")) {
            return "Software";
        }
        if (combined.contains("furniture") || combined.contains("chair") || combined.contains("desk") || combined.contains("table")) {
            return "Furniture";
        }
        if (combined.contains("phone") || combined.contains("mobile") || combined.contains("iphone") || combined.contains("galaxy") || combined.contains("smartphone")) {
            return "Mobile Devices";
        }
        if (combined.contains("laptop") || combined.contains("thinkpad") || combined.contains("latitude") || combined.contains("macbook") || combined.contains("notebook") || combined.contains("computing") || combined.contains("dell") || combined.contains("lenovo") || combined.contains("hp")) {
            return "Laptops";
        }

        if (vendorCategory != null && !vendorCategory.isBlank() && !vendorCategory.equalsIgnoreCase("General")) {
            return vendorCategory;
        }

        if (productName != null && !productName.isBlank()) {
            String[] words = productName.trim().split("\\s+");
            if (words.length > 0) {
                String word = words[0].replaceAll("[^a-zA-Z0-9]", "");
                if (!word.isBlank() && word.length() > 2) {
                    return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                }
            }
        }
        return "Electronics";
    }
}
