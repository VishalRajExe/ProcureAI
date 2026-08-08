package com.procureai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution extends BaseEntity {

    @Column(nullable = false)
    private String title; // e.g. "Procurement of 50 Laptops"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.CREATED;

    @Column(length = 2000)
    private String description;

    private Long createdByUserId;

    public enum Status {
        CREATED, PROCESSING, COMPARED, NEGOTIATING, PENDING_APPROVAL,
        AWAITING_VENDOR_RESPONSE, RE_EVALUATING, VENDOR_SELECTED, PO_GENERATED, COMPLETED, FAILED
    }
}
