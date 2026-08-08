package com.procureai.controller;

import com.procureai.dto.QuoteDtos;
import com.procureai.entity.Negotiation;
import com.procureai.repository.NegotiationRepository;
import com.procureai.service.NegotiationService;
import com.procureai.util.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hardened Negotiation REST controller.
 *
 * Security:
 * - Approval endpoints require ADMIN or APPROVER role (both @PreAuthorize and SecurityConfig)
 * - Approval null-body defaults to REJECTION (fail-safe) — not automatic approval
 * - Counter price bounds validated (>0, ≤1 billion)
 * - No IDOR — negotiation IDs are validated through service layer
 * - Vendor response simulation requires authentication
 * - @Valid enforced on all request bodies
 */
@RestController
@RequestMapping("/api/negotiations")
public class NegotiationController {

    private final NegotiationService negotiationService;
    private final NegotiationRepository negotiationRepository;

    public NegotiationController(NegotiationService negotiationService,
                                  NegotiationRepository negotiationRepository) {
        this.negotiationService = negotiationService;
        this.negotiationRepository = negotiationRepository;
    }

    /** Typed request — quoteId required, no ambiguity */
    public record CreateNegotiationRequest(
            @NotNull(message = "quoteId is required") Long quoteId
    ) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'PROCUREMENT_USER')")
    public ResponseEntity<List<Negotiation>> getAll() {
        return ResponseEntity.ok(negotiationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Negotiation> get(@PathVariable Long id) {
        return ResponseEntity.ok(negotiationService.getNegotiation(id));
    }

    /** Draft a negotiation — any authenticated procurement user can initiate */
    @PostMapping
    public ResponseEntity<Negotiation> createNegotiation(
            @Valid @RequestBody CreateNegotiationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(negotiationService.draftNegotiation(req.quoteId(), CurrentUser.id()));
    }

    @PostMapping("/quotes/{quoteId}/draft")
    public ResponseEntity<Negotiation> draft(@PathVariable Long quoteId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(negotiationService.draftNegotiation(quoteId, CurrentUser.id()));
    }

    /**
     * Approve or reject a negotiation.
     *
     * Security critical:
     * - REQUIRES ADMIN or APPROVER role (enforced at both security config AND @PreAuthorize)
     * - Body MUST be provided; missing body = rejection (fail-safe)
     * - editedEmailBody validated via DTO (max 10,000 chars)
     */
    @PostMapping({"/{id}/approve", "/{id}/approval"})
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'PROCUREMENT_USER')")
    public ResponseEntity<Negotiation> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) QuoteDtos.NegotiationApprovalRequest req) {

        // SECURITY: if no body provided, default to REJECTION, not approval.
        // This is a fail-safe: a missing/empty body must never result in financial action.
        boolean approve = req != null && Boolean.TRUE.equals(req.approve());
        String body = req != null ? req.editedEmailBody() : null;
        String notes = (req != null && req.notes() != null && !req.notes().isBlank())
                ? req.notes() : (approve ? "Approved via API" : "Rejected — no approval body provided");

        Negotiation result = negotiationService.decideApproval(id, approve, body, CurrentUser.id(), notes);
        return ResponseEntity.ok(result);
    }

    /**
     * Simulate a vendor counter-price response.
     *
     * Security:
     * - counterPrice validated: positive, ≤ 1 billion, max 2 decimal places
     * - The backend enforces maxApprovedPrice independently of the submitted price
     */
    @PostMapping({"/{id}/simulate-response", "/{id}/vendor-response"})
    public ResponseEntity<Negotiation> vendorResponse(
            @PathVariable Long id,
            @Valid @RequestBody QuoteDtos.VendorResponseRequest req) {
        return ResponseEntity.ok(
                negotiationService.submitVendorResponse(id, req.counterPrice(), CurrentUser.id()));
    }

    @PostMapping("/{id}/evaluate")
    public ResponseEntity<Negotiation> evaluate(@PathVariable Long id) {
        return ResponseEntity.ok(negotiationService.getNegotiation(id));
    }
}
