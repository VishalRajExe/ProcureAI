package com.procureai.repository;

import com.procureai.entity.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {
    Optional<WorkflowExecution> findTopByOrderByCreatedAtDesc();
    Optional<WorkflowExecution> findTopByCreatedByUserIdOrderByCreatedAtDesc(Long createdByUserId);
    List<WorkflowExecution> findByCreatedByUserIdOrderByCreatedAtDesc(Long createdByUserId);
    Optional<WorkflowExecution> findByIdAndCreatedByUserId(Long id, Long createdByUserId);
}
