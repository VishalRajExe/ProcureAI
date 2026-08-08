package com.procureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "quotes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Quote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_execution_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private WorkflowExecution workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vendor vendor;

    /** Original uploaded filename, for traceability. */
    private String sourceFileName;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType = SourceType.JSON;

    @Enumerated(EnumType.STRING)
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    @Column(length = 4000)
    private String extractionError;

    private Double extractionConfidence;

    private String currency = "INR";

    private BigDecimal discountPercent = BigDecimal.ZERO;
    private BigDecimal taxPercent = BigDecimal.ZERO;
    private BigDecimal shippingCost = BigDecimal.ZERO;

    private BigDecimal vendorDeclaredTotal;
    private BigDecimal calculatedTotal;

    private Integer warrantyMonths;
    private Integer deliveryDays;
    private String paymentTerms;
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    private BenchmarkStatus benchmarkStatus = BenchmarkStatus.UNKNOWN;

    private Double vendorScore;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("quote")
    private List<QuoteItem> items = new ArrayList<>();

    public enum SourceType { PDF, SCANNED_PDF, IMAGE, EMAIL_TEXT, JSON }
    public enum ExtractionStatus { PENDING, PROCESSING, EXTRACTED, VALIDATED, FAILED }
    public enum BenchmarkStatus { BELOW, WITHIN, ABOVE, UNKNOWN }
}
