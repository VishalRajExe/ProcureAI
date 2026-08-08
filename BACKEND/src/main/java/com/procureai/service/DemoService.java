package com.procureai.service;

import com.procureai.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Runs complete ProcureAI workflow scenarios end-to-end (HP, Lenovo, or Dell winning options).
 */
@Service
public class DemoService {

    private final QuoteService quoteService;
    private final ComparisonService comparisonService;
    private final NegotiationService negotiationService;
    private final PurchaseOrderService purchaseOrderService;
    private final AuditService auditService;

    public DemoService(QuoteService quoteService, ComparisonService comparisonService, NegotiationService negotiationService,
                        PurchaseOrderService purchaseOrderService, AuditService auditService) {
        this.quoteService = quoteService;
        this.comparisonService = comparisonService;
        this.negotiationService = negotiationService;
        this.purchaseOrderService = purchaseOrderService;
        this.auditService = auditService;
    }

    @Transactional
    public Map<String, Object> runDemo(Long demoUserId) {
        return runDemo(demoUserId, "HP");
    }

    @Transactional
    public Map<String, Object> runDemo(Long demoUserId, String scenarioVendor) {
        String vendorChoice = (scenarioVendor != null && !scenarioVendor.isBlank())
                ? scenarioVendor.toUpperCase() : "HP";

        String title;
        String lenovoText;
        String hpText;
        String dellText;

        if (vendorChoice.contains("LENOVO")) {
            title = "Procurement Scenario — Lenovo Corporate Winner";
            lenovoText = """
                    Vendor: Lenovo Corporate Sales
                    Product: Lenovo ThinkPad P16 Gen 2
                    Quantity: 50
                    Unit Price: 58000
                    GST: 18
                    Shipping: 0
                    Warranty: 4 years
                    Delivery: 8 days
                    Payment Terms: Net 30
                    """;
            hpText = hpRawText();
            dellText = dellRawText();
        } else if (vendorChoice.contains("DELL")) {
            title = "Procurement Scenario — Dell Direct Winner";
            dellText = """
                    Vendor: Dell Direct Enterprise
                    Product: Dell Latitude 5450
                    Quantity: 50
                    Unit Price: 55000
                    GST: 18
                    Shipping: 0
                    Warranty: 3 years
                    Delivery: 10 days
                    Payment Terms: Net 30
                    """;
            hpText = hpRawText();
            lenovoText = lenovoRawText();
        } else {
            title = "Procurement Scenario — HP Business Winner";
            hpText = hpRawText();
            dellText = dellRawText();
            lenovoText = lenovoRawText();
        }

        WorkflowExecution workflow = quoteService.createWorkflow(
                title, "Auto-generated for demo scenario: " + vendorChoice, demoUserId);

        Quote quoteA = quoteService.ingestQuote(workflow, "Dell Direct Enterprise", "sales@dell-direct.demo",
                dellText, "dell_quotation.pdf", Quote.SourceType.PDF, demoUserId);
        Quote quoteB = quoteService.ingestQuote(workflow, "HP Business Solutions", "quotes@hp-business.demo",
                hpText, "hp_quotation.pdf", Quote.SourceType.PDF, demoUserId);
        Quote quoteC = quoteService.ingestQuote(workflow, "Lenovo Corporate Sales", "corporate@lenovo-sales.demo",
                lenovoText, "lenovo_quotation.pdf", Quote.SourceType.PDF, demoUserId);

        quoteService.updateStatus(workflow, WorkflowExecution.Status.COMPARED);
        ComparisonService.ComparisonResult comparison = comparisonService.compare(workflow.getId(), null);

        quoteService.updateStatus(workflow, WorkflowExecution.Status.NEGOTIATING);
        Negotiation negotiation = negotiationService.draftNegotiation(comparison.recommended().getId(), demoUserId);

        negotiation = negotiationService.decideApproval(negotiation.getId(), true, null, demoUserId, "Auto-approved for scenario " + vendorChoice);

        quoteService.updateStatus(workflow, WorkflowExecution.Status.AWAITING_VENDOR_RESPONSE);
        BigDecimal counter = negotiation.getCurrentPrice()
                .subtract(negotiation.getCurrentPrice().subtract(negotiation.getTargetPrice()).multiply(new BigDecimal("0.7")))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        quoteService.updateStatus(workflow, WorkflowExecution.Status.RE_EVALUATING);
        negotiation = negotiationService.submitVendorResponse(negotiation.getId(), counter, demoUserId);

        quoteService.updateStatus(workflow, WorkflowExecution.Status.VENDOR_SELECTED);
        PurchaseOrder po = purchaseOrderService.generate(comparison.recommended(), workflow, demoUserId,
                negotiation.getStatus() == Negotiation.Status.ACCEPTED ? negotiation.getId() : null);

        quoteService.updateStatus(workflow, WorkflowExecution.Status.COMPLETED);
        auditService.log(workflow.getId(), demoUserId, "DEMO_WORKFLOW_COMPLETED", "WorkflowExecution", workflow.getId(),
                "Recommended vendor=" + comparison.recommended().getVendor().getName() + " PO=" + po.getPoNumber());

        Map<String, Object> map = new java.util.HashMap<>();
        map.put("workflowId", workflow.getId());
        map.put("quotes", List.of(quoteA.getId(), quoteB.getId(), quoteC.getId()));
        map.put("recommendedVendor", comparison.recommended().getVendor().getName());
        map.put("aiExplanation", comparison.aiExplanation());
        map.put("negotiationId", negotiation.getId());
        map.put("negotiationStatus", negotiation.getStatus());
        map.put("finalAgreedPrice", negotiation.getFinalAgreedPrice() != null ? negotiation.getFinalAgreedPrice() : negotiation.getCurrentPrice());
        map.put("purchaseOrderId", po.getId());
        map.put("poNumber", po.getPoNumber());
        map.put("poTotal", po.getTotalAmount());
        return map;
    }

    private String dellRawText() {
        return """
                Vendor: Dell Direct Enterprise
                Product: Dell Latitude 5450
                Quantity: 50
                Unit Price: 68000
                GST: 18
                Shipping: 15000
                Warranty: 3 years
                Delivery: 12 days
                Payment Terms: Net 30
                """;
    }

    private String hpRawText() {
        return """
                Vendor: HP Business Solutions
                Product: HP EliteBook
                Quantity: 50
                Unit Price: 63500
                GST: 18
                Shipping: 0
                Warranty: 3 years
                Delivery: 18 days
                Payment Terms: Net 45
                """;
    }

    private String lenovoRawText() {
        return """
                Vendor: Lenovo Corporate Sales
                Product: Lenovo ThinkPad
                Quantity: 50
                Unit Price: 71000
                GST: 18
                Shipping: 10000
                Warranty: 4 years
                Delivery: 10 days
                Payment Terms: Net 30
                """;
    }
}
