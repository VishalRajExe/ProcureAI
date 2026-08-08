package com.procureai.controller;

import com.procureai.entity.EmailMessage;
import com.procureai.repository.EmailMessageRepository;
import com.procureai.service.email.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for viewing email status and retrying failed email deliveries.
 */
@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailMessageRepository emailMessageRepository;
    private final EmailService emailService;

    public EmailController(EmailMessageRepository emailMessageRepository, EmailService emailService) {
        this.emailMessageRepository = emailMessageRepository;
        this.emailService = emailService;
    }

    /** List outbound emails for a negotiation */
    @GetMapping("/negotiation/{negotiationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'PROCUREMENT_USER')")
    public ResponseEntity<List<EmailMessage>> getForNegotiation(@PathVariable Long negotiationId) {
        return ResponseEntity.ok(emailMessageRepository.findByNegotiationIdOrderByCreatedAtAsc(negotiationId));
    }

    /** Retry a failed email message */
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ResponseEntity<EmailMessage> retrySend(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.retrySend(id));
    }
}
