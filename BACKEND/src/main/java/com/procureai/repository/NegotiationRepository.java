package com.procureai.repository;

import com.procureai.entity.Negotiation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NegotiationRepository extends JpaRepository<Negotiation, Long> {
    @EntityGraph(attributePaths = {"rounds", "quote", "quote.vendor"})
    List<Negotiation> findByWorkflowId(Long workflowId);

    @EntityGraph(attributePaths = {"rounds", "quote", "quote.vendor"})
    List<Negotiation> findByQuoteWorkflowId(Long workflowId);

    @EntityGraph(attributePaths = {"rounds", "quote", "quote.vendor", "workflow"})
    List<Negotiation> findByWorkflowCreatedByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"rounds", "quote", "quote.vendor", "workflow"})
    Optional<Negotiation> findByIdAndWorkflowCreatedByUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"rounds", "quote", "quote.vendor"})
    List<Negotiation> findAll();
}
