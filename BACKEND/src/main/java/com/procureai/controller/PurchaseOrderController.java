package com.procureai.controller;

import com.procureai.entity.PurchaseOrder;
import com.procureai.entity.Quote;
import com.procureai.entity.WorkflowExecution;
import com.procureai.repository.WorkflowExecutionRepository;
import com.procureai.service.PurchaseOrderService;
import com.procureai.service.QuoteService;
import com.procureai.util.CurrentUser;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final QuoteService quoteService;
    private final WorkflowExecutionRepository workflowRepository;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService, QuoteService quoteService,
                                   WorkflowExecutionRepository workflowRepository) {
        this.purchaseOrderService = purchaseOrderService;
        this.quoteService = quoteService;
        this.workflowRepository = workflowRepository;
    }

    public record GenerateRequest(Long quoteId, Long negotiationId, Long workflowId) {}

    @PostMapping({"/generate", "/workflows/{workflowId}/generate"})
    public ResponseEntity<PurchaseOrder> generate(@PathVariable(required = false) Long workflowId,
                                                   @RequestBody(required = false) GenerateRequest req) {
        Long targetWfId = workflowId != null ? workflowId : (req != null ? req.workflowId() : null);
        if (targetWfId == null) {
            targetWfId = workflowRepository.findTopByOrderByCreatedAtDesc()
                    .map(WorkflowExecution::getId)
                    .orElseThrow(() -> new IllegalArgumentException("No workflow found to generate purchase order. Run demo seed or upload quotes first."));
        }
        WorkflowExecution wf = quoteService.getWorkflow(targetWfId);
        Long quoteId = (req != null && req.quoteId() != null) ? req.quoteId() : null;
        Quote quote;
        if (quoteId != null) {
            quote = quoteService.getQuote(quoteId);
        } else {
            List<Quote> quotes = quoteService.getQuotesForWorkflow(targetWfId);
            if (quotes.isEmpty()) {
                throw new IllegalArgumentException("No quotes found in workflow " + targetWfId);
            }
            quote = quotes.get(0);
        }
        Long negId = req != null ? req.negotiationId() : null;
        PurchaseOrder po = purchaseOrderService.generate(quote, wf, CurrentUser.id(), negId);
        return ResponseEntity.ok(po);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> all() {
        return ResponseEntity.ok(purchaseOrderService.all());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> get(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.get(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<FileSystemResource> pdf(@PathVariable Long id) {
        PurchaseOrder po = purchaseOrderService.get(id);
        FileSystemResource resource = new FileSystemResource(po.getPdfFilePath());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + po.getPoNumber() + ".pdf\"")
                .body(resource);
    }
}
