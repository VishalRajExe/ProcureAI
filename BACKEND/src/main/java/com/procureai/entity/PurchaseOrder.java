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
@Table(name = "purchase_orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PurchaseOrder extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_execution_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private WorkflowExecution workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Quote sourceQuote;

    private BigDecimal totalAmount;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;

    private Integer warrantyMonths;
    private Integer deliveryDays;
    private String paymentTerms;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    private Status status = Status.GENERATED;

    private String pdfFilePath;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("purchaseOrder")
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public enum Status { GENERATED, ISSUED, CANCELLED }
}
