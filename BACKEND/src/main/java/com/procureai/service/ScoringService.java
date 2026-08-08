package com.procureai.service;

import com.procureai.dto.ScoringWeightsRequest;
import com.procureai.entity.Quote;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configurable vendor scoring engine. All numerical scores are computed here in the
 * backend — the AI may explain a score in natural language but never invents the
 * number itself.
 */
@Service
public class ScoringService {

    public record Weights(double price, double warranty, double delivery, double paymentTerms, double reliability) {
        public static final Weights DEFAULT = new Weights(0.40, 0.20, 0.15, 0.10, 0.15);

        public static Weights from(ScoringWeightsRequest req) {
            if (req == null) return DEFAULT;
            double price = pct(req.priceWeight(), DEFAULT.price);
            double warranty = pct(req.warrantyWeight(), DEFAULT.warranty);
            double delivery = pct(req.deliveryWeight(), DEFAULT.delivery);
            double payment = pct(req.paymentTermsWeight(), DEFAULT.paymentTerms);
            double reliability = pct(req.reliabilityWeight(), DEFAULT.reliability);
            double sum = price + warranty + delivery + payment + reliability;
            if (Math.abs(sum - 1.0) > 0.01) {
                throw new IllegalArgumentException("Scoring weights must sum to 100 (got " + Math.round(sum * 100) + ")");
            }
            return new Weights(price, warranty, delivery, payment, reliability);
        }

        private static double pct(BigDecimal v, double fallback) {
            return v == null ? fallback : v.doubleValue() / 100.0;
        }
    }

    /** Scores every quote in the list (0-100), using min/max normalization across the set. */
    public void scoreAll(List<Quote> quotes, Weights weights) {
        if (quotes.isEmpty()) return;

        BigDecimal minTotal = quotes.stream().map(Quote::getCalculatedTotal).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxTotal = quotes.stream().map(Quote::getCalculatedTotal).max(BigDecimal::compareTo).orElse(BigDecimal.ONE);

        int maxWarranty = quotes.stream().mapToInt(q -> nz(q.getWarrantyMonths())).max().orElse(1);
        int minWarranty = quotes.stream().mapToInt(q -> nz(q.getWarrantyMonths())).min().orElse(0);

        int maxDelivery = quotes.stream().mapToInt(q -> nz(q.getDeliveryDays())).max().orElse(1);
        int minDelivery = quotes.stream().mapToInt(q -> nz(q.getDeliveryDays())).min().orElse(0);

        for (Quote q : quotes) {
            double priceScore = normalizedInverse(q.getCalculatedTotal().doubleValue(), minTotal.doubleValue(), maxTotal.doubleValue());
            double warrantyScore = normalized(nz(q.getWarrantyMonths()), minWarranty, maxWarranty);
            double deliveryScore = normalizedInverse(nz(q.getDeliveryDays()), minDelivery, maxDelivery);
            double paymentScore = paymentTermsScore(q.getPaymentTerms());
            double reliabilityScore = (q.getVendor().getReliabilityScore() == null ? 75.0 : q.getVendor().getReliabilityScore()) / 100.0;

            double total = 100 * (
                    weights.price * priceScore +
                    weights.warranty * warrantyScore +
                    weights.delivery * deliveryScore +
                    weights.paymentTerms * paymentScore +
                    weights.reliability * reliabilityScore
            );
            q.setVendorScore(Math.round(total * 100.0) / 100.0);
        }
    }

    private double normalized(double value, double min, double max) {
        if (max == min) return 1.0;
        return (value - min) / (max - min);
    }

    private double normalizedInverse(double value, double min, double max) {
        if (max == min) return 1.0;
        return 1.0 - ((value - min) / (max - min));
    }

    private double paymentTermsScore(String terms) {
        if (terms == null) return 0.5;
        String t = terms.toLowerCase();
        if (t.contains("60")) return 1.0;
        if (t.contains("45")) return 0.8;
        if (t.contains("30")) return 0.6;
        if (t.contains("advance") || t.contains("15")) return 0.3;
        return 0.5;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
