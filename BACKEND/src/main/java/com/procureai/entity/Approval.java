package com.procureai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approvals")
public class Approval extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalType type; // NEGOTIATION, PURCHASE_ORDER

    @Column(nullable = false)
    private Long referenceId; // id of Negotiation or PurchaseOrder

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private Long requestedByUserId;
    private Long decidedByUserId;

    @Column(length = 2000)
    private String decisionNotes;

    public enum ApprovalType { NEGOTIATION, PURCHASE_ORDER }
    public enum Status { PENDING, APPROVED, REJECTED }
}
