package com.procureai.controller;

import com.procureai.service.MarketIntelligenceService;
import com.procureai.service.VendorIntelligenceService;
import com.procureai.service.QuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Market Intelligence REST controller.
 * Provides market pricing data and vendor intelligence assessments.
 */
@RestController
@RequestMapping("/api/market-intelligence")
public class MarketIntelligenceController {

    private final MarketIntelligenceService marketService;
    private final VendorIntelligenceService vendorIntelligenceService;
    private final QuoteService quoteService;

    public MarketIntelligenceController(MarketIntelligenceService marketService,
                                         VendorIntelligenceService vendorIntelligenceService,
                                         QuoteService quoteService) {
        this.marketService = marketService;
        this.vendorIntelligenceService = vendorIntelligenceService;
        this.quoteService = quoteService;
    }

    /**
     * Get market intelligence data for a specific product category.
     * Returns price ranges, trends, and supply chain risks.
     */
    @GetMapping("/{category}")
    public ResponseEntity<?> getMarketData(@PathVariable String category) {
        MarketIntelligenceService.MarketData data = marketService.getMarketData(category);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(data);
    }

    /** Get all available market intelligence categories */
    @GetMapping
    public ResponseEntity<Map<String, MarketIntelligenceService.MarketData>> getAllCategories() {
        return ResponseEntity.ok(marketService.getAllCategories());
    }

    /**
     * Get vendor intelligence bundle for a specific quote.
     * Runs all 5 agent assessments (RFP compliance, reputation, market, legal, negotiation).
     */
    @GetMapping("/quotes/{quoteId}/intelligence")
    public ResponseEntity<VendorIntelligenceService.VendorIntelligenceBundle> getVendorIntelligence(
            @PathVariable Long quoteId) {
        return ResponseEntity.ok(vendorIntelligenceService.assess(quoteService.getQuote(quoteId)));
    }
}
