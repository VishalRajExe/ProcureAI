package com.procureai.controller;

import com.procureai.entity.Approval;
import com.procureai.service.ApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Approval>> pending() {
        return ResponseEntity.ok(approvalService.pending());
    }

    @GetMapping
    public ResponseEntity<List<Approval>> all() {
        return ResponseEntity.ok(approvalService.all());
    }
}
