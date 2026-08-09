package com.procureai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Root endpoint controller providing service status and directing users to the frontend application URL.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> rootInfo() {
        return ResponseEntity.ok(Map.of(
                "service", "ProcureAI REST API",
                "status", "ONLINE",
                "frontendUrl", "http://localhost:5173",
                "productionUrl", "https://procure-ai.web.app",
                "message", "ProcureAI Backend API is running! Open http://localhost:5173 in your browser to use the ProcureAI App."
        ));
    }
}
