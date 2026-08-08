package com.procureai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    private Long workflowId;

    private Long userId; // null for system/AI actions

    @Column(nullable = false)
    private String event; // e.g. "QUOTE_UPLOADED", "NEGOTIATION_APPROVED"

    @Enumerated(EnumType.STRING)
    private Status status = Status.SUCCESS;

    @Column(length = 200)
    private String referenceType; // e.g. "Quote", "Negotiation", "PurchaseOrder"

    private Long referenceId;

    @Column(length = 2000)
    private String details;

    public enum Status { SUCCESS, FAILURE, INFO }
}
