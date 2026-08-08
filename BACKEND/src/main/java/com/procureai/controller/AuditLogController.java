package com.procureai.controller;

import com.procureai.entity.AuditLog;
import com.procureai.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Audit log REST controller.
 *
 * Security:
 * - Only ADMIN and APPROVER can read audit logs.
 * - Audit logs are IMMUTABLE — there are no write/delete endpoints.
 * - Page size capped at 200 to prevent resource exhaustion.
 * - No stack traces or internal details in audit log entries (enforced at write time).
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private static final int MAX_PAGE_SIZE = 200;

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ResponseEntity<?> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        return ResponseEntity.ok(
                auditLogRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(safePage, safeSize)));
    }

    @GetMapping("/workflows/{workflowId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ResponseEntity<?> forWorkflow(
            @PathVariable Long workflowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        return ResponseEntity.ok(
                auditLogRepository.findByWorkflowIdOrderByCreatedAtDesc(
                        workflowId, PageRequest.of(safePage, safeSize)));
    }
}
