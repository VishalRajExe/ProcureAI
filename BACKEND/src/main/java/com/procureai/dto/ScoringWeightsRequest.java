package com.procureai.dto;

import java.math.BigDecimal;

/** All weights must sum to 100; validated in ScoringService. */
public record ScoringWeightsRequest(
        BigDecimal priceWeight,
        BigDecimal warrantyWeight,
        BigDecimal deliveryWeight,
        BigDecimal paymentTermsWeight,
        BigDecimal reliabilityWeight
) {}
