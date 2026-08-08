package com.procureai;

import com.procureai.service.DemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("demo")
@Transactional
@DisplayName("Full Procurement Workflow Integration & Demo Scenarios Audit Test")
class ProcureAiWorkflowIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ProcureAiWorkflowIntegrationTest.class);

    @Autowired
    private DemoService demoService;

    @Test
    @DisplayName("End-to-End Autonomous Procurement Workflow: HP Scenario")
    void testEndToEndDemoWorkflowHP() {
        Map<String, Object> result = demoService.runDemo(1L, "HP");

        assertNotNull(result, "Demo workflow result should not be null");
        assertNotNull(result.get("workflowId"), "Workflow ID should be present");
        assertNotNull(result.get("recommendedVendor"), "Recommended vendor should be populated");
        assertNotNull(result.get("poNumber"), "PO number should be generated");
        assertNotNull(result.get("poTotal"), "PO total should be calculated");

        assertThat(result.get("recommendedVendor").toString()).contains("HP");
        log.info("HP Scenario Test Succeeded: PO {} generated for vendor {}", result.get("poNumber"), result.get("recommendedVendor"));
    }

    @Test
    @DisplayName("End-to-End Autonomous Procurement Workflow: Lenovo Scenario")
    void testEndToEndDemoWorkflowLenovo() {
        Map<String, Object> result = demoService.runDemo(1L, "LENOVO");

        assertNotNull(result, "Demo workflow result should not be null");
        assertNotNull(result.get("workflowId"), "Workflow ID should be present");
        assertNotNull(result.get("recommendedVendor"), "Recommended vendor should be populated");
        assertNotNull(result.get("poNumber"), "PO number should be generated");
        assertNotNull(result.get("poTotal"), "PO total should be calculated");

        assertThat(result.get("recommendedVendor").toString()).contains("Lenovo");
        log.info("Lenovo Scenario Test Succeeded: PO {} generated for vendor {}", result.get("poNumber"), result.get("recommendedVendor"));
    }

    @Test
    @DisplayName("End-to-End Autonomous Procurement Workflow: Dell Scenario")
    void testEndToEndDemoWorkflowDell() {
        Map<String, Object> result = demoService.runDemo(1L, "DELL");

        assertNotNull(result, "Demo workflow result should not be null");
        assertNotNull(result.get("workflowId"), "Workflow ID should be present");
        assertNotNull(result.get("recommendedVendor"), "Recommended vendor should be populated");
        assertNotNull(result.get("poNumber"), "PO number should be generated");
        assertNotNull(result.get("poTotal"), "PO total should be calculated");

        assertThat(result.get("recommendedVendor").toString()).contains("Dell");
        log.info("Dell Scenario Test Succeeded: PO {} generated for vendor {}", result.get("poNumber"), result.get("recommendedVendor"));
    }
}
