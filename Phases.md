# Phases.md — ProcureAI

## Build Phases

Each phase should be completed, run, and verified before moving to the next. Do not start a later phase with an earlier one broken.

---

### Phase 0 — Repository Inspection & Plan
- Inspect existing project structure, frontend, backend, dependencies, database, env files, config, existing APIs
- Decide what can be reused vs. built fresh
- Confirm folder structure against `Architecture.md`
- **Exit criteria:** A clear implementation plan exists; no code written yet beyond scaffolding.

---

### Phase 1 — Backend Foundation & Auth
- Spring Boot project scaffold, base config, DB connection
- Core entities + migrations: `User`, `Vendor`, `Quote`, `QuoteItem`, `WorkflowExecution`, `AuditLog` (remaining entities added as needed in later phases)
- Authentication (login, JWT/session), password hashing
- Role-based authorization scaffolding (roles: e.g. Buyer, Approver, Admin)
- `/login` page (frontend) wired to real auth
- **Exit criteria:** A user can register/log in, and protected routes correctly reject unauthenticated/unauthorized requests.

---

### Phase 2 — Document Ingestion & AI Extraction
- `DocumentService`: file validation, text extraction, OCR fallback
- `AIProvider` abstraction + `MockAIProvider` (deterministic) + `GeminiProvider`
- `AIService`: structured JSON extraction with schema validation, `MISSING_FIELD` handling, confidence score
- `/quotes/upload` page: drag-and-drop upload, per-vendor processing status (Uploading → Reading → Extracting → Validating → Complete)
- `/quotes/:id` page: view a single extracted quote
- Seed 3 sample vendor quotes (different layouts/terminology) as demo data
- **Exit criteria:** Uploading the 3 demo quote files produces correct, schema-valid structured JSON for each, visible on `/quotes/:id`.

---

### Phase 3 — Normalization & Comparison Engine
- `ComparisonService`: normalize vendor-specific terms into the common internal model, recalculate totals independently (do not trust vendor-stated totals)
- TCO / `effectiveCost` calculation
- `/comparison` page: full comparison table with visual indicators (Good / Above Benchmark / Best Price / Recommended), sortable columns
- **Exit criteria:** All 3 demo vendors appear correctly normalized and compared side-by-side with accurate calculated totals.

---

### Phase 4 — Benchmark & Scoring
- `BenchmarkService` abstraction with curated demo dataset, clearly labeled as reference/demo data
- Benchmark classification: BELOW / WITHIN / ABOVE MARKET with % deviation
- Configurable weighted vendor scoring engine (backend-calculated), with AI-generated natural-language explanation of the score
- AI Insights panel on `/comparison` (concise summary + recommended action)
- **Exit criteria:** Each demo vendor shows a benchmark classification and a backend-computed score with an AI explanation that matches the numbers.

---

### Phase 5 — Negotiation Agent & Rules Engine
- `NegotiationService`: structured negotiation recommendation (`action`, `targetUnitPrice`, `maximumApprovedPrice`, `strategy`, `reason`, `confidence`)
- Negotiation rules configuration (target discount %, max price, min warranty, max delivery days, auto-negotiation toggle) — enforced server-side, not just suggested
- `/negotiations` and `/negotiations/:id` pages
- Negotiation email drafting (AI-generated, editable)
- **Exit criteria:** For each vendor above benchmark, the system proposes a negotiation strategy that never exceeds the configured maximum price.

---

### Phase 6 — Human Approval & Email Simulation
- `/approvals` page: Edit / Approve & Send / Reject flow, RBAC-restricted to Approver role
- `EmailService` abstraction: `MockEmailService` (Vendor Inbox Simulator) as default, `GmailEmailService` optional
- Negotiation timeline view (quote received → extracted → benchmark checked → drafted → approved → sent → vendor responded → evaluated → accepted)
- Multi-round negotiation (min. 2 rounds), automatic stop at max rounds
- **Exit criteria:** A negotiation can be approved, "sent" to the mock inbox, receive a simulated vendor reply, and be automatically re-evaluated by the AI against the configured limits.

