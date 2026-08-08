package com.procureai.service.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Structured, schema-valid AI document-extraction output. Every field is validated
 * against backend business rules before being persisted — the AI never writes
 * directly to the database or triggers actions.
 */
public record ExtractedQuoteData(
        String vendorName,
        List<Item> items,
        BigDecimal discountPercent,
        BigDecimal taxPercent,
        BigDecimal shippingCost,
        BigDecimal vendorDeclaredTotal,
        Integer warrantyMonths,
        Integer deliveryDays,
        String paymentTerms,
        String validUntil, // ISO date string, nullable
        double confidence,
        List<String> missingFields
) {
    public record Item(String productName, String model, Integer quantity, BigDecimal unitPrice) {}
}
