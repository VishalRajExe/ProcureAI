package com.procureai.service;

import com.procureai.entity.*;
import com.procureai.exception.ExtractionException;
import com.procureai.exception.NotFoundException;
import com.procureai.repository.NegotiationRepository;
import com.procureai.repository.QuoteRepository;
import com.procureai.repository.VendorRepository;
import com.procureai.repository.WorkflowExecutionRepository;
import com.procureai.service.ai.AIProvider;
import com.procureai.service.ai.ExtractedQuoteData;
import com.procureai.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    private final QuoteRepository quoteRepository;
    private final VendorRepository vendorRepository;
    private final WorkflowExecutionRepository workflowRepository;
    private final NegotiationRepository negotiationRepository;
    private final AIProvider aiProvider;
    private final QuoteCalculationService calculationService;
    private final BenchmarkService benchmarkService;
    private final AuditService auditService;
    private final ComparisonService comparisonService;
    private final NegotiationService negotiationService;

    public QuoteService(QuoteRepository quoteRepository, VendorRepository vendorRepository,
                         WorkflowExecutionRepository workflowRepository, NegotiationRepository negotiationRepository,
                         AIProvider aiProvider, QuoteCalculationService calculationService,
                         BenchmarkService benchmarkService, AuditService auditService,
                         @Lazy ComparisonService comparisonService, @Lazy NegotiationService negotiationService) {
        this.quoteRepository = quoteRepository;
        this.vendorRepository = vendorRepository;
        this.workflowRepository = workflowRepository;
        this.negotiationRepository = negotiationRepository;
        this.aiProvider = aiProvider;
        this.calculationService = calculationService;
        this.benchmarkService = benchmarkService;
        this.auditService = auditService;
        this.comparisonService = comparisonService;
        this.negotiationService = negotiationService;
    }

    @Transactional
    public WorkflowExecution createWorkflow(String title, String description, Long userId) {
        WorkflowExecution wf = new WorkflowExecution();
        wf.setTitle(title);
        wf.setDescription(description);
        wf.setCreatedByUserId(userId);
        wf.setStatus(WorkflowExecution.Status.PROCESSING);
        wf = workflowRepository.save(wf);
        auditService.log(wf.getId(), userId, "WORKFLOW_CREATED", "WorkflowExecution", wf.getId(), title);
        return wf;
    }

    /**
     * Ingests one vendor quote: runs AI extraction on raw text, validates the result,
     * normalizes it, computes the authoritative total, applies benchmarking, and
     * automatically orchestrates downstream comparison and negotiation drafting.
     */
    @Transactional
    public Quote ingestQuote(WorkflowExecution workflow, String vendorName, String vendorEmail,
                              String rawText, String sourceFileName, Quote.SourceType sourceType, Long userId) {
        Vendor vendor = vendorRepository.findByNameIgnoreCase(vendorName)
                .orElseGet(() -> {
                    Vendor v = new Vendor();
                    v.setName(vendorName);
                    v.setContactEmail(vendorEmail);
                    return vendorRepository.save(v);
                });

        Quote quote = new Quote();
        quote.setWorkflow(workflow);
        quote.setVendor(vendor);
        quote.setSourceFileName(sourceFileName);
        quote.setSourceType(sourceType);
        quote.setExtractionStatus(Quote.ExtractionStatus.PROCESSING);
        quote = quoteRepository.save(quote);

        try {
            String safeText = InputSanitizer.sanitizeForAi(rawText);
            String safeVendorName = InputSanitizer.sanitizeField(vendorName);
            ExtractedQuoteData extracted = aiProvider.extractQuoteData(safeText, safeVendorName);
            validateExtraction(extracted);
            applyExtraction(quote, extracted);

            quote.setExtractionStatus(Quote.ExtractionStatus.VALIDATED);
            calculationService.recalculate(quote);

            BigDecimal avgUnitPrice = quote.getItems().isEmpty() ? BigDecimal.ZERO : quote.getItems().get(0).getUnitPrice();
            benchmarkService.applyBenchmark(quote, avgUnitPrice);

            quote = quoteRepository.save(quote);
            auditService.log(workflow.getId(), userId, "QUOTE_EXTRACTED", "Quote", quote.getId(),
                    "Vendor=" + vendorName + " confidence=" + extracted.confidence() + " missing=" + extracted.missingFields());

            // Auto-orchestrate downstream workflow (Comparison, AI Negotiation, Approvals)
            autoOrchestrateWorkflow(workflow, userId);

            return quote;
        } catch (Exception ex) {
            quote.setExtractionStatus(Quote.ExtractionStatus.FAILED);
            quote.setExtractionError(ex.getMessage());
            quoteRepository.save(quote);
            auditService.logFailure(workflow.getId(), userId, "QUOTE_EXTRACTION_FAILED", "Quote", quote.getId(), ex.getMessage());
            throw new ExtractionException("Failed to extract quote for vendor " + vendorName + ": " + ex.getMessage(), ex);
        }
    }

    private void autoOrchestrateWorkflow(WorkflowExecution workflow, Long userId) {
        try {
            List<Quote> quotes = quoteRepository.findByWorkflowId(workflow.getId());
            if (quotes.isEmpty()) return;

            workflow.setStatus(WorkflowExecution.Status.COMPARED);
            workflowRepository.save(workflow);

            ComparisonService.ComparisonResult comparison = comparisonService.compare(workflow.getId(), null);
            if (comparison != null && comparison.recommended() != null) {
                Quote topQuote = comparison.recommended();
                List<Negotiation> existing = negotiationRepository.findByWorkflowId(workflow.getId());
                if (existing.isEmpty()) {
                    workflow.setStatus(WorkflowExecution.Status.NEGOTIATING);
                    workflowRepository.save(workflow);
                    negotiationService.draftNegotiation(topQuote.getId(), userId);
                }
            }
        } catch (Exception ex) {
            log.warn("Auto-orchestration downstream flow skipped: {}", ex.getMessage());
        }
    }

    private void validateExtraction(ExtractedQuoteData data) {
        if (data.items() == null || data.items().isEmpty()) {
            throw new ExtractionException("AI extraction returned no line items");
        }
        for (ExtractedQuoteData.Item item : data.items()) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new ExtractionException("Invalid or missing quantity for item: " + item.productName());
            }
            if (item.unitPrice() == null || item.unitPrice().signum() < 0) {
                throw new ExtractionException("Invalid or missing unit price for item: " + item.productName());
            }
        }
        if (data.discountPercent() != null && (data.discountPercent().signum() < 0 || data.discountPercent().doubleValue() > 100)) {
            throw new ExtractionException("Discount percent out of valid range (0-100)");
        }
        if (data.taxPercent() != null && (data.taxPercent().signum() < 0 || data.taxPercent().doubleValue() > 100)) {
            throw new ExtractionException("Tax percent out of valid range (0-100)");
        }
    }

    private void applyExtraction(Quote quote, ExtractedQuoteData data) {
        quote.setDiscountPercent(data.discountPercent() == null ? BigDecimal.ZERO : data.discountPercent());
        quote.setTaxPercent(data.taxPercent() == null ? BigDecimal.ZERO : data.taxPercent());
        quote.setShippingCost(data.shippingCost() == null ? BigDecimal.ZERO : data.shippingCost());
        quote.setVendorDeclaredTotal(data.vendorDeclaredTotal());
        quote.setWarrantyMonths(data.warrantyMonths());
        quote.setDeliveryDays(data.deliveryDays());
        quote.setPaymentTerms(data.paymentTerms());
        quote.setExtractionConfidence(data.confidence());
        if (data.validUntil() != null) {
            try {
                quote.setValidUntil(LocalDate.parse(data.validUntil()));
            } catch (Exception ignored) {}
        }

        quote.getItems().clear();
        for (ExtractedQuoteData.Item item : data.items()) {
            QuoteItem qi = new QuoteItem();
            qi.setQuote(quote);
            qi.setProductName(item.productName());
            qi.setModel(item.model());
            qi.setQuantity(item.quantity());
            qi.setUnitPrice(item.unitPrice());
            quote.getItems().add(qi);
        }
    }

    public List<Quote> getQuotesForWorkflow(Long workflowId) {
        return quoteRepository.findByWorkflowId(workflowId);
    }

    public List<Quote> getAllQuotes() {
        return quoteRepository.findAll();
    }

    public Quote getQuote(Long id) {
        return quoteRepository.findById(id).orElseThrow(() -> new NotFoundException("Quote not found: " + id));
    }

    public WorkflowExecution getWorkflow(Long id) {
        return workflowRepository.findById(id).orElseThrow(() -> new NotFoundException("Workflow not found: " + id));
    }

    public WorkflowExecution updateStatus(WorkflowExecution wf, WorkflowExecution.Status status) {
        wf.setStatus(status);
        return workflowRepository.save(wf);
    }
}
