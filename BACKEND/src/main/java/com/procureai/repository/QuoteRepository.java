package com.procureai.repository;

import com.procureai.entity.Quote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    @EntityGraph(attributePaths = {"items", "vendor"})
    List<Quote> findByWorkflowId(Long workflowId);

    @EntityGraph(attributePaths = {"items", "vendor", "workflow"})
    List<Quote> findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "vendor"})
    List<Quote> findByWorkflowIdAndWorkflowCreatedByUserId(Long workflowId, Long userId);

    @EntityGraph(attributePaths = {"items", "vendor", "workflow"})
    Optional<Quote> findByIdAndWorkflowCreatedByUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"items", "vendor", "workflow"})
    List<Quote> findAll();
}
