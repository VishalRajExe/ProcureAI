package com.procureai.service;

import com.procureai.entity.AuditLog;
import com.procureai.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long workflowId, Long userId, String event, String referenceType, Long referenceId, String details) {
        AuditLog log = new AuditLog();
        log.setWorkflowId(workflowId);
        log.setUserId(userId);
        log.setEvent(event);
        log.setReferenceType(referenceType);
        log.setReferenceId(referenceId);
        log.setDetails(details);
        log.setStatus(AuditLog.Status.SUCCESS);
        auditLogRepository.save(log);
    }

    public void logFailure(Long workflowId, Long userId, String event, String referenceType, Long referenceId, String details) {
        AuditLog log = new AuditLog();
        log.setWorkflowId(workflowId);
        log.setUserId(userId);
        log.setEvent(event);
        log.setReferenceType(referenceType);
        log.setReferenceId(referenceId);
        log.setDetails(details);
        log.setStatus(AuditLog.Status.FAILURE);
        auditLogRepository.save(log);
    }
}
