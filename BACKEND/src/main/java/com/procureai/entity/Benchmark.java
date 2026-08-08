package com.procureai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Curated reference/demo pricing. NOT live market data — clearly labeled as such
 * everywhere it is surfaced to the client. Architected so a real pricing API/service
 * can populate this table in the future without changing consumers.
 */
@Getter
@Setter
@Entity
@Table(name = "benchmarks")
public class Benchmark extends BaseEntity {

    @Column(nullable = false)
    private String productCategory; // e.g. "Business Laptop"

    @Column(nullable = false)
    private BigDecimal referenceMinUnitPrice;

    @Column(nullable = false)
    private BigDecimal referenceMaxUnitPrice;

    private String source = "Reference/Demo Benchmark Data";
}
