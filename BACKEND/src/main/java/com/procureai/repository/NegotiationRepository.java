package com.procureai.repository;

import com.procureai.entity.Negotiation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NegotiationRepository extends JpaRepository<Negotiation, Long> {
    @EntityGraph(attributePaths = {"rounds", "quote", "quote.vendor"})
    List<Negotiation> findByWorkflowId(Long workflowId);

    @EntityGraph(attributePaths = {"rounds", "quote", "quote.vendor"})
    List<Negotiation> findAll();
}
