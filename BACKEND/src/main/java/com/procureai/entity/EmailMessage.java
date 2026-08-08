package com.procureai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_messages")
public class EmailMessage extends BaseEntity {

    private Long negotiationId;
    private Long purchaseOrderId;

    @Enumerated(EnumType.STRING)
    private Direction direction; // OUTBOUND (to vendor) / INBOUND (simulated vendor reply)

    private String fromAddress;
    private String toAddress;
    private String subject;

    @Column(length = 8000)
    private String body;

    @Enumerated(EnumType.STRING)
    private Status status = Status.SENT;

    @Column(length = 2000)
    private String errorMessage;

    public enum Direction { OUTBOUND, INBOUND }
    public enum Status { DRAFT, SENT, FAILED, RECEIVED }
}
