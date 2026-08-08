package com.procureai;

import com.procureai.entity.Quote;
import com.procureai.entity.QuoteItem;
import com.procureai.service.QuoteCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class QuoteCalculationServiceTest {

    private QuoteCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new QuoteCalculationService();
    }

    @Test
    void testRecalculateTotalsWithDiscountTaxAndShipping() {
        Quote quote = new Quote();
        quote.setDiscountPercent(new BigDecimal("10.00")); // 10%
        quote.setTaxPercent(new BigDecimal("18.00"));      // 18%
        quote.setShippingCost(new BigDecimal("1500.00"));

        QuoteItem item = new QuoteItem();
        item.setQuantity(50);
        item.setUnitPrice(new BigDecimal("60000.00"));
        item.setQuote(quote);
        quote.getItems().add(item);

        calculationService.recalculate(quote);

        // Subtotal = 50 * 60,000 = 3,000,000
        // Discount = 10% of 3,000,000 = 300,000
        // Net taxable = 2,700,000
        // Tax = 18% of 2,700,000 = 486,000
        // Total = 2,700,000 + 486,000 + 1,500 = 3,187,500
        assertEquals(new BigDecimal("3000000.00"), quote.getItems().get(0).getLineSubtotal());
        assertEquals(new BigDecimal("300000.00"), calculationService.discountAmount(quote));
        assertEquals(new BigDecimal("486000.00"), calculationService.taxAmount(quote));
        assertEquals(new BigDecimal("3187500.00"), quote.getCalculatedTotal());
    }
}
