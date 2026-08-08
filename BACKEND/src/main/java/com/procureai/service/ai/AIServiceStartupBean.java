package com.procureai.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Checks FastAPI AI service availability on Spring Boot startup.
 *
 * Behavior:
 *  - If PYTHON_AI_ENABLED=false (default): logs a single INFO line and exits
 *  - If PYTHON_AI_ENABLED=true:
 *      → Polls FastAPI health check up to maxAttempts times with 1s delay
 *      → Logs READY or UNAVAILABLE — never blocks or crashes Spring Boot startup
 *
 * Spring Boot NEVER hangs waiting for FastAPI.
 * FastAPI unavailability is graceful — all AI calls fall back to GeminiAIProvider.
 */
@Component
public class AIServiceStartupBean implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(AIServiceStartupBean.class);

    @Value("${app.python-ai.enabled:false}")
    private boolean enabled;

    @Value("${app.python-ai.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${app.python-ai.startup-check-attempts:3}")
    private int maxAttempts;

    private final PythonAIClient client;

    public AIServiceStartupBean(PythonAIClient client) {
        this.client = client;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!enabled) {
            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║  ProcureAI AI Service: DISABLED (PYTHON_AI_ENABLED=false) ║");
            log.info("║  Using GeminiAIProvider for all AI operations.           ║");
            log.info("║  To enable FastAPI AI, set PYTHON_AI_ENABLED=true        ║");
            log.info("╚══════════════════════════════════════════════════════════╝");
            return;
        }

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  ProcureAI AI Service: CHECKING ({})  ║", baseUrl);
        log.info("╚══════════════════════════════════════════════════════════╝");

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean healthy = client.isHealthy();
                if (healthy) {
                    log.info("╔══════════════════════════════════════════════════════════╗");
                    log.info("║  ✅ FastAPI AI Service: READY at {}  ║", baseUrl);
                    log.info("║  Enhanced AI features: vendor evaluation, market intel,  ║");
                    log.info("║  Defensive/Balanced/Aggressive negotiation strategy.     ║");
                    log.info("╚══════════════════════════════════════════════════════════╝");
                    return;
                }
            } catch (Exception e) {
                log.debug("FastAPI health attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.warn("╔══════════════════════════════════════════════════════════╗");
        log.warn("║  ⚠️  FastAPI AI Service: UNAVAILABLE at {}  ║", baseUrl);
        log.warn("║  Spring Boot continues with GeminiAIProvider fallback.   ║");
        log.warn("║  Start FastAPI: cd AI-SERVICE && uvicorn main:app --reload ║");
        log.warn("╚══════════════════════════════════════════════════════════╝");
    }
}
