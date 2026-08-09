package com.procureai.controller;

import com.procureai.entity.WorkflowExecution;
import com.procureai.service.EvaluationReportService;
import com.procureai.service.QuoteService;
import com.procureai.repository.WorkflowExecutionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for procurement workflow management.
 * Provides workflow listing, detail view, and full evaluation report generation.
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowExecutionRepository workflowRepository;
    private final EvaluationReportService reportService;
    private final QuoteService quoteService;

    public WorkflowController(WorkflowExecutionRepository workflowRepository,
                               EvaluationReportService reportService,
                               QuoteService quoteService) {
        this.workflowRepository = workflowRepository;
        this.reportService = reportService;
        this.quoteService = quoteService;
    }

    /** List all procurement workflows for the authenticated user */
    @GetMapping
    public ResponseEntity<List<WorkflowExecution>> listWorkflows() {
        return ResponseEntity.ok(quoteService.getUserWorkflows());
    }

    /** Get single workflow detail */
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowExecution> getWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.getWorkflow(id));
    }

    /**
     * Generate a full evaluation report for the workflow.
     * Consolidates all agent assessments: scoring, market intelligence,
     * legal compliance, negotiation outcome, and savings calculation.
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<EvaluationReportService.EvaluationReport> getReport(@PathVariable Long id) {
        WorkflowExecution workflow = quoteService.getWorkflow(id);
        return ResponseEntity.ok(reportService.generateReport(workflow));
    }
}
