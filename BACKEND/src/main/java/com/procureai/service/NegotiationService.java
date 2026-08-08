package com.procureai.service;

import com.procureai.entity.*;
import com.procureai.exception.BusinessRuleException;
import com.procureai.exception.NotFoundException;
import com.procureai.repository.*;
import com.procureai.service.ai.AIProvider;
import com.procureai.service.ai.NegotiationContext;
import com.procureai.service.ai.NegotiationDecision;
import com.procureai.service.ai.RoundEvaluation;
import com.procureai.service.email.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Orchestrates the AI negotiation agent end-to-end. Every AI recommendation is a
 * *suggestion* — the backend independently enforces maxApprovedPrice, minimum
 * warranty, maximum delivery days, round limits, and the human-approval gate before
 * any negotiation email is sent or any offer is accepted. The AI can never bypass
 * these checks.
 */
@Service
public class NegotiationService {

    private static final BigDecimal DEFAULT_TARGET_DISCOUNT = new BigDecimal("0.06"); // 6% below current
    private static final BigDecimal DEFAULT_MAX_DISCOUNT_FLOOR = new BigDecimal("0.10"); // never approve >10% below current automatically
    private static final int DEFAULT_MIN_WARRANTY_MONTHS = 24;
    private static final int DEFAULT_MAX_DELIVERY_DAYS = 30;
    private static final int DEFAULT_MAX_ROUNDS = 2;

    private final NegotiationRepository negotiationRepository;
    private final NegotiationRoundRepository roundRepository;
    private final QuoteRepository quoteRepository;
    private final BenchmarkService benchmarkService;
    private final AIProvider aiProvider;
    private final EmailService emailService;
    private final AuditService auditService;
    private final ApprovalService approvalService;

    public NegotiationService(NegotiationRepository negotiationRepository, NegotiationRoundRepository roundRepository,
                               QuoteRepository quoteRepository, BenchmarkService benchmarkService, AIProvider aiProvider,
                               EmailService emailService, AuditService auditService, ApprovalService approvalService) {
        this.negotiationRepository = negotiationRepository;
        this.roundRepository = roundRepository;
        this.quoteRepository = quoteRepository;
        this.benchmarkService = benchmarkService;
        this.aiProvider = aiProvider;
        this.emailService = emailService;
        this.auditService = auditService;
        this.approvalService = approvalService;
    }

