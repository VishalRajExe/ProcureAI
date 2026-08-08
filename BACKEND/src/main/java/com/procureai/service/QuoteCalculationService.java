package com.procureai.service;

import com.procureai.entity.Quote;
import com.procureai.entity.QuoteItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of truth for all monetary calculations. Vendor-declared totals are
 * NEVER trusted directly — everything is recomputed here using BigDecimal with a
 * fixed scale/rounding mode to avoid floating point errors.
 *
 * Formula: Product Cost + Shipping + Taxes - Discounts = Actual Total
 */
@Service
public class QuoteCalculationService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** Recomputes each item's line subtotal and the quote's authoritative calculatedTotal. */
    public BigDecimal recalculate(Quote quote) {
        BigDecimal productCost = BigDecimal.ZERO;
        for (QuoteItem item : quote.getItems()) {
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            BigDecimal lineSubtotal = item.getUnitPrice().multiply(qty).setScale(SCALE, ROUNDING);
            item.setLineSubtotal(lineSubtotal);
            productCost = productCost.add(lineSubtotal);
        }

        BigDecimal discountPercent = nz(quote.getDiscountPercent());
        BigDecimal taxPercent = nz(quote.getTaxPercent());
        BigDecimal shipping = nz(quote.getShippingCost());

        BigDecimal discountAmount = productCost.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);

        BigDecimal taxableBase = productCost.subtract(discountAmount);
        BigDecimal taxAmount = taxableBase.multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);

        BigDecimal actualTotal = productCost.add(shipping).add(taxAmount).subtract(discountAmount);
        actualTotal = actualTotal.setScale(SCALE, ROUNDING);

        quote.setCalculatedTotal(actualTotal);
        return actualTotal;
    }

    public BigDecimal productCost(Quote quote) {
        BigDecimal total = BigDecimal.ZERO;
        for (QuoteItem item : quote.getItems()) {
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total.setScale(SCALE, ROUNDING);
    }

    public BigDecimal discountAmount(Quote quote) {
        return productCost(quote).multiply(nz(quote.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
    }

    public BigDecimal taxAmount(Quote quote) {
        BigDecimal taxableBase = productCost(quote).subtract(discountAmount(quote));
        return taxableBase.multiply(nz(quote.getTaxPercent()))
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
