package com.procureai.service;

import com.procureai.entity.*;
import com.procureai.exception.BusinessRuleException;
import com.procureai.exception.NotFoundException;
import com.procureai.repository.*;
import com.procureai.service.ai.AIProvider;
import com.procureai.service.ai.NegotiationContext;
import com.procureai.service.ai.NegotiationDecision;
import com.procureai.service.ai.PythonAIService;
import com.procureai.service.ai.RoundEvaluation;
import com.procureai.service.email.EmailService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.procureai.util.CurrentUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates the AI negotiation agent end-to-end.
 */
@Service
public class NegotiationService {

    private static final Logger log = LoggerFactory.getLogger(NegotiationService.class);

    private static final BigDecimal DEFAULT_TARGET_DISCOUNT = new BigDecimal("0.06");
    private static final BigDecimal DEFAULT_MAX_DISCOUNT_FLOOR = new BigDecimal("0.10");
    private static final int DEFAULT_MIN_WARRANTY_MONTHS = 24;
    private static final int DEFAULT_MAX_DELIVERY_DAYS = 30;
    private static final int DEFAULT_MAX_ROUNDS = 2;

    private final NegotiationRepository negotiationRepository;
    private final NegotiationRoundRepository roundRepository;
    private final QuoteRepository quoteRepository;
    private final WorkflowExecutionRepository workflowRepository;
    private final BenchmarkService benchmarkService;
    private final AIProvider aiProvider;
    private final PythonAIService pythonAIService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final ApprovalService approvalService;
    private final PurchaseOrderService purchaseOrderService;

    public NegotiationService(NegotiationRepository negotiationRepository, NegotiationRoundRepository roundRepository,
                               QuoteRepository quoteRepository, WorkflowExecutionRepository workflowRepository,
                               BenchmarkService benchmarkService, AIProvider aiProvider,
                               PythonAIService pythonAIService,
                               EmailService emailService, AuditService auditService, ApprovalService approvalService,
                               @Lazy PurchaseOrderService purchaseOrderService) {
        this.negotiationRepository = negotiationRepository;
        this.roundRepository = roundRepository;
        this.quoteRepository = quoteRepository;
        this.workflowRepository = workflowRepository;
        this.benchmarkService = benchmarkService;
        this.aiProvider = aiProvider;
        this.pythonAIService = pythonAIService;
        this.emailService = emailService;
        this.auditService = auditService;
        this.approvalService = approvalService;
        this.purchaseOrderService = purchaseOrderService;
    }

    /** AI drafts a negotiation strategy + email for a given quote. */
    @Transactional
    public Negotiation draftNegotiation(Long quoteId, Long userId) {
        Quote quote = quoteRepository.findById(quoteId).orElseThrow(() -> new NotFoundException("Quote not found: " + quoteId));
        if (quote.getExtractionStatus() != Quote.ExtractionStatus.VALIDATED) {
            throw new BusinessRuleException("Quote must be validated before negotiation can start");
        }

        BigDecimal unitPrice = quote.getItems().isEmpty() ? BigDecimal.ZERO : quote.getItems().get(0).getUnitPrice();
        BigDecimal currentPrice = quote.getCalculatedTotal();

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
        BigDecimal safeTarget = decision.targetPrice().min(maxApprovedPrice);

        // ── Enhanced: FastAPI Defensive/Balanced/Aggressive approach framing ──
        // Adapted from negotiation_strategy agent (agent_prompts.jinja)
        // Falls back silently to Gemini-only path if FastAPI is unavailable
        String negotiationApproach = "Balanced"; // default
        try {
            PythonAIService.NegotiationStrategyResult pyStrategy = pythonAIService.negotiationStrategy(
                    ctx,
                    benchmark.map(b -> b.getReferenceMinUnitPrice()).orElse(null),
                    benchmark.map(b -> b.getReferenceMaxUnitPrice()).orElse(null)
            );
            if (pyStrategy != null && pyStrategy.approach() != null) {
                negotiationApproach = pyStrategy.approach();
                log.info("FastAPI negotiation approach: {} (mode={})", negotiationApproach, pyStrategy.aiMode());
            }
        } catch (Exception e) {
            log.debug("FastAPI negotiation strategy enhancement skipped: {}", e.getMessage());
        }

        Negotiation negotiation = new Negotiation();
        negotiation.setQuote(quote);
        negotiation.setWorkflow(quote.getWorkflow());
        negotiation.setCurrentPrice(currentPrice);
        negotiation.setTargetPrice(safeTarget);
        negotiation.setMaxApprovedPrice(maxApprovedPrice);
        negotiation.setAiAction(Negotiation.AiAction.valueOf(decision.action().name()));
        // Prepend approach to strategy so it shows in UI
        negotiation.setAiStrategy("[" + negotiationApproach + "] " + decision.strategy());
        negotiation.setAiReason(decision.reason());
        negotiation.setAiConfidence(decision.confidence());
        negotiation.setMaxRounds(DEFAULT_MAX_ROUNDS);
        negotiation.setCurrentRound(0);
        negotiation.setStatus(Negotiation.Status.DRAFTED);

        // ── Enhanced: FastAPI email generation with approach context ──
        // Falls back to GeminiAIProvider.draftNegotiationEmail if unavailable
        String email = null;
        try {
            email = pythonAIService.generateEnhancedEmail(ctx, decision, negotiationApproach, 1);
            if (email != null) {
                log.info("FastAPI generated enhanced negotiation email (approach={})", negotiationApproach);
            }
        } catch (Exception e) {
            log.debug("FastAPI email generation skipped: {}", e.getMessage());
        }
        if (email == null || email.isBlank()) {
            email = aiProvider.draftNegotiationEmail(ctx, decision);
        }
        negotiation.setDraftEmailBody(email);

        negotiation = negotiationRepository.save(negotiation);
        auditService.log(quote.getWorkflow().getId(), userId, "NEGOTIATION_DRAFTED", "Negotiation", negotiation.getId(),
                "action=" + decision.action() + " target=" + safeTarget + " max=" + maxApprovedPrice);

        approvalService.requestApproval(Approval.ApprovalType.NEGOTIATION, negotiation.getId(), userId);
        negotiation.setStatus(Negotiation.Status.PENDING_APPROVAL);
        return negotiationRepository.save(negotiation);
    }

