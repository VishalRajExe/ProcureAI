package com.procureai.repository;

import com.procureai.entity.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {
    java.util.Optional<WorkflowExecution> findTopByOrderByCreatedAtDesc();
}
