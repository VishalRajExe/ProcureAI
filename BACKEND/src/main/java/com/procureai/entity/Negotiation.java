package com.procureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "negotiations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Negotiation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_execution_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private WorkflowExecution workflow;

    private BigDecimal currentPrice;
    private BigDecimal targetPrice;
    private BigDecimal maxApprovedPrice;

    @Enumerated(EnumType.STRING)
    private AiAction aiAction;

    @Column(length = 4000)
    private String aiStrategy;

    @Column(length = 4000)
    private String aiReason;

    private Double aiConfidence;

    @Column(length = 4000)
    private String draftEmailBody;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFTED;

    private Integer maxRounds = 2;
    private Integer currentRound = 0;

    private BigDecimal finalAgreedPrice;

    @OneToMany(mappedBy = "negotiation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("negotiation")
    private List<NegotiationRound> rounds = new ArrayList<>();

    public enum AiAction { NEGOTIATE, ACCEPT, REJECT, REQUEST_CLARIFICATION }

    public enum Status {
        DRAFTED, PENDING_APPROVAL, APPROVED, REJECTED_BY_HUMAN,
        SENT, VENDOR_RESPONDED, RE_EVALUATING, ACCEPTED, FAILED, COMPLETED
    }
}
