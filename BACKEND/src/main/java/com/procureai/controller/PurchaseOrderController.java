package com.procureai.controller;

import com.procureai.entity.PurchaseOrder;
import com.procureai.entity.Quote;
import com.procureai.entity.WorkflowExecution;
import com.procureai.exception.BusinessRuleException;
import com.procureai.repository.WorkflowExecutionRepository;
import com.procureai.service.PurchaseOrderService;
import com.procureai.service.QuoteService;
import com.procureai.util.CurrentUser;
import jakarta.validation.constraints.NotNull;
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
            @NotNull(message = "workflowId is required") Long workflowId,
            Long quoteId,
            Long negotiationId
    ) {}

    @PostMapping({"/generate", "/workflows/{workflowId}/generate"})
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ResponseEntity<PurchaseOrder> generate(
            @PathVariable(required = false) Long workflowId,
            @RequestBody(required = false) GenerateRequest req) {

        Long targetWfId = workflowId != null ? workflowId
                : (req != null ? req.workflowId() : null);

        if (targetWfId == null) {
            targetWfId = workflowRepository.findTopByOrderByCreatedAtDesc()
                    .map(WorkflowExecution::getId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No workflow found. Run demo or upload quotes first."));
        }

        WorkflowExecution wf = quoteService.getWorkflow(targetWfId);
        Long quoteId = (req != null && req.quoteId() != null) ? req.quoteId() : null;
        Quote quote;
        if (quoteId != null) {
            quote = quoteService.getQuote(quoteId);
            if (!quote.getWorkflow().getId().equals(targetWfId)) {
                throw new BusinessRuleException(
                        "Quote does not belong to the specified workflow");
            }
        } else {
            List<Quote> quotes = quoteService.getQuotesForWorkflow(targetWfId);
            if (quotes.isEmpty()) {
                throw new IllegalArgumentException("No quotes found in workflow " + targetWfId);
            }
            quote = quotes.get(0);
        }

        Long negId = req != null ? req.negotiationId() : null;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
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