    /** Human approves or rejects the drafted negotiation. */
    @Transactional
    public Negotiation decideApproval(Long negotiationId, boolean approve, String editedEmailBody, Long approverUserId, String notes) {
        return decideApproval(negotiationId, approve, editedEmailBody, null, approverUserId, notes);
    }

    @Transactional
    public Negotiation decideApproval(Long negotiationId, boolean approve, String editedEmailBody, String recipientEmail, Long approverUserId, String notes) {
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
        if (recipientEmail != null && !recipientEmail.isBlank() && negotiation.getQuote() != null && negotiation.getQuote().getVendor() != null) {
            negotiation.getQuote().getVendor().setContactEmail(recipientEmail.trim());
        }
        negotiation.setStatus(Negotiation.Status.APPROVED);
        negotiation = negotiationRepository.save(negotiation);
        auditService.log(negotiation.getWorkflow().getId(), approverUserId, "NEGOTIATION_APPROVED", "Negotiation", negotiationId, notes);

        return sendNegotiationEmail(negotiation, recipientEmail, approverUserId);
    }

    private Negotiation sendNegotiationEmail(Negotiation negotiation, String customRecipientEmail, Long userId) {
        String vendorEmail = (customRecipientEmail != null && !customRecipientEmail.isBlank())
                ? customRecipientEmail.trim()
                : negotiation.getQuote().getVendor().getContactEmail();
        String to = (vendorEmail != null && !vendorEmail.isBlank()) ? vendorEmail : "vendor@example-demo.com";
        String subject = "Quotation Discussion — " + (negotiation.getQuote().getItems().isEmpty() ? negotiation.getQuote().getVendor().getName() : negotiation.getQuote().getItems().get(0).getProductName());
        EmailMessage emailMsg = emailService.sendEmailDetails(to, subject, negotiation.getDraftEmailBody(), negotiation.getId(), null);

        if (emailMsg.getStatus() == EmailMessage.Status.FAILED) {
            log.error("Failed to dispatch negotiation email for negotiation {}: {}", negotiation.getId(), emailMsg.getErrorMessage());
            negotiation.setStatus(Negotiation.Status.FAILED);
            negotiationRepository.save(negotiation);
            throw new BusinessRuleException("Failed to send email to " + to + ": " + (emailMsg.getErrorMessage() != null ? emailMsg.getErrorMessage() : "Delivery error"));
        }

        negotiation.setStatus(Negotiation.Status.SENT);
        negotiation.setCurrentRound(negotiation.getCurrentRound() + 1);
        negotiation = negotiationRepository.save(negotiation);

        auditService.log(negotiation.getWorkflow().getId(), userId, "NEGOTIATION_EMAIL_SENT", "EmailMessage", emailMsg.getId(),
                "round=" + negotiation.getCurrentRound());
        return negotiation;
    }

    /**
     * Processes vendor counter price and auto-generates/updates the Purchase Order when accepted.
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

            // Update quote calculated total with final agreed price
            quote.setCalculatedTotal(counterPrice);
            quoteRepository.save(quote);

            // Auto-generate / update Purchase Order for this accepted offer
            try {
                WorkflowExecution wf = negotiation.getWorkflow();
                wf.setStatus(WorkflowExecution.Status.VENDOR_SELECTED);
                workflowRepository.save(wf);
                purchaseOrderService.generate(quote, wf, userId, negotiation.getId());
                wf.setStatus(WorkflowExecution.Status.PO_GENERATED);
                workflowRepository.save(wf);
            } catch (Exception ex) {
                // Non-critical if PO already generated
            }

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

    public List<Negotiation> getAllNegotiations() {
        Long userId = CurrentUser.id();
        if (userId != null) {
            return negotiationRepository.findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(userId);
        }
        return negotiationRepository.findAll();
    }

    public Negotiation getNegotiation(Long id) {
        Long userId = CurrentUser.id();
        if (userId != null) {
            return negotiationRepository.findByIdAndWorkflowCreatedByUserId(id, userId)
                    .orElseThrow(() -> new NotFoundException("Negotiation not found or access denied: " + id));
        }
        return negotiationRepository.findById(id).orElseThrow(() -> new NotFoundException("Negotiation not found: " + id));
    }
}
