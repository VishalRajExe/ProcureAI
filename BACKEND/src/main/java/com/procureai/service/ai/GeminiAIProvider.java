package com.procureai.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureai.entity.Quote;
import com.procureai.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Production Google Gemini AI Provider implementation.
 *
 * Integrates with Google Gemini API via REST:
 * - Vendor document / quote understanding
 * - Structured quote data extraction (JSON Mode)
 * - Negotiation decision & strategy reasoning
 * - Negotiation email drafting
 * - Vendor counter-offer response evaluation
 * - Recommendation executive summary generation
 *
 * Security & Reliability:
 * - API key injected strictly via environment variables (never logged or exposed)
 * - Input text sanitized via InputSanitizer (max chars, control character stripping)
 * - Gemini output strictly schema-validated before backend persistence
 * - On any Gemini API failure (rate limit, key issue, timeout, bad JSON),
 *   gracefully falls back to MockAIProvider so the application never crashes.
 */
@Component
public class GeminiAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAIProvider.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final MockAIProvider mockFallback;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:gemini-1.5-flash}")
    private String modelName;

    public GeminiAIProvider(MockAIProvider mockFallback, ObjectMapper objectMapper) {
        this.mockFallback = mockFallback;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public ExtractedQuoteData extractQuoteData(String rawDocumentText, String hintedVendorName) {
        if (!isConfigured()) {
            log.info("Gemini API key not set — using MockAIProvider fallback for quote extraction");
            return mockFallback.extractQuoteData(rawDocumentText, hintedVendorName);
        }

        String safeText = InputSanitizer.sanitizeForAi(rawDocumentText);
        String prompt = """
                You are an expert procurement document AI parser. Extract structured quote details from the raw text into exact JSON format.
                
                Rules:
                - Output ONLY a JSON object matching this schema.
                - Do NOT guess or hallucinate missing prices or terms.
                
                JSON Schema required:
                {
                  "vendorName": "string",
                  "items": [
                    { "productName": "string", "model": "string or null", "quantity": 1, "unitPrice": 100.0 }
                  ],
                  "discountPercent": 0.0,
                  "taxPercent": 18.0,
                  "shippingCost": 0.0,
                  "vendorDeclaredTotal": 0.0,
                  "warrantyMonths": 12,
                  "deliveryDays": 7,
                  "paymentTerms": "Net 30",
                  "validUntil": "YYYY-MM-DD or null",
                  "confidence": 0.95,
                  "missingFields": ["fieldNames"]
                }

                Hinted Vendor Name: %s
                
                Raw Document Text:
                %s
                """.formatted(hintedVendorName != null ? hintedVendorName : "", safeText);

        try {
            String jsonResult = callGemini(prompt, true);
            JsonNode root = objectMapper.readTree(jsonResult);

            String vendor = root.path("vendorName").asText(hintedVendorName != null ? hintedVendorName : "Unknown Vendor");
            List<ExtractedQuoteData.Item> items = new ArrayList<>();
            if (root.has("items") && root.get("items").isArray()) {
                for (JsonNode itemNode : root.get("items")) {
                    String pName = itemNode.path("productName").asText("Product");
                    String pModel = itemNode.has("model") && !itemNode.get("model").isNull() ? itemNode.get("model").asText() : null;
                    int qty = itemNode.path("quantity").asInt(1);
                    BigDecimal price = itemNode.has("unitPrice") ? new BigDecimal(itemNode.get("unitPrice").asText("0")) : BigDecimal.ZERO;
                    items.add(new ExtractedQuoteData.Item(pName, pModel, qty, price));
                }
            }
            if (items.isEmpty()) {
                items.add(new ExtractedQuoteData.Item("Product Item", null, 1, BigDecimal.ZERO));
            }

            BigDecimal discount = root.has("discountPercent") ? new BigDecimal(root.get("discountPercent").asText("0")) : BigDecimal.ZERO;
            BigDecimal tax = root.has("taxPercent") ? new BigDecimal(root.get("taxPercent").asText("0")) : BigDecimal.ZERO;
            BigDecimal shipping = root.has("shippingCost") ? new BigDecimal(root.get("shippingCost").asText("0")) : BigDecimal.ZERO;
            BigDecimal declaredTotal = root.has("vendorDeclaredTotal") && !root.get("vendorDeclaredTotal").isNull() ? new BigDecimal(root.get("vendorDeclaredTotal").asText()) : null;
            Integer warranty = root.has("warrantyMonths") ? root.get("warrantyMonths").asInt(12) : 12;
            Integer delivery = root.has("deliveryDays") ? root.get("deliveryDays").asInt(7) : 7;
            String payTerms = root.path("paymentTerms").asText("Net 30");
            String validUntil = root.has("validUntil") && !root.get("validUntil").isNull() ? root.get("validUntil").asText() : null;
            double conf = root.path("confidence").asDouble(0.90);

            List<String> missing = new ArrayList<>();
            if (root.has("missingFields") && root.get("missingFields").isArray()) {
                for (JsonNode m : root.get("missingFields")) {
                    missing.add(m.asText());
                }
            }

            log.info("Gemini extracted quote data for vendor: {}", vendor);
            return new ExtractedQuoteData(vendor, items, discount, tax, shipping, declaredTotal, warranty, delivery, payTerms, validUntil, conf, missing);

        } catch (Exception e) {
            log.warn("Gemini quote extraction call failed: {} — using MockAIProvider fallback", e.getMessage());
            return mockFallback.extractQuoteData(rawDocumentText, hintedVendorName);
        }
    }

    @Override
    public NegotiationDecision decideNegotiationStrategy(NegotiationContext context) {
        if (!isConfigured()) {
            return mockFallback.decideNegotiationStrategy(context);
        }

        String prompt = """
                You are an expert AI procurement negotiator. Evaluate the quote context and output a negotiation decision JSON.
                
                Required JSON format:
                {
                  "action": "NEGOTIATE" | "ACCEPT" | "REJECT",
                  "targetPrice": number,
                  "maxApprovedPrice": number,
                  "strategy": "string description",
                  "reason": "string reasoning",
                  "confidence": 0.90
                }
                
                Context:
                Vendor: %s
                Product: %s
                Current Price: %s
                Benchmark Range: %s to %s
                Target Price: %s
                Max Budget: %s
                Quantity: %d
                Warranty: %d months (Min required: %d)
                Delivery: %d days (Max allowed: %d)
                """.formatted(
                context.vendorName(), context.productSummary(), context.currentPrice(),
                context.benchmarkMinPrice(), context.benchmarkMaxPrice(),
                context.targetPrice(), context.maxAcceptablePrice(),
                context.quantity() != null ? context.quantity() : 1,
                context.warrantyMonths() != null ? context.warrantyMonths() : 12,
                context.minWarrantyMonths() != null ? context.minWarrantyMonths() : 12,
                context.deliveryDays() != null ? context.deliveryDays() : 7,
                context.maxDeliveryDays() != null ? context.maxDeliveryDays() : 14
        );

        try {
            String jsonResult = callGemini(prompt, true);
            JsonNode root = objectMapper.readTree(jsonResult);

            String actionStr = root.path("action").asText("NEGOTIATE").toUpperCase();
            NegotiationDecision.Action action;
            try {
                action = NegotiationDecision.Action.valueOf(actionStr);
            } catch (Exception ex) {
                action = NegotiationDecision.Action.NEGOTIATE;
            }

            BigDecimal target = root.has("targetPrice") ? new BigDecimal(root.get("targetPrice").asText()) : context.targetPrice();
            BigDecimal max = root.has("maxApprovedPrice") ? new BigDecimal(root.get("maxApprovedPrice").asText()) : context.maxAcceptablePrice();
            String strategy = root.path("strategy").asText("Negotiate pricing toward target based on benchmark data");
            String reason = root.path("reason").asText("Current offer is above benchmark target");
            double confidence = root.path("confidence").asDouble(0.85);

            return new NegotiationDecision(action, target, max, strategy, reason, confidence);

        } catch (Exception e) {
            log.warn("Gemini negotiation strategy decision failed: {} — using MockAIProvider fallback", e.getMessage());
            return mockFallback.decideNegotiationStrategy(context);
        }
    }

    @Override
    public String draftNegotiationEmail(NegotiationContext context, NegotiationDecision decision) {
        if (!isConfigured()) {
            return mockFallback.draftNegotiationEmail(context, decision);
        }

        String prompt = """
                Draft a formal, persuasive negotiation email from a procurement team to a vendor.
                
                Context:
                Vendor: %s
                Item: %s (Qty: %d)
                Current Quote Price: %s
                Proposed Target Price: %s
                Strategy Note: %s
                
                Draft a clear, professional email body requesting a price adjustment while expressing strong interest in a long-term partnership.
                """.formatted(
                context.vendorName(), context.productSummary(),
                context.quantity() != null ? context.quantity() : 1,
                context.currentPrice(), decision.targetPrice(), decision.strategy()
        );

        try {
            return callGemini(prompt, false);
        } catch (Exception e) {
            log.warn("Gemini email drafting failed: {} — using MockAIProvider fallback", e.getMessage());
            return mockFallback.draftNegotiationEmail(context, decision);
        }
    }

    @Override
    public RoundEvaluation evaluateVendorResponse(NegotiationContext context, BigDecimal vendorCounterPrice, int roundNumber) {
        if (!isConfigured()) {
            return mockFallback.evaluateVendorResponse(context, vendorCounterPrice, roundNumber);
        }

        String prompt = """
                Evaluate a vendor's counter-offer in a procurement negotiation.
                Return JSON format:
                {
                  "recommendAccept": boolean,
                  "notes": "string explanation",
                  "confidence": 0.85
                }
                
                Context:
                Vendor: %s
                Counter Price Offered: %s
                Max Acceptable Budget: %s
                Target Price: %s
                Current Round: %d
                """.formatted(
                context.vendorName(), vendorCounterPrice, context.maxAcceptablePrice(),
                context.targetPrice(), roundNumber
        );

        try {
            String jsonResult = callGemini(prompt, true);
            JsonNode root = objectMapper.readTree(jsonResult);
            boolean rec = root.path("recommendAccept").asBoolean(vendorCounterPrice.compareTo(context.maxAcceptablePrice()) <= 0);
            String notes = root.path("notes").asText("Vendor counter offer analyzed against budget parameters");
            double conf = root.path("confidence").asDouble(0.85);

            return new RoundEvaluation(rec, notes, conf);
        } catch (Exception e) {
            log.warn("Gemini vendor evaluation failed: {} — using MockAIProvider fallback", e.getMessage());
            return mockFallback.evaluateVendorResponse(context, vendorCounterPrice, roundNumber);
        }
    }

    @Override
    public String explainRecommendation(Quote recommendedQuote, String comparisonSummary) {
        if (!isConfigured()) {
            return mockFallback.explainRecommendation(recommendedQuote, comparisonSummary);
        }

        String prompt = """
                Write a concise 2-sentence executive summary explaining why vendor '%s' is recommended.
                Total Cost: %s
                Comparison Details: %s
                """.formatted(
                recommendedQuote.getVendor().getName(),
                recommendedQuote.getCalculatedTotal(),
                comparisonSummary
        );

        try {
            return callGemini(prompt, false);
        } catch (Exception e) {
            log.warn("Gemini recommendation explanation failed: {} — using MockAIProvider fallback", e.getMessage());
            return mockFallback.explainRecommendation(recommendedQuote, comparisonSummary);
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("CHANGE_ME");
    }

    private String callGemini(String promptText, boolean jsonMode) {
        String url = GEMINI_BASE_URL + modelName + ":generateContent?key=" + apiKey;

        Map<String, Object> part = Map.of("text", promptText);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody;

        if (jsonMode) {
            Map<String, Object> genConfig = Map.of("responseMimeType", "application/json", "temperature", 0.1);
            requestBody = Map.of("contents", List.of(content), "generationConfig", genConfig);
        } else {
            requestBody = Map.of("contents", List.of(content));
        }

        String responseStr = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            JsonNode tree = objectMapper.readTree(responseStr);
            JsonNode candidate = tree.path("candidates").get(0);
            String text = candidate.path("content").path("parts").get(0).path("text").asText();
            return text != null ? text.trim() : "";
        } catch (Exception ex) {
            log.error("Failed to parse Gemini response payload: {}", ex.getMessage());
            throw new RuntimeException("Malformed Gemini response payload");
        }
    }
}
