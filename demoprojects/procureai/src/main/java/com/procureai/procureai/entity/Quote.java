package com.procureai.procureai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quotes", indexes = {
    @Index(name = "idx_quotes_quote_number", columnList = "quote_number"),
    @Index(name = "idx_quotes_vendor_id", columnList = "vendor_id"),
    @Index(name = "idx_quotes_status", columnList = "status"),
    @Index(name = "idx_quotes_uploaded_by", columnList = "uploaded_by"),
    @Index(name = "idx_quotes_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "quote_number", nullable = false, unique = true, length = 50)
    private String quoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.UPLOADED;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    @Builder.Default
    private SourceType sourceType = SourceType.PDF;

    @Column(name = "source_filename", length = 255)
    private String sourceFilename;

    @Column(name = "source_file_path", length = 500)
    private String sourceFilePath;

    @Column(name = "source_file_hash", length = 64)
    private String sourceFileHash;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "subtotal", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Column(name = "delivery_terms", length = 255)
    private String deliveryTerms;

    @Column(name = "warranty_period", length = 100)
    private String warrantyPeriod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ai_extracted", nullable = false)
    @Builder.Default
    private Boolean aiExtracted = false;

    @Column(name = "ai_confidence", precision = 3, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "ai_raw_response", columnDefinition = "JSONB")
    private String aiRawResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by")
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    public enum QuoteStatus {
        UPLOADED, PROCESSING, EXTRACTED, NORMALIZED, COMPARED, NEGOTIATING, APPROVED, REJECTED, EXPIRED, ARCHIVED
    }

    public enum SourceType {
        PDF, IMAGE, EMAIL, DOCX, MANUAL, API
    }
}