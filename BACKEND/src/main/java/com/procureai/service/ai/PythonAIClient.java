package com.procureai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * HTTP client for ProcureAI Python FastAPI AI Service.
 *
 * Responsibilities:
 *  - Build and send typed JSON requests to FastAPI
 *  - Handle timeouts, connection failures, malformed JSON
 *  - Return empty Optional on any failure (Spring Boot falls back to GeminiAIProvider)
 *  - Never throw exceptions out of this class
 *  - Never expose raw FastAPI error responses to users
 *
 * Security:
 *  - All inputs are validated by callers before reaching this client
 *  - Optional X-Internal-Token header if PYTHON_AI_INTERNAL_TOKEN is set
 *  - Never logs API keys or sensitive personal data
 */
@Component
public class PythonAIClient {

    private static final Logger log = LoggerFactory.getLogger(PythonAIClient.class);

    @Value("${app.python-ai.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${app.python-ai.enabled:false}")
    private boolean enabled;

    @Value("${app.python-ai.internal-token:}")
    private String internalToken;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PythonAIClient(ObjectMapper objectMapper,
                          @Value("${app.python-ai.connect-timeout-ms:5000}") int connectTimeoutMs,
                          @Value("${app.python-ai.read-timeout-ms:60000}") int readTimeoutMs) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    if (internalToken != null && !internalToken.isBlank()) {
                        request.getHeaders().set("X-Internal-Token", internalToken.trim());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    // ─── Health ────────────────────────────────────────────────────────────────

    /**
     * Returns true if FastAPI health endpoint responds with status=ok.
     * Called by AIServiceStartupBean on Spring Boot ready.
     */
    public boolean isHealthy() {
        if (!enabled) return false;
        try {
            String response = restClient.get()
                    .uri(URI.create(baseUrl + "/api/ai/health"))
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(response);
            boolean ok = "ok".equals(node.path("status").asText());
            if (ok) {
                String mode = node.path("mode").asText("unknown");
                log.info("FastAPI AI Service healthy — mode={}", mode);
            }
            return ok;
        } catch (Exception e) {
            log.debug("FastAPI health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── Quote Analysis ────────────────────────────────────────────────────────

    /**
     * POST /api/ai/analyze-quote
     * Returns raw JsonNode — caller validates before use.
     */
    public Optional<JsonNode> analyzeQuote(String rawText, String hintedVendorName) {
        if (!enabled) return Optional.empty();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("raw_text", truncate(rawText, 40000));
            if (hintedVendorName != null) body.put("hinted_vendor_name", truncate(hintedVendorName, 200));
            body.put("document_type", "quotation");
            return post("/api/ai/analyze-quote", body);
        } catch (Exception e) {
            log.warn("PythonAIClient.analyzeQuote failed: {} — Spring Boot will use fallback", e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Compare Quotes ────────────────────────────────────────────────────────

    /**
     * POST /api/ai/compare-quotes
     */
    public Optional<JsonNode> compareQuotes(List<QuoteInput> quotes, String category, BigDecimal budgetCeiling) {
        if (!enabled || quotes == null || quotes.size() < 2) return Optional.empty();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode quotesArr = body.putArray("quotes");
            for (QuoteInput q : quotes) {
                ObjectNode qNode = quotesArr.addObject();
                if (q.quoteId() != null) qNode.put("quote_id", q.quoteId());
                qNode.put("vendor_name", q.vendorName());
                qNode.put("total_price", q.totalPrice().doubleValue());
                qNode.put("warranty_months", q.warrantyMonths());
                qNode.put("delivery_days", q.deliveryDays());
                qNode.put("payment_terms", q.paymentTerms() != null ? q.paymentTerms() : "Net 30");
            }
            if (category != null) body.put("category", category);
            if (budgetCeiling != null) body.put("budget_ceiling", budgetCeiling.doubleValue());
            return post("/api/ai/compare-quotes", body);
        } catch (Exception e) {
            log.warn("PythonAIClient.compareQuotes failed: {} — using fallback", e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Recommend Vendor ──────────────────────────────────────────────────────

    public Optional<JsonNode> recommendVendor(List<QuoteInput> quotes, String category,
                                               BigDecimal budgetCeiling, int minWarranty, int maxDelivery) {
        if (!enabled || quotes == null || quotes.isEmpty()) return Optional.empty();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode quotesArr = body.putArray("quotes");
            for (QuoteInput q : quotes) {
                ObjectNode qNode = quotesArr.addObject();
                if (q.quoteId() != null) qNode.put("quote_id", q.quoteId());
                qNode.put("vendor_name", q.vendorName());
                qNode.put("total_price", q.totalPrice().doubleValue());
                qNode.put("warranty_months", q.warrantyMonths());
                qNode.put("delivery_days", q.deliveryDays());
            }
            if (category != null) body.put("category", category);
            if (budgetCeiling != null) body.put("budget_ceiling", budgetCeiling.doubleValue());
            body.put("required_warranty_months", minWarranty);
            body.put("max_delivery_days", maxDelivery);
            return post("/api/ai/recommend-vendor", body);
        } catch (Exception e) {
            log.warn("PythonAIClient.recommendVendor failed: {} — using fallback", e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Negotiation Strategy ──────────────────────────────────────────────────

    public Optional<JsonNode> negotiationStrategy(NegotiationInput input) {
        if (!enabled) return Optional.empty();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("vendor_name", truncate(input.vendorName(), 200));
            body.put("product_summary", truncate(input.productSummary(), 500));
            body.put("current_price", input.currentPrice().doubleValue());
            body.put("target_price", input.targetPrice().doubleValue());
            body.put("max_acceptable_price", input.maxAcceptablePrice().doubleValue());
            body.put("quantity", input.quantity());
            body.put("warranty_months", input.warrantyMonths());
            body.put("delivery_days", input.deliveryDays());
            body.put("min_warranty_months", input.minWarrantyMonths());
            body.put("max_delivery_days", input.maxDeliveryDays());
            body.put("negotiation_round", input.negotiationRound());
            if (input.benchmarkMin() != null) body.put("benchmark_min_price", input.benchmarkMin().doubleValue());
            if (input.benchmarkMax() != null) body.put("benchmark_max_price", input.benchmarkMax().doubleValue());
            return post("/api/ai/negotiation-strategy", body);
        } catch (Exception e) {
            log.warn("PythonAIClient.negotiationStrategy failed: {} — using fallback", e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Generate Negotiation Email ────────────────────────────────────────────

    public Optional<JsonNode> generateNegotiationEmail(NegotiationInput input, String approach, String strategy, int round) {
        if (!enabled) return Optional.empty();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("vendor_name", truncate(input.vendorName(), 200));
            body.put("product_summary", truncate(input.productSummary(), 500));
            body.put("current_price", input.currentPrice().doubleValue());
            body.put("target_price", input.targetPrice().doubleValue());
            body.put("quantity", input.quantity());
            body.put("strategy", truncate(strategy != null ? strategy : "", 1000));
            body.put("approach", approach != null ? approach : "Balanced");
            body.put("negotiation_round", round);
            body.put("sender_name", "Procurement Team");
            body.put("sender_org", "ProcureAI");
            return post("/api/ai/generate-negotiation", body);
        } catch (Exception e) {
            log.warn("PythonAIClient.generateNegotiationEmail failed: {} — using fallback", e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Vendor Response Analysis ──────────────────────────────────────────────

    public Optional<JsonNode> analyzeVendorResponse(String vendorName, String productSummary,
                                                     BigDecimal originalPrice, BigDecimal counterPrice,
                                                     BigDecimal targetPrice, BigDecimal maxPrice,
                                                     int round, int maxRounds) {
        if (!enabled) return Optional.empty();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("vendor_name", truncate(vendorName, 200));
            body.put("product_summary", truncate(productSummary, 500));
            body.put("original_price", originalPrice.doubleValue());
            body.put("counter_price", counterPrice.doubleValue());
            body.put("target_price", targetPrice.doubleValue());
            body.put("max_acceptable_price", maxPrice.doubleValue());
            body.put("negotiation_round", round);
            body.put("max_rounds", maxRounds);
            return post("/api/ai/analyze-vendor-response", body);
        } catch (Exception e) {
            log.warn("PythonAIClient.analyzeVendorResponse failed: {} — using fallback", e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Vendor Evaluation ─────────────────────────────────────────────────────

    public Optional<JsonNode> evaluateVendor(String vendorName, BigDecimal totalPrice,
                                              int warrantyMonths, int deliveryDays, String paymentTerms,
                                              String category, BigDecimal budgetCeiling,
                                              int requiredWarranty, int maxDelivery) {
        if (!enabled) return Optional.empty();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode vendor = body.putObject("vendor");
            vendor.put("vendor_name", truncate(vendorName, 200));
            vendor.put("total_price", totalPrice.doubleValue());
            vendor.put("warranty_months", warrantyMonths);
            vendor.put("delivery_days", deliveryDays);
            vendor.put("payment_terms", paymentTerms != null ? paymentTerms : "Net 30");
            if (category != null) vendor.put("category", category);
            if (budgetCeiling != null) body.put("budget_ceiling", budgetCeiling.doubleValue());
            body.put("required_warranty_months", requiredWarranty);
            body.put("max_delivery_days", maxDelivery);
            return post("/api/ai/evaluate-vendor", body);
        } catch (Exception e) {
            log.warn("PythonAIClient.evaluateVendor failed: {} — using fallback", e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Internal helpers ──────────────────────────────────────────────────────

    private Optional<JsonNode> post(String path, Object body) {
        try {
            String responseStr = restClient.post()
                    .uri(URI.create(baseUrl + path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseStr == null || responseStr.isBlank()) {
                log.warn("FastAPI returned empty response for {}", path);
                return Optional.empty();
            }
            JsonNode result = objectMapper.readTree(responseStr);
            log.debug("FastAPI {} — ai_mode={}", path, result.path("ai_mode").asText("unknown"));
            return Optional.of(result);

        } catch (ResourceAccessException e) {
            log.warn("FastAPI AI service unreachable at {} — {}", baseUrl + path, e.getMessage());
            return Optional.empty();
        } catch (RestClientResponseException e) {
            log.warn("FastAPI {} returned HTTP {}: {}", path, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("FastAPI {} call failed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    // ─── Input DTOs ────────────────────────────────────────────────────────────

    public record QuoteInput(Long quoteId, String vendorName, BigDecimal totalPrice,
                              int warrantyMonths, int deliveryDays, String paymentTerms) {}

    public record NegotiationInput(String vendorName, String productSummary,
                                    BigDecimal currentPrice, BigDecimal targetPrice,
                                    BigDecimal maxAcceptablePrice, int quantity,
                                    int warrantyMonths, int deliveryDays,
                                    int minWarrantyMonths, int maxDeliveryDays,
                                    int negotiationRound,
                                    BigDecimal benchmarkMin, BigDecimal benchmarkMax) {}
}
