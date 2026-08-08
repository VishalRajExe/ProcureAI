package com.procureai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * Scoring weights for vendor comparison.
 *
 * All weights must be in [0.0, 1.0].
 * The sum of all weights must be exactly 1.0 (enforced in ScoringService).
 * Null values fall back to defaults in ScoringService.
 */
public record ScoringWeightsRequest(
        @DecimalMin(value = "0.0", message = "priceWeight must be >= 0.0")
        @DecimalMax(value = "1.0", message = "priceWeight must be <= 1.0")
        BigDecimal priceWeight,

        @DecimalMin(value = "0.0", message = "warrantyWeight must be >= 0.0")
        @DecimalMax(value = "1.0", message = "warrantyWeight must be <= 1.0")
        BigDecimal warrantyWeight,

        @DecimalMin(value = "0.0", message = "deliveryWeight must be >= 0.0")
        @DecimalMax(value = "1.0", message = "deliveryWeight must be <= 1.0")
        BigDecimal deliveryWeight,

        @DecimalMin(value = "0.0", message = "paymentTermsWeight must be >= 0.0")
        @DecimalMax(value = "1.0", message = "paymentTermsWeight must be <= 1.0")
        BigDecimal paymentTermsWeight,

        @DecimalMin(value = "0.0", message = "reliabilityWeight must be >= 0.0")
        @DecimalMax(value = "1.0", message = "reliabilityWeight must be <= 1.0")
        BigDecimal reliabilityWeight
) {}
