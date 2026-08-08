# Memory.md — ProcureAI

## Purpose

This file tracks project decisions and status continuously so any session has a single source of truth for current state and next steps.

---

## Current Status

- **Phase:** 6-Hour Hackathon Complete Backend Built & Verified
- **Last updated:** 2026-08-08
- **Overall completion:** Full backend workflow operational end-to-end
- **Git Repository:** [https://github.com/VishalRajExe/ProcureAI.git](https://github.com/VishalRajExe/ProcureAI.git)

---

## Completed Work Log

```
[6-Hour Backend Implementation] — Complete REST APIs, entities, services, unit & integration tests, git push —
  - Entities: User, Vendor, Quote, QuoteItem, Negotiation, NegotiationRound, Approval, PurchaseOrder, PurchaseOrderItem, AuditLog, Benchmark, EmailMessage, WorkflowExecution.
  - Auth APIs: POST /api/auth/register, POST /api/auth/login, GET /api/auth/me (JWT BCrypt).
  - Vendor APIs: GET /api/vendors, GET /api/vendors/{id}.
  - Quote & Workflow APIs: GET /api/quotes, GET /api/quotes/{id}, POST /api/quotes, POST /api/quotes/upload, POST /api/quotes/workflows.
  - Comparison API: GET /api/comparison, POST /api/comparison.
  - Scoring Engine: Deterministic weighted scoring (Price 40%, Warranty 20%, Delivery 15%, Payment Terms 10%, Vendor Reliability 15%).
  - Benchmark Engine: BELOW_MARKET, WITHIN_MARKET, ABOVE_MARKET classification against demo reference dataset.
  - AI Layer: Provider-agnostic AIProvider with MockAIProvider returning structured JSON decisions bounded by hard backend limits.
  - Negotiation & Approval Workflow: POST /api/negotiations, POST /api/negotiations/{id}/approve, POST /api/negotiations/{id}/simulate-response, POST /api/negotiations/{id}/evaluate.
  - Purchase Order PDF Generator: Apache PDFBox rendering dynamic PO entity & PDF at GET /api/purchase-orders/{id}/pdf and POST /api/purchase-orders/generate.
  - Audit Log & Dashboard APIs: GET /api/audit-logs, GET /api/dashboard.
  - Demo Seed Mechanism: POST /api/demo/seed and POST /api/demo/run (Dell 50 @ ₹68k, HP 50 @ ₹63.5k, Lenovo 50 @ ₹71k).
  - Tests: ProcureAiWorkflowIntegrationTest, QuoteCalculationServiceTest, ScoringServiceTest all passed.
```

---

## Decisions Made

- Active profile `demo` uses H2 in-memory DB so application starts zero-config instantly, with `mysql` profile toggle for production MySQL database.
- PDF generation uses Apache PDFBox 3.0.3 to create dynamic PO PDFs saved to `po-output/` and streamed over HTTP.

---

## Key File/Path Reference

| Area | Path |
|---|---|
| Main Application | `BACKEND/src/main/java/com/procureai/ProcureAiApplication.java` |
| Security Config | `BACKEND/src/main/java/com/procureai/config/SecurityConfig.java` |
| REST Controllers | `BACKEND/src/main/java/com/procureai/controller/` |
| Core Services | `BACKEND/src/main/java/com/procureai/service/` |
| Tests | `BACKEND/src/test/java/com/procureai/` |
| Docker & Railway | `BACKEND/Dockerfile`, `BACKEND/railway.json` |

---

## Next Step

Frontend UI component integration & wiring to the backend REST endpoints.
