package com.procureai;

import com.procureai.entity.Quote;
import com.procureai.entity.QuoteItem;
import com.procureai.service.QuoteCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuoteCalculationTest {

    private QuoteCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new QuoteCalculationService();
    }

    @Test
    void testCalculateQuoteTotalsWithItemsDiscountsAndTaxes() {
        Quote quote = new Quote();
        quote.setCurrency("INR");
        quote.setDiscountPercent(new BigDecimal("10.00")); // 10% discount
        quote.setTaxPercent(new BigDecimal("18.00"));      // 18% GST tax
        quote.setShippingCost(new BigDecimal("5000.00"));

        QuoteItem item1 = new QuoteItem();
        item1.setQuantity(50);
        item1.setUnitPrice(new BigDecimal("60000.00")); // 3,000,000

        QuoteItem item2 = new QuoteItem();
        item2.setQuantity(2);
        item2.setUnitPrice(new BigDecimal("100000.00")); // 200,000

        quote.setItems(List.of(item1, item2));

        BigDecimal calculatedTotal = calculationService.recalculate(quote);

        // Subtotal = 3,200,000
        // Discount 10% = 320,000 -> After Discount = 2,880,000
        // Tax 18% on 2,880,000 = 518,400 -> Subtotal + Tax = 3,398,400
        // Shipping = 5,000 -> Total = 3,403,400
        assertNotNull(calculatedTotal, "Calculated total should not be null");
        assertEquals(new BigDecimal("3403400.00"), calculatedTotal, "Authoritative total calculation must match financial rules");
    }

    @Test
    void testCalculateQuoteTotalZeroItems() {
        Quote quote = new Quote();
        quote.setItems(new ArrayList<>());
        quote.setVendorDeclaredTotal(new BigDecimal("150000.00"));

        BigDecimal calculatedTotal = calculationService.recalculate(quote);
        assertEquals(new BigDecimal("0.00"), calculatedTotal, "Zero item quotes should return 0.00 calculatedTotal");
    }
}
