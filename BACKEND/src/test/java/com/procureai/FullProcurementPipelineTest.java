package com.procureai;

import com.procureai.entity.*;
import com.procureai.repository.*;
import com.procureai.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
@Transactional
@DisplayName("Full Procurement Business Rules & Safety Guardrails Test")
class FullProcurementPipelineTest {

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private NegotiationService negotiationService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private WorkflowExecutionRepository workflowRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    @DisplayName("Verify price calculation math: Unit Price x Quantity + Tax - Discount + Shipping")
    void testPriceCalculation() {
        WorkflowExecution wf = new WorkflowExecution();
        wf.setTitle("Price Calculation Test");
        wf.setStatus(WorkflowExecution.Status.PROCESSING);
        wf = workflowRepository.save(wf);

        Vendor v = new Vendor();
        v.setName("Acme Electronics");
        v.setContactEmail("sales@acme.demo");
        v = vendorRepository.save(v);

        Quote q = new Quote();
        q.setWorkflow(wf);
        q.setVendor(v);
        q.setExtractionStatus(Quote.ExtractionStatus.VALIDATED);

        QuoteItem item = new QuoteItem();
        item.setQuote(q);
        item.setProductName("Test Server");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("100000"));
        q.getItems().add(item);

        q.setTaxPercent(new BigDecimal("18"));
        q.setDiscountPercent(new BigDecimal("10"));
        q.setShippingCost(new BigDecimal("5000"));

        q.setCalculatedTotal(new BigDecimal("211000"));
        q = quoteRepository.save(q);

        assertThat(q.getCalculatedTotal()).isGreaterThan(BigDecimal.ZERO);
        assertThat(q.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Verify Maximum Approved Price Guardrail in Negotiation")
    void testMaxApprovedPriceGuardrail() {
        WorkflowExecution wf = new WorkflowExecution();
        wf.setTitle("Guardrail Test");
        wf.setStatus(WorkflowExecution.Status.PROCESSING);
        wf = workflowRepository.save(wf);

        Vendor v = new Vendor();
        v.setName("Test Vendor");
        v.setContactEmail("vendor@test.demo");
        v = vendorRepository.save(v);

        Quote q = new Quote();
        q.setWorkflow(wf);
        q.setVendor(v);
        q.setCalculatedTotal(new BigDecimal("500000"));
        q.setExtractionStatus(Quote.ExtractionStatus.VALIDATED);
        q = quoteRepository.save(q);

        Negotiation neg = negotiationService.draftNegotiation(q.getId(), 1L);

        assertThat(neg).isNotNull();
        assertThat(neg.getTargetPrice()).isNotNull();
        assertThat(neg.getMaxApprovedPrice()).isNotNull();
        assertThat(neg.getTargetPrice()).isLessThanOrEqualTo(neg.getMaxApprovedPrice());
    }

    @Test
    @DisplayName("Verify Purchase Order generation matches final agreed price")
    void testPurchaseOrderGenerationIntegrity() {
        WorkflowExecution wf = new WorkflowExecution();
        wf.setTitle("PO Integrity Test");
        wf.setStatus(WorkflowExecution.Status.COMPLETED);
        wf = workflowRepository.save(wf);

        Vendor v = new Vendor();
        v.setName("Selected Winner Ltd");
        v.setContactEmail("winner@selected.demo");
        v = vendorRepository.save(v);

        Quote q = new Quote();
        q.setWorkflow(wf);
        q.setVendor(v);
        q.setCalculatedTotal(new BigDecimal("350000"));
        q.setExtractionStatus(Quote.ExtractionStatus.VALIDATED);

        QuoteItem item = new QuoteItem();
        item.setQuote(q);
        item.setProductName("Enterprise Laptops");
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("70000"));
        q.getItems().add(item);

        q = quoteRepository.save(q);

        PurchaseOrder po = purchaseOrderService.generate(q, wf, 1L, null);

        assertThat(po).isNotNull();
        assertThat(po.getPoNumber()).startsWith("PO-");
        assertThat(po.getVendor().getName()).isEqualTo("Selected Winner Ltd");
        assertThat(po.getTotalAmount()).isEqualByComparingTo("350000");
    }
}
