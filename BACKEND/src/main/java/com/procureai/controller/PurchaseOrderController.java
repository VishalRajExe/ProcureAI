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
 *
 * Security:
 * - PO generation requires ADMIN or APPROVER role
 * - PDF download validates file path stays within allowed output directory (no path traversal)
 * - PO total is always calculated server-side from the Quote entity — never from client input
 * - Negotiation approval is verified before PO generation when a negotiation ID is given
 */
@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final QuoteService quoteService;
    private final WorkflowExecutionRepository workflowRepository;

    /** The configured output directory — used for path traversal prevention */
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

    /** PO generation restricted to ADMIN and APPROVER roles. */
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
            // Security: verify quote belongs to the requested workflow
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
        // PO total is calculated server-side from quote — client never submits a total
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

    /** Issues and dispatches the Purchase Order email to the vendor. Requires ADMIN or APPROVER role. */
    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ResponseEntity<PurchaseOrder> sendPoEmail(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.sendPoEmail(id, CurrentUser.id()));
    }

    /**
     * Serves the PO PDF file.
     *
     * Security: Path traversal prevention — validates the resolved file path
     * is within the configured output directory before serving.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<FileSystemResource> pdf(@PathVariable Long id) {
        PurchaseOrder po = purchaseOrderService.get(id);

        if (po.getPdfFilePath() == null || po.getPdfFilePath().isBlank()) {
            throw new BusinessRuleException("PDF has not been generated for this purchase order yet");
        }

        // Path traversal prevention
        Path requestedPath = Paths.get(po.getPdfFilePath()).toAbsolutePath().normalize();
        Path allowedBase = Paths.get(outputDir).toAbsolutePath().normalize();

        if (!requestedPath.startsWith(allowedBase)) {
            // This should never happen for legitimate POs — reject immediately
            throw new BusinessRuleException("PO file path is outside the allowed directory");
        }

        if (!requestedPath.toFile().exists()) {
            throw new com.procureai.exception.NotFoundException("PO PDF file not found on disk");
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
