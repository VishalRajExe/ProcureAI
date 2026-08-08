package com.procureai.controller;

import com.procureai.service.DemoService;
import com.procureai.util.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Backs the "RUN DEMO PROCUREMENT" button — runs the entire workflow automatically. */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping({"/run", "/seed"})
    public ResponseEntity<Map<String, Object>> run() {
        return ResponseEntity.ok(demoService.runDemo(CurrentUser.id()));
    }
}
