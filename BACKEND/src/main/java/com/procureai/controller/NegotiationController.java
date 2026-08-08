package com.procureai.controller;

import com.procureai.dto.NegotiationApprovalRequest;
import com.procureai.dto.VendorResponseRequest;
import com.procureai.entity.Negotiation;
import com.procureai.repository.NegotiationRepository;
import com.procureai.service.NegotiationService;
import com.procureai.util.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/negotiations")
public class NegotiationController {

    private final NegotiationService negotiationService;
    private final NegotiationRepository negotiationRepository;

    public NegotiationController(NegotiationService negotiationService, NegotiationRepository negotiationRepository) {
        this.negotiationService = negotiationService;
        this.negotiationRepository = negotiationRepository;
    }

    public record CreateNegotiationRequest(Long quoteId) {}

    @GetMapping
    public ResponseEntity<List<Negotiation>> getAll() {
        return ResponseEntity.ok(negotiationRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Negotiation> createNegotiation(@RequestBody(required = false) CreateNegotiationRequest req,
                                                          @RequestParam(required = false) Long quoteId) {
        Long targetQuoteId = quoteId != null ? quoteId : (req != null ? req.quoteId() : null);
        if (targetQuoteId == null) {
            throw new IllegalArgumentException("quoteId is required to draft a negotiation");
        }
        return ResponseEntity.ok(negotiationService.draftNegotiation(targetQuoteId, CurrentUser.id()));
    }

    @PostMapping("/quotes/{quoteId}/draft")
    public ResponseEntity<Negotiation> draft(@PathVariable Long quoteId) {
        return ResponseEntity.ok(negotiationService.draftNegotiation(quoteId, CurrentUser.id()));
    }

    @PostMapping({"/{id}/approve", "/{id}/approval"})
    public ResponseEntity<Negotiation> approve(@PathVariable Long id, @RequestBody(required = false) NegotiationApprovalRequest req) {
        boolean approve = req == null || req.approve() == null || req.approve();
        String body = req != null ? req.editedEmailBody() : null;
        String notes = req != null ? req.notes() : "Approved via API";
        Negotiation result = negotiationService.decideApproval(id, approve, body, CurrentUser.id(), notes);
        return ResponseEntity.ok(result);
    }

    @PostMapping({"/{id}/simulate-response", "/{id}/vendor-response"})
    public ResponseEntity<Negotiation> vendorResponse(@PathVariable Long id, @RequestBody(required = false) VendorResponseRequest req,
                                                        @RequestParam(required = false) BigDecimal counterPrice) {
        BigDecimal price = counterPrice != null ? counterPrice : (req != null ? req.counterPrice() : new BigDecimal("66000"));
        return ResponseEntity.ok(negotiationService.submitVendorResponse(id, price, CurrentUser.id()));
    }

    @PostMapping("/{id}/evaluate")
    public ResponseEntity<Negotiation> evaluate(@PathVariable Long id) {
        Negotiation neg = negotiationService.getNegotiation(id);
        return ResponseEntity.ok(neg);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Negotiation> get(@PathVariable Long id) {
        return ResponseEntity.ok(negotiationService.getNegotiation(id));
    }
}
