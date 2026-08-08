package com.procureai;

import com.procureai.entity.Quote;
import com.procureai.entity.Vendor;
import com.procureai.service.BenchmarkService;
import com.procureai.service.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoringServiceTest {

    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService();
    }

    @Test
    void testScoringDeterministicCalculation() {
        Vendor v1 = new Vendor();
        v1.setName("Vendor A");
        v1.setReliabilityScore(90.0);

        Quote q1 = new Quote();
        q1.setVendor(v1);
        q1.setCalculatedTotal(new BigDecimal("3000000.00"));
        q1.setWarrantyMonths(36);
        q1.setDeliveryDays(10);
        q1.setPaymentTerms("Net 30");
        q1.setBenchmarkStatus(Quote.BenchmarkStatus.WITHIN);

        Vendor v2 = new Vendor();
        v2.setName("Vendor B");
        v2.setReliabilityScore(70.0);

        Quote q2 = new Quote();
        q2.setVendor(v2);
        q2.setCalculatedTotal(new BigDecimal("4000000.00"));
        q2.setWarrantyMonths(12);
        q2.setDeliveryDays(30);
        q2.setPaymentTerms("Net 15");
        q2.setBenchmarkStatus(Quote.BenchmarkStatus.ABOVE);

        scoringService.scoreAll(List.of(q1, q2), ScoringService.Weights.DEFAULT);

        assertNotNull(q1.getVendorScore());
        assertNotNull(q2.getVendorScore());
        assertTrue(q1.getVendorScore() > q2.getVendorScore(), "Vendor A should score higher due to lower cost, longer warranty, faster delivery and higher reliability");
    }
}
