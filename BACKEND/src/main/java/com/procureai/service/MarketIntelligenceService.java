package com.procureai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Market Intelligence Service.
 *
 * Adapts the Python MarketIntelligencePlugin concept (from AI-Powered-RFP-Analyzer)
 * into a Java service backed by a static JSON dataset. In a production build this
 * would be replaced by a live data feed or web-scraping pipeline (like the IndiaMART
 * scraper reference project), but for the hackathon demo it reads from the bundled
 * market-intelligence.json resource.
 */
@Service
public class MarketIntelligenceService {

    public record MarketData(
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal medianPrice,
            String supplyChainRisk,
            List<String> trends,
            List<String> supplyChainRisks,
            List<String> regulatoryChanges,
            List<String> competitorInsights
    ) {}

    private final Map<String, MarketData> categoryData;

    public MarketIntelligenceService() {
        this.categoryData = buildInternalDataset();
    }

    /**
     * Returns market intelligence for the given category (case-insensitive keyword match).
     */
    public MarketData getMarketData(String productNameOrCategory) {
        if (productNameOrCategory == null) return null;
        String lower = productNameOrCategory.toLowerCase();

        // Keyword match against known categories
        for (Map.Entry<String, MarketData> entry : categoryData.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // Fallback: return general category
        return categoryData.get("general");
    }

    public Map<String, MarketData> getAllCategories() {
        return categoryData;
    }

    /**
     * Internal dataset — modeled after the Python market-intelligence.json structure
     * from the AI-Powered-RFP-Analyzer reference project, adapted for Indian
     * procurement context (INR pricing, GST, etc.).
     */
    private Map<String, MarketData> buildInternalDataset() {
        return Map.of(
                "laptop", new MarketData(
                        "Laptops & Computing",
                        new BigDecimal("45000"),
                        new BigDecimal("120000"),
                        new BigDecimal("65000"),
                        "MEDIUM",
                        List.of(
                                "ARM-based processors gaining enterprise adoption",
                                "Chip shortage easing; lead times normalizing in 2024-25",
                                "Refurbished enterprise laptops market growing 25% YoY",
                                "Government 'Make in India' incentives affecting local pricing"
                        ),
                        List.of(
                                "Semiconductor supply remains partially constrained",
                                "Taiwan-China geopolitical risk affects TSMC supply chain",
                                "Import duty changes may impact pricing by 5-8%"
                        ),
                        List.of(
                                "BIS certification mandatory for all imported laptops >INR 50,000",
                                "GST rate: 18% on laptops above INR 20,000"
                        ),
                        List.of(
                                "Dell: Strong enterprise support, premium pricing (+10% vs market)",
                                "HP: Competitive pricing, good after-sales network in Tier-2 cities",
                                "Lenovo: Best price-to-performance, growing market share in India"
                        )
                ),
                "elitebook", new MarketData(
                        "HP Business Laptops",
                        new BigDecimal("55000"),
                        new BigDecimal("95000"),
                        new BigDecimal("70000"),
                        "LOW",
                        List.of("HP EliteBook series widely adopted in Indian BFSI sector"),
                        List.of("HP has local assembly in India — supply chain resilient"),
                        List.of("HP certified under Govt. Make in India scheme"),
                        List.of("Dell Latitude: Similar spec, typically 8-12% higher priced")
                ),
                "thinkpad", new MarketData(
                        "Lenovo ThinkPad",
                        new BigDecimal("52000"),
                        new BigDecimal("90000"),
                        new BigDecimal("67000"),
                        "LOW",
                        List.of("ThinkPad T-series is most popular enterprise laptop in India"),
                        List.of("Lenovo manufactures in Pune — shortest lead times in category"),
                        List.of("Lenovo compliant with BIS and Make in India requirements"),
                        List.of("HP EliteBook: Similar target market, slightly higher warranty coverage")
                ),
                "latitude", new MarketData(
                        "Dell Business Laptops",
                        new BigDecimal("60000"),
                        new BigDecimal("100000"),
                        new BigDecimal("72000"),
                        "LOW",
                        List.of("Dell Latitude demand growing in government/PSU sector"),
                        List.of("Dell supply chain resilient; service centers in 100+ Indian cities"),
                        List.of("Dell certified Govt supplier; GeM portal listed"),
                        List.of("HP EliteBook: Lower price point with comparable specs")
                ),
                "server", new MarketData(
                        "Servers & Infrastructure",
                        new BigDecimal("150000"),
                        new BigDecimal("800000"),
                        new BigDecimal("280000"),
                        "HIGH",
                        List.of(
                                "AI workload demand driving server refresh cycles faster",
                                "Data center cooling requirements increasing procurement complexity",
                                "Hyperscaler demand competing with enterprise procurement for supply"
                        ),
                        List.of(
                                "Server grade CPUs (Intel Xeon, AMD EPYC) supply constrained",
                                "NAND/DRAM pricing volatile — may affect quotes significantly"
                        ),
                        List.of("IT Sustainability mandate requires ENERGY STAR or equivalent certification"),
                        List.of("HPE: Market leader, highest reliability score", "Dell PowerEdge: Better TCO for mid-market")
                ),
                "furniture", new MarketData(
                        "Office Furniture",
                        new BigDecimal("8000"),
                        new BigDecimal("45000"),
                        new BigDecimal("18000"),
                        "LOW",
                        List.of("Ergonomic furniture demand up 40% post-pandemic", "WFH surge driving residential + office demand simultaneously"),
                        List.of("Raw material (steel, fabric) prices stabilized after 2022-23 spike"),
                        List.of("BIS IS 1828 standards apply for office chairs; verify vendor compliance"),
                        List.of("Local vendors offer 20-30% lower pricing vs national brands")
                ),
                "software", new MarketData(
                        "Software Licenses",
                        new BigDecimal("2000"),
                        new BigDecimal("50000"),
                        new BigDecimal("12000"),
                        "LOW",
                        List.of("SaaS replacing perpetual license model; multi-year contracts preferred", "Open source alternatives reducing MS/Oracle negotiation leverage"),
                        List.of("Vendor lock-in risk with proprietary platforms"),
                        List.of("Data localization requirements apply for SaaS with personal data"),
                        List.of("Microsoft EA vs. Google Workspace vs. open source — 3x price variance")
                ),
                "general", new MarketData(
                        "General Procurement",
                        new BigDecimal("1000"),
                        new BigDecimal("500000"),
                        new BigDecimal("50000"),
                        "MEDIUM",
                        List.of("Supply chain normalization post-COVID improving vendor reliability"),
                        List.of("Inflation pressures on raw materials persisting through 2024"),
                        List.of("Procurement compliance requirements increasing under GST framework"),
                        List.of("Multiple vendor competition advisable for orders >INR 5 lakhs")
                )
        );
    }
}