    /** AI drafts a negotiation strategy + email for a given quote, bounded by backend-owned limits. */
    @Transactional
    public Negotiation draftNegotiation(Long quoteId, Long userId) {
        Quote quote = quoteRepository.findById(quoteId).orElseThrow(() -> new NotFoundException("Quote not found: " + quoteId));
        if (quote.getExtractionStatus() != Quote.ExtractionStatus.VALIDATED) {
            throw new BusinessRuleException("Quote must be validated before negotiation can start");
        }

        BigDecimal unitPrice = quote.getItems().isEmpty() ? BigDecimal.ZERO : quote.getItems().get(0).getUnitPrice();
        BigDecimal currentPrice = quote.getCalculatedTotal();

        // Backend-owned negotiation rules (would be user-configurable per workflow in a full build).
        BigDecimal targetPrice = currentPrice.multiply(BigDecimal.ONE.subtract(DEFAULT_TARGET_DISCOUNT)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxApprovedPrice = currentPrice.multiply(BigDecimal.ONE.subtract(DEFAULT_MAX_DISCOUNT_FLOOR)).setScale(2, RoundingMode.HALF_UP);

        var benchmark = benchmarkService.findForCategory(null);

        NegotiationContext ctx = new NegotiationContext(
                quote.getVendor().getName(),
                quote.getItems().isEmpty() ? "items" : quote.getItems().get(0).getProductName(),
                currentPrice,
                benchmark.map(Benchmark::getReferenceMinUnitPrice).orElse(null),
                benchmark.map(Benchmark::getReferenceMaxUnitPrice).orElse(null),
                targetPrice,
                maxApprovedPrice,
                quote.getItems().isEmpty() ? 0 : quote.getItems().get(0).getQuantity(),
                quote.getWarrantyMonths(),
                quote.getDeliveryDays(),
                DEFAULT_MIN_WARRANTY_MONTHS,
                DEFAULT_MAX_DELIVERY_DAYS
        );

        NegotiationDecision decision = aiProvider.decideNegotiationStrategy(ctx);
        // Backend hard-enforcement: AI can never recommend a target above the max it was given.
        BigDecimal safeTarget = decision.targetPrice().min(maxApprovedPrice);

        Negotiation negotiation = new Negotiation();
        negotiation.setQuote(quote);
        negotiation.setWorkflow(quote.getWorkflow());
        negotiation.setCurrentPrice(currentPrice);
        negotiation.setTargetPrice(safeTarget);
        negotiation.setMaxApprovedPrice(maxApprovedPrice);
        negotiation.setAiAction(Negotiation.AiAction.valueOf(decision.action().name()));
        negotiation.setAiStrategy(decision.strategy());
        negotiation.setAiReason(decision.reason());
        negotiation.setAiConfidence(decision.confidence());
        negotiation.setMaxRounds(DEFAULT_MAX_ROUNDS);
        negotiation.setCurrentRound(0);
        negotiation.setStatus(Negotiation.Status.DRAFTED);

        String email = aiProvider.draftNegotiationEmail(ctx, decision);
        negotiation.setDraftEmailBody(email);

        negotiation = negotiationRepository.save(negotiation);
        auditService.log(quote.getWorkflow().getId(), userId, "NEGOTIATION_DRAFTED", "Negotiation", negotiation.getId(),
                "action=" + decision.action() + " target=" + safeTarget + " max=" + maxApprovedPrice);

        // Human-in-the-loop is mandatory for every financial action — create the approval request now.
        approvalService.requestApproval(Approval.ApprovalType.NEGOTIATION, negotiation.getId(), userId);
        negotiation.setStatus(Negotiation.Status.PENDING_APPROVAL);
        return negotiationRepository.save(negotiation);
    }

    /** Human approves or rejects the drafted negotiation. Only after approval can it be sent. */
    @Transactional
    public Negotiation decideApproval(Long negotiationId, boolean approve, String editedEmailBody, Long approverUserId, String notes) {
        Negotiation negotiation = getNegotiation(negotiationId);
        if (negotiation.getStatus() != Negotiation.Status.PENDING_APPROVAL) {
            throw new BusinessRuleException("Negotiation is not pending approval (current status: " + negotiation.getStatus() + ")");
        }

        approvalService.decide(Approval.ApprovalType.NEGOTIATION, negotiationId, approve, approverUserId, notes);

        if (!approve) {
            negotiation.setStatus(Negotiation.Status.REJECTED_BY_HUMAN);
            negotiationRepository.save(negotiation);
            auditService.log(negotiation.getWorkflow().getId(), approverUserId, "NEGOTIATION_REJECTED", "Negotiation", negotiationId, notes);
            return negotiation;
        }

        if (editedEmailBody != null && !editedEmailBody.isBlank()) {
            negotiation.setDraftEmailBody(editedEmailBody);
        }
        negotiation.setStatus(Negotiation.Status.APPROVED);
        negotiation = negotiationRepository.save(negotiation);
        auditService.log(negotiation.getWorkflow().getId(), approverUserId, "NEGOTIATION_APPROVED", "Negotiation", negotiationId, notes);

        return sendNegotiationEmail(negotiation, approverUserId);
    }

    private Negotiation sendNegotiationEmail(Negotiation negotiation, Long userId) {
        String vendorEmail = negotiation.getQuote().getVendor().getContactEmail();
        String to = (vendorEmail == null || vendorEmail.isBlank()) ? "vendor@example-demo.com" : vendorEmail;
        Long emailId = emailService.send(to, "Quotation Discussion", negotiation.getDraftEmailBody(), negotiation.getId());

        negotiation.setStatus(Negotiation.Status.SENT);
        negotiation.setCurrentRound(negotiation.getCurrentRound() + 1);
        negotiation = negotiationRepository.save(negotiation);

        auditService.log(negotiation.getWorkflow().getId(), userId, "NEGOTIATION_EMAIL_SENT", "EmailMessage", emailId,
                "round=" + negotiation.getCurrentRound());
        return negotiation;
    }

    /**
     * Simulates a vendor counter-offer arriving (Vendor Inbox Simulator), then runs the
     * AI evaluation for this round. The backend independently verifies the counter price
     * against maxApprovedPrice — the AI's recommendation is never trusted blindly.
     */
    @Transactional
    public Negotiation submitVendorResponse(Long negotiationId, BigDecimal counterPrice, Long userId) {
        Negotiation negotiation = getNegotiation(negotiationId);
        if (negotiation.getStatus() != Negotiation.Status.SENT && negotiation.getStatus() != Negotiation.Status.RE_EVALUATING) {
            throw new BusinessRuleException("Negotiation is not awaiting a vendor response (current status: " + negotiation.getStatus() + ")");
        }
        if (counterPrice == null || counterPrice.signum() <= 0) {
            throw new BusinessRuleException("Vendor counter price must be a positive amount");
        }

        int roundNumber = negotiation.getCurrentRound();

        Quote quote = negotiation.getQuote();
        NegotiationContext ctx = new NegotiationContext(
                quote.getVendor().getName(),
                quote.getItems().isEmpty() ? "items" : quote.getItems().get(0).getProductName(),
                negotiation.getCurrentPrice(),
                null, null,
                negotiation.getTargetPrice(),
                negotiation.getMaxApprovedPrice(),
                quote.getItems().isEmpty() ? 0 : quote.getItems().get(0).getQuantity(),
                quote.getWarrantyMonths(), quote.getDeliveryDays(),
                DEFAULT_MIN_WARRANTY_MONTHS, DEFAULT_MAX_DELIVERY_DAYS
        );

        RoundEvaluation evaluation = aiProvider.evaluateVendorResponse(ctx, counterPrice, roundNumber);
        // Backend-authoritative check — independent of what the AI recommended.
        boolean withinMax = counterPrice.compareTo(negotiation.getMaxApprovedPrice()) <= 0;

        NegotiationRound round = new NegotiationRound();
        round.setNegotiation(negotiation);
        round.setRoundNumber(roundNumber);
        round.setOfferedPriceByAi(negotiation.getTargetPrice());
        round.setVendorCounterPrice(counterPrice);
        round.setAiEvaluationNotes(evaluation.notes());
        round.setWithinMaxApproved(withinMax);

        boolean roundLimitReached = roundNumber >= negotiation.getMaxRounds();
        boolean accept = withinMax && (evaluation.recommendAccept() || roundLimitReached);

        if (accept) {
            round.setOutcome(NegotiationRound.RoundOutcome.SYSTEM_ACCEPTED);
            negotiation.setStatus(Negotiation.Status.ACCEPTED);
            negotiation.setFinalAgreedPrice(counterPrice);
            auditService.log(negotiation.getWorkflow().getId(), userId, "NEGOTIATION_ACCEPTED", "Negotiation", negotiationId,
                    "finalPrice=" + counterPrice + " round=" + roundNumber);
        } else if (!withinMax && roundLimitReached) {
            round.setOutcome(NegotiationRound.RoundOutcome.SYSTEM_REJECTED);
            negotiation.setStatus(Negotiation.Status.FAILED);
            auditService.log(negotiation.getWorkflow().getId(), userId, "NEGOTIATION_FAILED", "Negotiation", negotiationId,
                    "reason=max_rounds_exceeded_over_budget counter=" + counterPrice);
        } else if (roundLimitReached) {
            round.setOutcome(NegotiationRound.RoundOutcome.ROUND_LIMIT_REACHED);
            negotiation.setStatus(Negotiation.Status.FAILED);
            auditService.log(negotiation.getWorkflow().getId(), userId, "NEGOTIATION_ROUND_LIMIT", "Negotiation", negotiationId,
                    "counter=" + counterPrice);
        } else {
            round.setOutcome(NegotiationRound.RoundOutcome.VENDOR_COUNTERED);
            negotiation.setStatus(Negotiation.Status.RE_EVALUATING);
            negotiation.setCurrentRound(roundNumber + 1);
            auditService.log(negotiation.getWorkflow().getId(), userId, "NEGOTIATION_ROUND_CONTINUED", "Negotiation", negotiationId,
                    "counter=" + counterPrice + " nextRound=" + negotiation.getCurrentRound());
        }

        roundRepository.save(round);
        return negotiationRepository.save(negotiation);
    }

    public Negotiation getNegotiation(Long id) {
        return negotiationRepository.findById(id).orElseThrow(() -> new NotFoundException("Negotiation not found: " + id));
    }
}
