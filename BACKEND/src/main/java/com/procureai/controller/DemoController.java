package com.procureai.controller;

import com.procureai.service.DemoService;
import com.procureai.util.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Demo workflow controller.
 *
 * Security: Requires authentication. Requires ADMIN or APPROVER role to run
 * the full automated procurement workflow (since it includes approval steps).
 *
 * This endpoint is intentionally restricted — an unauthenticated caller should
 * not be able to trigger database writes, vendor scoring, negotiation, or PO generation.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping({"/run", "/seed"})
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'PROCUREMENT_USER')")
    public ResponseEntity<Map<String, Object>> run() {
        return ResponseEntity.ok(demoService.runDemo(CurrentUser.id()));
    }
}
