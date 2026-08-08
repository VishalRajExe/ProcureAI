package com.procureai.service;

import com.procureai.entity.Quote;
import com.procureai.entity.QuoteItem;
import com.procureai.repository.QuoteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Market Intelligence Service.
 *
 * Combines reference benchmark datasets with live ingested database quotes
 * so uploaded quote data dynamically reflects in market ranges, median pricing,
 * and competitor insights.
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

    private final QuoteRepository quoteRepository;
    private final Map<String, MarketData> baseDataset;

    public MarketIntelligenceService(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
        this.baseDataset = buildInternalDataset();
    }

    /**
     * Returns market intelligence for the given category (case-insensitive keyword match).
     */
    public MarketData getMarketData(String productNameOrCategory) {
        if (productNameOrCategory == null) return null;
        Map<String, MarketData> all = getAllCategories();
        String lower = productNameOrCategory.toLowerCase();

        for (Map.Entry<String, MarketData> entry : all.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase()) || entry.getKey().toLowerCase().contains(lower)) {
                return entry.getValue();
            }
        }
        return all.getOrDefault("general", baseDataset.get("general"));
    }

    public Map<String, MarketData> getAllCategories() {
        Map<String, MarketData> merged = new LinkedHashMap<>(baseDataset);
        List<Quote> quotes = quoteRepository.findAll();

        if (quotes == null || quotes.isEmpty()) {
            return merged;
        }

        // Group live quote items by category/product key
        Map<String, List<BigDecimal>> livePrices = new HashMap<>();
        Map<String, List<String>> liveInsights = new HashMap<>();

        for (Quote quote : quotes) {
            String vendorName = quote.getVendor() != null ? quote.getVendor().getName() : "Vendor";
            if (quote.getItems() == null) continue;

            for (QuoteItem item : quote.getItems()) {
                if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) continue;

                String key = matchCategoryKey(item.getProductName());
                livePrices.computeIfAbsent(key, k -> new ArrayList<>()).add(item.getUnitPrice());

                String insight = "Live Ingested Quote: " + vendorName + " offered " + item.getProductName() + " at Rs. " + item.getUnitPrice();
                liveInsights.computeIfAbsent(key, k -> new ArrayList<>()).add(insight);
            }
        }

        // Update base categories with live price ranges
        for (Map.Entry<String, List<BigDecimal>> entry : livePrices.entrySet()) {
            String key = entry.getKey();
            List<BigDecimal> prices = entry.getValue();
            Collections.sort(prices);

            MarketData base = merged.get(key);
            List<String> insights = liveInsights.getOrDefault(key, List.of());

            if (base != null) {
                BigDecimal minP = base.minPrice().min(prices.get(0));
                BigDecimal maxP = base.maxPrice().max(prices.get(prices.size() - 1));
                BigDecimal medianP = computeMedian(prices, base.medianPrice());

                List<String> updatedInsights = new ArrayList<>(base.competitorInsights());
                for (String ins : insights) {
                    if (!updatedInsights.contains(ins)) {
                        updatedInsights.add(0, ins);
                    }
                }

                merged.put(key, new MarketData(
                        base.category(),
                        minP,
                        maxP,
                        medianP,
                        base.supplyChainRisk(),
                        base.trends(),
                        base.supplyChainRisks(),
                        base.regulatoryChanges(),
                        updatedInsights
                ));
            } else {
                // Dynamically create a new MarketData category for custom uploaded items
                BigDecimal minP = prices.get(0);
                BigDecimal maxP = prices.get(prices.size() - 1);
                BigDecimal medianP = computeMedian(prices, minP);

                String formattedCat = capitalizeWords(key);
                merged.put(key, new MarketData(
                        formattedCat,
                        minP,
                        maxP,
                        medianP,
                        "LOW",
                        List.of("Recent quote data ingested from live procurement workflows"),
                        List.of("Standard market availability"),
                        List.of("Standard GST & commercial compliance"),
                        insights
                ));
            }
        }

        return merged;
    }

    private String matchCategoryKey(String productName) {
        if (productName == null) return "general";
        String lower = productName.toLowerCase();
        if (lower.contains("thinkpad") || lower.contains("lenovo")) return "thinkpad";
        if (lower.contains("latitude") || lower.contains("dell")) return "latitude";
        if (lower.contains("elitebook") || lower.contains("hp")) return "elitebook";
        if (lower.contains("laptop") || lower.contains("computing")) return "laptop";
        if (lower.contains("server") || lower.contains("infrastructure")) return "server";
        if (lower.contains("furniture") || lower.contains("chair") || lower.contains("desk")) return "furniture";
        if (lower.contains("software") || lower.contains("license") || lower.contains("saas")) return "software";

        String clean = lower.replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        return clean.isBlank() ? "general" : clean;
    }

    private BigDecimal computeMedian(List<BigDecimal> prices, BigDecimal fallback) {
        if (prices == null || prices.isEmpty()) return fallback;
        int size = prices.size();
        if (size % 2 == 1) {
            return prices.get(size / 2);
        } else {
            return prices.get(size / 2 - 1).add(prices.get(size / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return "Custom Procurement";
        return Arrays.stream(str.split("-"))
                .filter(s -> !s.isBlank())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    /**
     * Internal dataset — modeled after the Python market-intelligence.json structure
     * from the AI-Powered-RFP-Analyzer reference project, adapted for Indian
     * procurement context (INR pricing, GST, etc.).
     */
    private Map<String, MarketData> buildInternalDataset() {
        Map<String, MarketData> map = new LinkedHashMap<>();
        map.put("laptop", new MarketData(
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
        ));
        map.put("elitebook", new MarketData(
                "HP Business Laptops",
                new BigDecimal("55000"),
                new BigDecimal("95000"),
                new BigDecimal("70000"),
                "LOW",
                List.of("HP EliteBook series widely adopted in Indian BFSI sector"),
                List.of("HP has local assembly in India — supply chain resilient"),
                List.of("HP certified under Govt. Make in India scheme"),
                List.of("Dell Latitude: Similar spec, typically 8-12% higher priced")
        ));
        map.put("thinkpad", new MarketData(
                "Lenovo ThinkPad",
                new BigDecimal("52000"),
                new BigDecimal("90000"),
                new BigDecimal("67000"),
                "LOW",
                List.of("ThinkPad T-series is most popular enterprise laptop in India"),
                List.of("Lenovo manufactures in Pune — shortest lead times in category"),
                List.of("Lenovo compliant with BIS and Make in India requirements"),
                List.of("HP EliteBook: Similar target market, slightly higher warranty coverage")
        ));
        map.put("latitude", new MarketData(
                "Dell Business Laptops",
                new BigDecimal("60000"),
                new BigDecimal("100000"),
                new BigDecimal("72000"),
                "LOW",
                List.of("Dell Latitude demand growing in government/PSU sector"),
                List.of("Dell supply chain resilient; service centers in 100+ Indian cities"),
                List.of("Dell certified Govt supplier; GeM portal listed"),
                List.of("HP EliteBook: Lower price point with comparable specs")
        ));
        map.put("server", new MarketData(
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
        ));
        map.put("furniture", new MarketData(
                "Office Furniture",
                new BigDecimal("8000"),
                new BigDecimal("45000"),
                new BigDecimal("18000"),
                "LOW",
                List.of("Ergonomic furniture demand up 40% post-pandemic", "WFH surge driving residential + office demand simultaneously"),
                List.of("Raw material (steel, fabric) prices stabilized after 2022-23 spike"),
                List.of("BIS IS 1828 standards apply for office chairs; verify vendor compliance"),
                List.of("Local vendors offer 20-30% lower pricing vs national brands")
        ));
        map.put("software", new MarketData(
                "Software Licenses",
                new BigDecimal("2000"),
                new BigDecimal("50000"),
                new BigDecimal("12000"),
                "LOW",
                List.of("SaaS replacing perpetual license model; multi-year contracts preferred", "Open source alternatives reducing MS/Oracle negotiation leverage"),
                List.of("Vendor lock-in risk with proprietary platforms"),
                List.of("Data localization requirements apply for SaaS with personal data"),
                List.of("Microsoft EA vs. Google Workspace vs. open source — 3x price variance")
        ));
        map.put("general", new MarketData(
                "General Procurement",
                new BigDecimal("1000"),
                new BigDecimal("500000"),
                new BigDecimal("50000"),
                "MEDIUM",
                List.of("Supply chain normalization post-COVID improving vendor reliability"),
                List.of("Inflation pressures on raw materials persisting through 2024"),
                List.of("Procurement compliance requirements increasing under GST framework"),
                List.of("Multiple vendor competition advisable for orders >INR 5 lakhs")
        ));
        return map;
    }
}
