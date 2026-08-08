package com.procureai.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Broadcasts real-time workflow progress updates over WebSocket/STOMP.
 *
 * Frontend subscribes to /topic/workflow/{workflowId}/progress and receives
 * structured JSON progress events as each workflow stage completes.
 *
 * Adapted from the quotation-agent reference project's WebSocket streaming
 * pattern, re-implemented using Spring STOMP instead of FastAPI WebSocket.
 */
@Service
public class WorkflowProgressService {

    private final SimpMessagingTemplate messagingTemplate;

    public WorkflowProgressService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public enum Stage {
        WORKFLOW_CREATED("Workflow Created", 5),
        QUOTE_UPLOADING("Uploading Quotes", 10),
        QUOTE_EXTRACTING("Extracting & Normalizing", 25),
        QUOTE_VALIDATED("Quotes Validated", 35),
        BENCHMARKING("Benchmarking Against Market", 45),
        COMPARING("Comparing Vendors", 55),
        AI_RECOMMENDATION("AI Recommendation Ready", 65),
        NEGOTIATION_DRAFTED("Negotiation Draft Created", 72),
        AWAITING_APPROVAL("Awaiting Human Approval", 80),
        NEGOTIATION_SENT("Negotiation Email Sent", 85),
        VENDOR_RESPONDED("Vendor Response Received", 90),
        AI_EVALUATION("AI Re-Evaluation Complete", 93),
        VENDOR_SELECTED("Best Vendor Selected", 96),
        PO_GENERATED("Purchase Order Generated", 100),
        COMPLETED("Workflow Complete", 100),
        FAILED("Workflow Failed", -1);

        public final String label;
        public final int progressPct;

        Stage(String label, int progressPct) {
            this.label = label;
            this.progressPct = progressPct;
        }
    }

    /**
     * Broadcast a progress event for a workflow stage.
     * Frontend receives: { stage, label, progress, message, timestamp }
     */
    public void broadcast(Long workflowId, Stage stage, String message) {
        String destination = "/topic/workflow/" + workflowId + "/progress";
        Map<String, Object> event = Map.of(
                "workflowId", workflowId,
                "stage", stage.name(),
                "label", stage.label,
                "progress", stage.progressPct,
                "message", message != null ? message : stage.label,
                "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSend(destination, event);
    }
}
