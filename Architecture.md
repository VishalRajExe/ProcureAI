# Architecture.md — ProcureAI

## System & Technical Architecture

---

## 1. Tech Stack

### Backend
- Java 17+
- Spring Boot 3+
- Spring Security (JWT/session-based auth, RBAC)
- Spring Data JPA + Hibernate
- Bean Validation (JSR-380)
- Maven
- PostgreSQL (or MySQL)

### Frontend
- React + Vite + TypeScript
- Tailwind CSS
- React Router
- Axios
- Recharts (analytics/dashboard charts)
- Framer Motion (subtle animation only)

### AI Layer
- Provider-agnostic `AIProvider` interface
- `GeminiProvider` (real LLM) + `MockAIProvider` (deterministic offline demo responses)

### Documents
- PDF generation library for Purchase Orders (backend)
- OCR fallback library for scanned quote images/PDFs

---

## 2. High-Level Workflow

```
┌───────────────┐
│ Vendor Quotes │
└───────┬───────┘
        ↓
┌────────────────┐
│ OCR / AI Read  │
└───────┬────────┘
        ↓
┌────────────────┐
│ Extract Data   │
└───────┬────────┘
        ↓
┌────────────────┐
│ Normalize      │
└───────┬────────┘
        ↓
┌────────────────┐
│ Compare Quotes │
└───────┬────────┘
        ↓
┌────────────────┐
│ Market Check   │
└───────┬────────┘
        ↓
┌────────────────┐
│ AI Negotiation │
└───────┬────────┘
        ↓
┌────────────────┐
│ Human Approval │
└───────┬────────┘
        ↓
┌────────────────┐
│ Vendor Reply   │
└───────┬────────┘
        ↓
┌────────────────┐
│ AI Re-evaluate │
└───────┬────────┘
        ↓
┌────────────────┐
│ Select Vendor  │
└───────┬────────┘
        ↓
┌────────────────┐
│ Generate PO    │
└───────┬────────┘
        ↓
┌────────────────┐
│ Audit Complete │
└────────────────┘
```

---

## 3. Backend Layered Architecture

```
Controller
   ↓
Service
   ↓
Domain Logic
   ↓
Repository
```

Rules:
- Controllers are thin — validation + delegation only, no business logic
- Services own business logic and orchestration; keep each service focused (no giant "God services")
- Repositories are Spring Data JPA interfaces — no business logic
- Entities are never exposed directly through the API — always map to/from DTOs

### 3.1 Core Services

| Service | Responsibility |
|---|---|
| `AIService` | Wraps `AIProvider`; sends prompts, parses/validates structured JSON responses |
| `DocumentService` | File validation, text extraction, OCR fallback orchestration |
| `QuoteService` | Persists and retrieves quotes/quote items |
| `ComparisonService` | Normalization + TCO calculation + comparison table assembly |
| `BenchmarkService` | Market benchmark classification (demo dataset abstraction) |
| `NegotiationService` | Negotiation strategy generation, round tracking, rule enforcement |
| `EmailService` | `MockEmailService` (default) / `GmailEmailService` (optional real integration) |
| `PurchaseOrderService` | PO generation (PDF) and persistence |
| `AuditService` | Central audit event logging for every workflow step |

### 3.2 AI Safety Architecture

```
LLM
 ↓
Structured Decision (typed JSON, schema-validated)
 ↓
Validation (backend schema + business-rule checks)
 ↓
Allowed Action Registry (NEGOTIATE / ACCEPT / REJECT / REQUEST_CLARIFICATION)
 ↓
Backend Executor (enforces hard limits, e.g. maxApprovedPrice)
```

- The AI **never** directly triggers side effects (sending email, creating a PO, approving anything).
- Every AI output is validated against a backend schema before it can influence UI or state.
- Negotiation limits (target price, max price, min warranty, max delivery) are enforced in backend code — the AI's role is confined to producing a *recommendation* within those bounds; if it proposes a value outside the bounds, the backend clamps/rejects it.

---

## 4. Repository / Folder Structure

