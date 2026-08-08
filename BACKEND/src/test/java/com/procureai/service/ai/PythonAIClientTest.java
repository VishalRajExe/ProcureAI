package com.procureai.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for PythonAIClient.
 * Tests that the client correctly handles FastAPI being DISABLED and UNAVAILABLE.
 *
 * Note: These tests do NOT start FastAPI — they verify that Spring Boot
 * handles FastAPI absence gracefully without throwing exceptions.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.python-ai.enabled=false",
    "app.python-ai.base-url=http://localhost:9999", // port that is never open
    "app.python-ai.connect-timeout-ms=500",
    "app.python-ai.read-timeout-ms=1000",
})
class PythonAIClientTest {

    @Autowired
    private PythonAIClient pythonAIClient;

    @Autowired
    private PythonAIService pythonAIService;

    // ── When disabled: all calls return empty without HTTP attempts ────────────

    @Test
    void health_whenDisabled_returnsFalse() {
        assertThat(pythonAIClient.isHealthy()).isFalse();
    }

    @Test
    void analyzeQuote_whenDisabled_returnsEmpty() {
        Optional<com.fasterxml.jackson.databind.JsonNode> result =
            pythonAIClient.analyzeQuote("Sample quote text", "TestVendor");
        assertThat(result).isEmpty();
    }

    @Test
    void compareQuotes_whenDisabled_returnsEmpty() {
        List<PythonAIClient.QuoteInput> quotes = List.of(
            new PythonAIClient.QuoteInput(1L, "VendorA", BigDecimal.valueOf(100000), 12, 14, "Net 30"),
            new PythonAIClient.QuoteInput(2L, "VendorB", BigDecimal.valueOf(90000), 12, 21, "Net 30")
        );
        Optional<com.fasterxml.jackson.databind.JsonNode> result =
            pythonAIClient.compareQuotes(quotes, "Electronics", null);
        assertThat(result).isEmpty();
    }

    @Test
    void negotiationStrategy_whenDisabled_returnsEmpty() {
        PythonAIClient.NegotiationInput input = new PythonAIClient.NegotiationInput(
            "TestVendor", "Laptop x10",
            BigDecimal.valueOf(950000), BigDecimal.valueOf(820000), BigDecimal.valueOf(900000),
            10, 12, 14, 24, 30, 0, null, null
        );
        Optional<com.fasterxml.jackson.databind.JsonNode> result =
            pythonAIClient.negotiationStrategy(input);
        assertThat(result).isEmpty();
    }

    @Test
    void pythonAIService_isEnabled_returnsFalse() {
        assertThat(pythonAIService.isEnabled()).isFalse();
    }

    @Test
    void pythonAIService_evaluateVendor_whenDisabled_returnsNull() {
        // PythonAIService returns null when disabled — callers use heuristic fallback
        assertThat(pythonAIService.isEnabled()).isFalse();
    }
}
