# Rules.md — ProcureAI

## Boundaries and Rules for the AI Coding Agent

These rules govern how the AI (acting as architect/engineer) should build, and how the *product's own AI features* must behave at runtime. Follow both sections strictly.

---

## 1. General Engineering Rules

1. **Inspect before building.** Always check existing project structure, dependencies, and config before writing new code. Reuse what already exists rather than duplicating it.
2. **No giant services.** Keep each backend service focused on one responsibility (see `Architecture.md §3.1`). If a service grows unwieldy, split it.
3. **Controllers stay thin.** No business logic in controllers — only request validation and delegation to services.
4. **Never expose JPA entities directly** through REST APIs. Always map to DTOs.
5. **One amazing end-to-end workflow beats ten shallow features.** Do not over-engineer with unnecessary microservices, Kubernetes, distributed queues, or a large swarm of AI agents. This is a focused hackathon build.
6. **Do not stop at "extraction only."** A version of this project that just shows "upload PDF → extracted table" and nothing else is considered incomplete. The negotiation and decision loop is mandatory.
7. **Finish what you start.** No half-implemented features, no dead/unreachable code paths, no stubbed endpoints left returning placeholder data in the final build.
8. **Run and verify.** After implementation, actually start the frontend and backend and run the complete demo workflow. If something fails, debug and fix it — do not just report the failure and stop.

---

## 2. Libraries & Stack Rules

- **Backend:** Java 17+, Spring Boot 3+, Spring Security, Spring Data JPA/Hibernate, Bean Validation, Maven, PostgreSQL/MySQL. Do not introduce a second backend framework or language.
- **Frontend:** React + Vite + TypeScript + Tailwind CSS + React Router + Axios + Recharts + Framer Motion. Avoid pulling in a second competing UI/component framework or a second charting library.
- **AI:** All AI calls must go through the `AIProvider` abstraction. Never call an LLM SDK directly from a controller or from frontend code.
- **PDF:** Use a single, well-supported PDF library for PO generation — do not mix multiple PDF libraries.
- Avoid adding new dependencies for functionality that can be reasonably hand-rolled in a few lines, especially for a hackathon-scoped codebase.

---

## 3. AI Behavior Rules (Runtime)

1. **Structured output only.** The AI must always return structured JSON matching a defined backend schema — never free-form text used directly to drive logic.
2. **Explicit missing data.** If the AI cannot confidently extract a field, it must return `MISSING_FIELD` for that field, not a guess.
3. **Never trust AI output blindly.** Every AI response is validated against a backend schema before it can affect application state. Invalid responses are rejected and surfaced as `NEEDS_REVIEW`, not silently accepted.
4. **AI does not compute final scores.** Vendor scoring is a deterministic backend calculation using user-configured weights. The AI may only *explain* the score in natural language — it cannot assign or override numeric scores.
5. **AI does not decide market benchmark status.** Classification into BELOW/WITHIN/ABOVE MARKET is a backend calculation against the `BenchmarkService` data — the AI narrates, it does not decide.
6. **Hard negotiation limits are enforced in backend code, not by the AI.** If a negotiation rule specifies a maximum approved price, the AI can never recommend, accept, or send anything above that price — the backend must reject or clamp any such AI output before it reaches the UI or an outgoing email.
7. **No autonomous external actions.** The AI must never directly send emails, generate a final PO, or approve a negotiation. Every such action requires a specific backend-executed step gated by human approval.
8. **Allowed Action Registry only.** The AI's decisions are limited to a fixed vocabulary: `NEGOTIATE`, `ACCEPT`, `REJECT`, `REQUEST_CLARIFICATION`. Any other action string must be rejected by the backend.
9. **No hallucinated prices or facts.** If extraction confidence is low or a benchmark/comparable is unavailable, the AI must say so rather than inventing a number.
10. **Mock provider parity.** `MockAIProvider` must produce realistic, deterministic responses that exercise the *same* schema and code paths as the real provider, so the demo works identically without an API key.
11. **Demo/benchmark data must be clearly labeled** as such in both API responses and UI — never presented as live market data.

---

## 4. Human-in-the-Loop Rules (Non-Negotiable)

- Negotiation emails are **never sent automatically**. They must pass through Edit / Approve & Send / Reject.
- Final vendor selection and Purchase Order generation require an authorized human action, not an autonomous AI trigger.
- Only users with the correct role/permission may approve negotiations or generate a final PO (RBAC-enforced, not just UI-hidden).

---

## 5. Error Handling Rules

- Every workflow entity has an explicit status: `PROCESSING`, `COMPLETED`, `NEEDS_REVIEW`, `FAILED`, `WAITING_APPROVAL`. Never leave a record in an ambiguous/undefined state.
- Handle and gracefully surface (not crash on): invalid PDFs, OCR failures, AI provider failures/timeouts, missing quote fields, invalid/negative prices, vendor response timeouts, email send failures, PO generation failures, database failures.
- Never fail silently — every failure must be visible to the user and written to the audit log.

---

## 6. Security Rules

- Authentication + role-based authorization on every protected endpoint.
- Passwords hashed (never stored/logged in plaintext); JWT/session handled securely (httpOnly cookies or properly scoped storage).
- Validate and size-limit all file uploads; reject unexpected file types.
- Validate all API inputs server-side (never trust client-side validation alone).
- No secrets, API keys, or credentials committed to source control — environment variables only.
- Guard explicitly against: prompt injection into the AI layer, IDOR (users accessing other tenants'/users' quotes, negotiations, or POs), unauthorized approval or PO-generation calls.
- Return safe, non-leaky error messages to the client; log full detail server-side only.

---

## 7. Code Quality Rules

Before considering any phase or the project complete, search for and eliminate:
```
TODO / FIXME
console.log / System.out.println (debug leftovers)
hardcoded secrets
duplicate code
unused imports
dead code
fake/hardcoded metrics presented as real
broken or stubbed endpoints
temporary hacks
```

- Write unit tests for: extraction, normalization, TCO/score calculations, benchmark classification, negotiation limit enforcement, approval flow, PO generation, authorization.
- Write integration tests for the full pipeline: Upload → Process → Compare → Negotiate → Approve → Finalize → Generate PO.

---

## 8. What the AI Coding Agent Should NOT Do

- Do not ask unnecessary clarifying questions — make sensible, documented engineering decisions and proceed.
- Do not build features not requested (extra integrations, extra agents, workflow builders) at the expense of finishing the core loop.
- Do not present mocked/demo data anywhere as if it were real production data.
- Do not let the AI provider's failure or absence (no API key) break the app — always degrade gracefully to `MockAIProvider`.
- Do not consider the project done until the full demo — quote upload through PO generation — has actually been run and verified successfully.

---

## 9. Priority Order (When Trade-offs Are Needed)

```
Reliability → End-to-End Functionality → AI Intelligence → Security → UI Polish → Extra Features
```