```
procureai/
├── backend/
│   ├── src/main/java/com/procureai/
│   │   ├── config/                 # Security, CORS, JWT, AI provider config
│   │   ├── controller/             # REST controllers (thin)
│   │   ├── dto/                    # Request/response DTOs
│   │   ├── entity/                 # JPA entities
│   │   ├── repository/             # Spring Data JPA repositories
│   │   ├── service/
│   │   │   ├── ai/                 # AIProvider, GeminiProvider, MockAIProvider
│   │   │   ├── document/           # DocumentService, OCR helpers
│   │   │   ├── quote/              # QuoteService
│   │   │   ├── comparison/         # ComparisonService, normalization logic
│   │   │   ├── benchmark/          # BenchmarkService
│   │   │   ├── negotiation/        # NegotiationService, rule engine
│   │   │   ├── email/              # EmailService, MockEmailService
│   │   │   ├── po/                 # PurchaseOrderService, PDF generation
│   │   │   └── audit/              # AuditService
│   │   ├── security/                # JWT filter, RBAC annotations
│   │   ├── exception/               # Global exception handling
│   │   └── ProcureAiApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/            # Flyway/Liquibase migrations
│   ├── src/test/java/...            # Unit + integration tests
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── pages/                   # One file/folder per route (see Pages list)
│   │   ├── components/              # Reusable UI components
│   │   ├── features/                 # Feature-scoped logic (quotes, negotiations, po, analytics)
│   │   ├── api/                     # Axios client + endpoint wrappers
│   │   ├── hooks/
│   │   ├── context/                 # Auth context, etc.
│   │   ├── types/                   # Shared TypeScript types
│   │   ├── utils/
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── index.html
│   ├── tailwind.config.ts
│   ├── vite.config.ts
│   └── package.json
│
├── docs/
│   ├── PRD.md
│   ├── Architecture.md
│   ├── Rules.md
│   ├── Phases.md
│   ├── Design.md
│   └── Memory.md
│
├── demo-data/                        # Sample vendor quote PDFs/images/JSON
└── README.md
```

---

## 5. Data Model (Core Entities)

```
User
Vendor
Quote
QuoteItem
Benchmark
Negotiation
NegotiationRound
Approval
PurchaseOrder
PurchaseOrderItem
WorkflowExecution
AuditLog
EmailMessage
```

Key relationships:
- `Quote` 1—N `QuoteItem`
- `Quote` N—1 `Vendor`
- `Negotiation` 1—N `NegotiationRound`
- `Negotiation` 1—1 `Approval` (per round or per negotiation, per rules)
- `PurchaseOrder` 1—N `PurchaseOrderItem`, N—1 `Vendor`
- `WorkflowExecution` 1—N `AuditLog` (ties every event to a single end-to-end run)
- `EmailMessage` N—1 `Negotiation`

---

## 6. Frontend Routes

```
/login
/dashboard
/quotes
/quotes/upload
/quotes/:id
/comparison
/negotiations
/negotiations/:id
/approvals
/purchase-orders
/purchase-orders/:id
/analytics
/audit-logs
/settings
```

---

## 7. Document Ingestion Pipeline

```
Upload → File Validation → Text Extraction → OCR (if needed)
       → AI Document Understanding → Structured JSON → Schema Validation → Database
```

- File validation: type, size limit, malware/format sanity checks
- Text extraction attempted first; OCR is a fallback for scanned/image documents
- AI understanding produces the structured extraction JSON (see `PRD.md §6.2`)
- Nothing reaches the database without passing schema validation

---

## 8. Environment & Configuration

- All secrets (DB credentials, AI API keys, JWT secret) via environment variables — never committed
- Application must run fully in **demo/offline mode** with `MockAIProvider` + `MockEmailService` when no external keys are configured
- Config toggles: `ai.provider=mock|gemini`, `email.provider=mock|gmail`

---

## 9. Async / Performance Considerations

- Document extraction and AI calls run asynchronously where they could block the UI (upload → processing status polling or WebSocket/SSE updates)
- Avoid N+1 queries in comparison/analytics endpoints — use fetch joins or projection DTOs
- Deduplicate AI calls per quote (don't re-extract on every page view — cache structured result)