---

### Phase 7 — Final Vendor Selection & Purchase Order
- Final vendor decision logic combining scoring + negotiation outcomes, with visible reasoning and alternatives shown
- `PurchaseOrderService`: professional PDF PO generation (PO Number, Date, Buyer, Vendor, Products, Pricing, Tax, Shipping, Total, Warranty, Delivery, Payment Terms, Notes)
- `/purchase-orders` and `/purchase-orders/:id` pages with Download PDF
- RBAC: only authorized roles may finalize/generate a PO
- **Exit criteria:** From the demo data, a final vendor is selected with visible reasoning and a downloadable, correctly populated PDF PO is generated.

---

### Phase 8 — Dashboard & Analytics
- `/dashboard`: Quotes Processed, Negotiations Automated, Estimated Savings, Time Saved, Negotiation Success Rate, Pending Approvals — computed from real workflow execution data (demo-seed clearly labeled)
- `/analytics`: Total Quotes, Avg Processing Time, Avg Savings, Success Rate, Best Performing Vendors, Avg Discount Achieved, Human Intervention Rate, Traditional vs. ProcureAI time comparison
- **Exit criteria:** Running the demo workflow updates dashboard/analytics numbers derived from that actual run.

---

### Phase 9 — Audit Log
- `AuditService`: log every major event (upload, extraction, normalization, benchmark check, scoring, negotiation drafted, approval, email sent, vendor response, re-evaluation, vendor selected, PO generated) with timestamp, user, workflow, event, input, output, status
- `/audit-logs` page with filtering
- **Exit criteria:** A single demo run produces a complete, correctly ordered audit trail from upload to PO.

---

### Phase 10 — Demo Mode & Settings
- "Run Demo Procurement" one-click button executing the full pipeline reliably, no external accounts required
- `/settings`: negotiation rule defaults, scoring weights, provider toggles (mock vs. real AI/email)
- **Exit criteria:** Clicking "Run Demo Procurement" from a clean state completes the entire workflow (upload → PO) without manual intervention beyond the mandatory human-approval steps.

---

### Phase 11 — UI Polish
- Apply `Design.md` visual system consistently across all pages
- Empty states, loading states, skeletons, toast notifications, confirmation dialogs
- Responsive layout pass, subtle Framer Motion transitions
- **Exit criteria:** The app looks and feels like a polished modern B2B SaaS product on desktop and common breakpoints.

---

### Phase 12 — Security & AI Safety Review
- Re-check auth/RBAC on every endpoint, especially approvals and PO generation
- Verify AI outputs cannot bypass negotiation limits or trigger unauthorized actions
- Check for prompt injection handling, IDOR, file upload vulnerabilities, secret leakage
- **Exit criteria:** All items in `Rules.md §6` and `§3` (AI Behavior Rules) verified in code, not just in design.

---

### Phase 13 — Code Quality Pass
- Sweep for `TODO`/`FIXME`/debug logs/hardcoded secrets/duplicate/dead code/fake metrics/broken endpoints per `Rules.md §7`
- Fix everything found
- **Exit criteria:** Clean sweep — nothing on the forbidden list remains.

---

### Phase 14 — Testing
- Backend unit tests (extraction, normalization, calculations, scoring, benchmark, negotiation limits, approval flow, PO generation, authorization)
- Frontend tests for major workflows
- Integration test of the full pipeline
- **Exit criteria:** Test suite passes; core workflow covered by an automated integration test.

---

### Phase 15 — Full Run-Through & Debug
- Start frontend + backend + DB from scratch
- Manually execute: login → upload → extraction → normalization → comparison → benchmark → negotiation → approval → vendor response simulation → final selection → PO generation → analytics → audit logs
- Fix anything broken — do not just report failures
- **Exit criteria:** The entire flow in `PRD.md §9 (Success Criteria)` completes successfully end-to-end, ready to demo.
