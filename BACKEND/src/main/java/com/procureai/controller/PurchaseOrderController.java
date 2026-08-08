package com.procureai.controller;

import com.procureai.entity.PurchaseOrder;
import com.procureai.entity.Quote;
import com.procureai.entity.WorkflowExecution;
import com.procureai.exception.BusinessRuleException;
import com.procureai.repository.WorkflowExecutionRepository;
import com.procureai.service.PurchaseOrderService;
import com.procureai.service.QuoteService;
import com.procureai.util.CurrentUser;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Hardened Purchase Order REST controller.
 */
@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final QuoteService quoteService;
    private final WorkflowExecutionRepository workflowRepository;

    @org.springframework.beans.factory.annotation.Value("${app.po.output-dir:./po-output}")
    private String outputDir;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService,
                                    QuoteService quoteService,
                                    WorkflowExecutionRepository workflowRepository) {
        this.purchaseOrderService = purchaseOrderService;
        this.quoteService = quoteService;
        this.workflowRepository = workflowRepository;
    }

    public record GenerateRequest(
            Long workflowId,
            Long quoteId,
            Long negotiationId
    ) {}

    @PostMapping({"/generate", "/workflows/{workflowId}/generate"})
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'PROCUREMENT_USER')")
    public ResponseEntity<PurchaseOrder> generate(
            @PathVariable(required = false) Long workflowId,
            @RequestBody(required = false) GenerateRequest req) {

        Long targetWfId = (workflowId != null && workflowId > 0) ? workflowId
                : (req != null && req.workflowId() != null && req.workflowId() > 0 ? req.workflowId() : null);

        if (targetWfId == null) {
            targetWfId = workflowRepository.findTopByOrderByCreatedAtDesc()
                    .map(WorkflowExecution::getId)
                    .orElse(null);
        }

        WorkflowExecution wf = targetWfId != null ? quoteService.getWorkflow(targetWfId) : null;
        Long quoteId = (req != null && req.quoteId() != null && req.quoteId() > 0) ? req.quoteId() : null;
        Quote quote = null;

        if (quoteId != null) {
            quote = quoteService.getQuote(quoteId);
        } else if (targetWfId != null) {
            List<Quote> quotes = quoteService.getQuotesForWorkflow(targetWfId);
            if (!quotes.isEmpty()) {
                quote = quotes.get(0);
            }
        }

        if (quote == null) {
            List<Quote> allQuotes = quoteService.getAllQuotes();
            if (!allQuotes.isEmpty()) {
                quote = allQuotes.get(0);
                wf = quote.getWorkflow();
            } else {
                throw new IllegalArgumentException("No quotes found in system. Please run demo or upload quotes first.");
            }
        }

        if (wf == null) {
            wf = quote.getWorkflow();
        }

        Long negId = (req != null && req.negotiationId() != null && req.negotiationId() > 0) ? req.negotiationId() : null;
        PurchaseOrder po = purchaseOrderService.generate(quote, wf, CurrentUser.id(), negId);
        return ResponseEntity.status(HttpStatus.CREATED).body(po);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'PROCUREMENT_USER')")
    public ResponseEntity<List<PurchaseOrder>> all() {
        return ResponseEntity.ok(purchaseOrderService.all());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> get(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.get(id));
    }

    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'PROCUREMENT_USER')")
    public ResponseEntity<PurchaseOrder> sendPoEmail(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.sendPoEmail(id, CurrentUser.id()));
    }

    /**
     * Serves the PO PDF file with auto-regeneration if missing on disk.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<FileSystemResource> pdf(@PathVariable Long id) {
        PurchaseOrder po = purchaseOrderService.get(id);

        Path requestedPath = null;
        if (po.getPdfFilePath() != null && !po.getPdfFilePath().isBlank()) {
            requestedPath = Paths.get(po.getPdfFilePath()).toAbsolutePath().normalize();
        }

        if (requestedPath == null || !requestedPath.toFile().exists()) {
            po = purchaseOrderService.ensurePdfGenerated(po);
            requestedPath = Paths.get(po.getPdfFilePath()).toAbsolutePath().normalize();
        }

        // Path traversal prevention
        Path allowedBase = Paths.get(outputDir).toAbsolutePath().normalize();
        if (!requestedPath.startsWith(allowedBase)) {
            throw new BusinessRuleException("PO file path is outside the allowed directory");
        }

        String safeFilename = po.getPoNumber().replaceAll("[^A-Za-z0-9_\\-]", "_") + ".pdf";

        FileSystemResource resource = new FileSystemResource(requestedPath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + safeFilename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
