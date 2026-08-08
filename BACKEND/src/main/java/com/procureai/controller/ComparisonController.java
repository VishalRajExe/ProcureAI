package com.procureai.controller;

import com.procureai.dto.ScoringWeightsRequest;
import com.procureai.repository.WorkflowExecutionRepository;
import com.procureai.service.ComparisonService;
import com.procureai.service.ScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comparison")
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final WorkflowExecutionRepository workflowRepository;

    public ComparisonController(ComparisonService comparisonService, WorkflowExecutionRepository workflowRepository) {
        this.comparisonService = comparisonService;
        this.workflowRepository = workflowRepository;
    }

    @GetMapping
    public ResponseEntity<ComparisonService.ComparisonResult> getLatestComparison() {
        Long workflowId = workflowRepository.findTopByOrderByCreatedAtDesc()
                .map(com.procureai.entity.WorkflowExecution::getId)
                .orElse(null);
        if (workflowId == null) {
            return ResponseEntity.ok(new ComparisonService.ComparisonResult(List.of(), null, "No workflows found. Seed demo quotes or upload a quote first."));
        }
        return ResponseEntity.ok(comparisonService.compare(workflowId, ScoringService.Weights.DEFAULT));
    }

    @PostMapping({"", "/workflows/{workflowId}"})
    public ResponseEntity<ComparisonService.ComparisonResult> compare(@PathVariable(required = false) Long workflowId,
                                                                        @RequestBody(required = false) ScoringWeightsRequest weights) {
        if (workflowId == null) {
            workflowId = workflowRepository.findTopByOrderByCreatedAtDesc()
                    .map(com.procureai.entity.WorkflowExecution::getId)
                    .orElse(null);
        }
        if (workflowId == null) {
            return ResponseEntity.ok(new ComparisonService.ComparisonResult(List.of(), null, "No workflows found. Seed demo quotes or upload a quote first."));
        }
        ScoringService.Weights w = weights == null ? ScoringService.Weights.DEFAULT : ScoringService.Weights.from(weights);
        return ResponseEntity.ok(comparisonService.compare(workflowId, w));
    }
}
