package com.procureai.repository;

import com.procureai.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByStatus(Approval.Status status);
    List<Approval> findByTypeAndReferenceId(Approval.ApprovalType type, Long referenceId);
}
