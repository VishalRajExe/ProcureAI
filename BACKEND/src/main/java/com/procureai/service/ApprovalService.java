package com.procureai.service;

import com.procureai.entity.Approval;
import com.procureai.exception.BusinessRuleException;
import com.procureai.exception.NotFoundException;
import com.procureai.repository.ApprovalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Backend-enforced human-in-the-loop gate for financial/procurement actions.
 * Frontend controls are never trusted as the source of authorization — every
 * approval decision is recorded and checked here.
 */
@Service
public class ApprovalService {

    private final ApprovalRepository approvalRepository;

    public ApprovalService(ApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    public Approval requestApproval(Approval.ApprovalType type, Long referenceId, Long requestedByUserId) {
        Approval approval = new Approval();
        approval.setType(type);
        approval.setReferenceId(referenceId);
        approval.setStatus(Approval.Status.PENDING);
        approval.setRequestedByUserId(requestedByUserId);
        return approvalRepository.save(approval);
    }

    public Approval decide(Approval.ApprovalType type, Long referenceId, boolean approve, Long approverUserId, String notes) {
        List<Approval> matches = approvalRepository.findByTypeAndReferenceId(type, referenceId);
        Approval approval = matches.stream()
                .filter(a -> a.getStatus() == Approval.Status.PENDING)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No pending approval found for " + type + " #" + referenceId));

        approval.setStatus(approve ? Approval.Status.APPROVED : Approval.Status.REJECTED);
        approval.setDecidedByUserId(approverUserId);
        approval.setDecisionNotes(notes);
        return approvalRepository.save(approval);
    }

    public void assertApproved(Approval.ApprovalType type, Long referenceId) {
        List<Approval> matches = approvalRepository.findByTypeAndReferenceId(type, referenceId);
        boolean approved = matches.stream().anyMatch(a -> a.getStatus() == Approval.Status.APPROVED);
        if (!approved) {
            throw new BusinessRuleException(type + " #" + referenceId + " has not been approved by a human reviewer");
        }
    }

    public List<Approval> pending() {
        return approvalRepository.findByStatus(Approval.Status.PENDING);
    }

    public List<Approval> all() {
        return approvalRepository.findAll();
    }
}
