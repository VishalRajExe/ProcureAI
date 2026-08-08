package com.procureai.controller;

import com.procureai.service.DemoService;
import com.procureai.util.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Demo workflow controller supporting scenario vendor selection.
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
    public ResponseEntity<Map<String, Object>> run(
            @RequestParam(value = "vendor", required = false) String vendor) {
        return ResponseEntity.ok(demoService.runDemo(CurrentUser.id(), vendor));
    }
}
