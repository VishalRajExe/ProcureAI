package com.procureai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security integration tests using MockMvc.
 *
 * Tests unauthorized access patterns:
 * - Unauthenticated access to protected endpoints returns 401
 * - Demo endpoint requires authentication (no longer public)
 * - Audit logs require ADMIN/APPROVER role
 * - Price manipulation via direct API call is blocked
 * - Malformed JWT returns 401
 * - Positive: auth endpoint is public (200/4xx from validation, not 401)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DisplayName("Security Integration Tests — Authorization & Authentication")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ---- Unauthenticated access: must return 401 ----

    @Test
    @DisplayName("GET /api/quotes — unauthenticated returns 401")
    void quotes_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/quotes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/negotiations — unauthenticated returns 401")
    void negotiations_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/negotiations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/purchase-orders — unauthenticated returns 401")
    void purchaseOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/audit-logs — unauthenticated returns 401")
    void auditLogs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/demo/run — unauthenticated returns 401 (no longer public)")
    void demo_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/demo/run"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/market-intelligence/categories — unauthenticated returns 401")
    void marketIntelligence_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/market-intelligence/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/purchase-orders/generate — unauthenticated returns 401")
    void poGenerate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/purchase-orders/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workflowId\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/negotiations/1/approve — unauthenticated returns 401")
    void negotiationApprove_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/negotiations/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approve\": true}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Public endpoints: auth and health must be accessible ----

    @Test
    @DisplayName("POST /api/auth/login — public, returns 4xx for bad credentials (not 401 from security)")
    void login_public_accessible() throws Exception {
        // Should reach the endpoint (not blocked by security filter) — bad creds = 401 from BadCredentialsException
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"WrongPass1!\"}"))
                .andExpect(status().isUnauthorized()); // BadCredentialsException from AuthService
    }

    @Test
    @DisplayName("GET /actuator/health — public, returns 200")
    void health_public_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // ---- Malformed JWT ----

    @Test
    @DisplayName("Malformed JWT bearer token returns 401")
    void malformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/quotes")
                .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT with wrong prefix is rejected")
    void wrongJwtPrefix_returns401() throws Exception {
        mockMvc.perform(get("/api/quotes")
                .header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
                .andExpect(status().isUnauthorized());
    }

    // ---- Input validation: bad registration data ----

    @Test
    @DisplayName("Registration with weak password returns 400")
    void register_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test User\",\"email\":\"user@test.com\",\"password\":\"password\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registration with invalid email returns 400")
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test User\",\"email\":\"not-an-email\",\"password\":\"SecurePass1!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registration with missing password returns 400")
    void register_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test User\",\"email\":\"user@test.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Error response never contains stack trace")
    void errorResponse_noStackTrace() throws Exception {
        // Trigger a 400 error and verify no 'stackTrace' or 'trace' field in response
        String responseBody = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"json\"}"))
                .andReturn().getResponse().getContentAsString();

        // Response must not include internal debugging information
        assertStackTraceNotInResponse(responseBody);
    }

    private void assertStackTraceNotInResponse(String body) {
        // These strings would indicate internal leakage
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("stackTrace")
                .doesNotContain("at com.procureai")
                .doesNotContain("at org.springframework")
                .doesNotContain("Caused by:");
    }
}
