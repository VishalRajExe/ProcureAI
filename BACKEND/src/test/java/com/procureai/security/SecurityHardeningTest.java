package com.procureai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureai.dto.AuthDtos;
import com.procureai.entity.User;
import com.procureai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("security_admin@procureai.demo")) {
            User admin = new User();
            admin.setName("Security Admin");
            admin.setEmail("security_admin@procureai.demo");
            admin.setPasswordHash("$2a$12$e8rXh...placeholder");
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("security_user@procureai.demo")) {
            User user = new User();
            user.setName("Security User");
            user.setEmail("security_user@procureai.demo");
            user.setPasswordHash("$2a$12$e8rXh...placeholder");
            user.setRole(User.Role.PROCUREMENT_USER);
            userRepository.save(user);
        }

        User admin = userRepository.findByEmail("security_admin@procureai.demo").get();
        User user = userRepository.findByEmail("security_user@procureai.demo").get();

        adminToken = "Bearer " + jwtService.generateToken(admin.getEmail(), admin.getRole().name(), admin.getId());
        userToken = "Bearer " + jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
    }

    @Test
    @DisplayName("Login response must NOT expose password, passwordHash or internal secrets")
    void testLoginResponseNoPasswordExposed() throws Exception {
        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest("admin@procureai.demo", "Admin@12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.permissions").exists());
    }

    @Test
    @DisplayName("RBAC: Procurement user CANNOT approve financial negotiations")
    void testRBACProcurementUserCannotApprove() throws Exception {
        mockMvc.perform(post("/api/negotiations/1/approve")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\": true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: Admin CAN access audit logs")
    void testRBACAdminCanAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("File Upload: Reject invalid non-PDF file extension")
    void testFileUploadRejectInvalidExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", "MZ...executable".getBytes()
        );

        mockMvc.perform(multipart("/api/quotes/upload")
                        .file(file)
                        .param("vendorName", "Malicious Vendor")
                        .header("Authorization", userToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Only PDF and plain-text files are accepted. Received: script.exe"));
    }

    @Test
    @DisplayName("Input Validation: Reject negative price in vendor response")
    void testInputValidationRejectNegativePrice() throws Exception {
        mockMvc.perform(post("/api/negotiations/1/simulate-response")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"counterPrice\": -500.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Error Response: Generic error without stack traces")
    void testGenericErrorNoStackTrace() throws Exception {
        mockMvc.perform(get("/api/quotes/999999")
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.cause").doesNotExist());
    }
}
