package com.procureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "negotiation_rounds")
public class NegotiationRound extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negotiation_id")
    private Negotiation negotiation;

    @Column(nullable = false)
    private Integer roundNumber;

    private BigDecimal offeredPriceByAi;
    private BigDecimal vendorCounterPrice;

    @Enumerated(EnumType.STRING)
    private RoundOutcome outcome;

    @Column(length = 4000)
    private String aiEvaluationNotes;

    private Boolean withinMaxApproved;

    public enum RoundOutcome { PENDING, VENDOR_ACCEPTED, VENDOR_COUNTERED, SYSTEM_ACCEPTED, SYSTEM_REJECTED, ROUND_LIMIT_REACHED }
}
