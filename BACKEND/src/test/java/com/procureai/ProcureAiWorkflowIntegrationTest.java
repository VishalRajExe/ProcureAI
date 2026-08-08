package com.procureai;

import com.procureai.service.DemoService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("demo")
class ProcureAiWorkflowIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ProcureAiWorkflowIntegrationTest.class);

    @Autowired
    private DemoService demoService;

    @Test
    void testEndToEndDemoWorkflow() {
        Map<String, Object> result = demoService.runDemo(1L);

        assertNotNull(result, "Demo workflow result should not be null");
        assertNotNull(result.get("workflowId"), "Workflow ID should be present");
        assertNotNull(result.get("recommendedVendor"), "Recommended vendor should be populated");
        assertNotNull(result.get("poNumber"), "PO number should be generated");
        assertNotNull(result.get("poTotal"), "PO total should be calculated");

        log.info("Integration Test Succeeded: PO {} generated for vendor {}", result.get("poNumber"), result.get("recommendedVendor"));
    }
}
