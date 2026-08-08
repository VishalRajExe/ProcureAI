package com.procureai.procureai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "benchmarks", indexes = {
    @Index(name = "idx_benchmarks_category", columnList = "category"),
    @Index(name = "idx_benchmarks_sku", columnList = "sku"),
    @Index(name = "idx_benchmarks_is_active", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Benchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "min_price", precision = 15, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "avg_price", precision = 15, scale = 2)
    private BigDecimal avgPrice;

    @Column(name = "max_price", precision = 15, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "source_date")
    private LocalDate sourceDate;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "quantity_tier", length = 50)
    private String quantityTier;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BigDecimal getPriceForQuantity(int quantity) {
        if (quantityTier == null) return avgPrice;
        
        if (quantityTier.contains("1-10") || quantityTier.contains("1-")) {
            return avgPrice;
        } else if (quantityTier.contains("11-50") || quantityTier.contains("11-")) {
            return avgPrice.multiply(BigDecimal.valueOf(0.95));
        } else if (quantityTier.contains("51-100") || quantityTier.contains("100")) {
            return avgPrice.multiply(BigDecimal.valueOf(0.90));
        }
        return avgPrice;
    }
}