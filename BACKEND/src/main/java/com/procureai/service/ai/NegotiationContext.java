package com.procureai.service.ai;

import java.math.BigDecimal;

/**
 * All inputs the AI is allowed to see/reason over for a negotiation decision.
 * Backend-owned limits (maxAcceptablePrice, minWarrantyMonths, maxDeliveryDays)
 * are enforced independently of whatever the AI recommends.
 */
public record NegotiationContext(
        String vendorName,
        String productSummary,
        BigDecimal currentPrice,
        BigDecimal benchmarkMinPrice,
        BigDecimal benchmarkMaxPrice,
        BigDecimal targetPrice,
        BigDecimal maxAcceptablePrice,
        Integer quantity,
        Integer warrantyMonths,
        Integer deliveryDays,
        Integer minWarrantyMonths,
        Integer maxDeliveryDays
) {}
