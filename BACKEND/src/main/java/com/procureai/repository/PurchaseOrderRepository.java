package com.procureai.repository;

import com.procureai.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @EntityGraph(attributePaths = {"items", "vendor", "workflow"})
    List<PurchaseOrder> findAll();

    @EntityGraph(attributePaths = {"items", "vendor", "workflow"})
    Optional<PurchaseOrder> findById(Long id);

    @EntityGraph(attributePaths = {"items", "vendor", "workflow"})
    List<PurchaseOrder> findByWorkflowId(Long workflowId);

    boolean existsByPoNumber(String poNumber);
}
