# Memory.md — ProcureAI

## Purpose

This file is **not filled in at project start.** It is updated continuously *while* the AI is coding, so that:

- A new chat/session/tool can resume work without re-reading the entire codebase
- The AI doesn't waste tokens rediscovering decisions already made
- The AI doesn't guess or re-invent things that were already decided or built
- There is a single source of truth for "where are we right now"

**Update this file at the end of every work session and after completing any phase from `Phases.md`.**

---

## How to Update This File

1. Update **Current Status** with the phase/step just completed.
2. Add/update entries in **Completed Work Log**.
3. Update **Decisions Made** if any new architectural/design/naming decision was made that isn't already in `Architecture.md` / `Rules.md` / `Design.md`.
4. Update **Known Issues / TODO** with anything left broken, deferred, or intentionally skipped.
5. Update **Next Step** so the very next session knows exactly where to resume.
6. Keep entries short and factual — this is a status log, not a narrative.

---

## Current Status

- **Phase:** _(e.g., "Phase 2 — Document Ingestion & AI Extraction")_
- **Last updated:** _(date)_
- **Overall completion:** _(e.g., "3 of 15 phases complete")_

---

## Completed Work Log

> Append new entries at the top. Format: `[Phase] — what was built — key files touched`

```
Example:
[Phase 1] — Auth + base entities scaffolded — backend/src/main/java/com/procureai/security/*,
            entity/User.java, entity/Vendor.java, entity/Quote.java. Login page wired at
            frontend/src/pages/Login.tsx. JWT auth confirmed working via manual test.
```

---

## Decisions Made (Not Yet in Core Docs)

> Record any decision made during implementation that deviates from or adds detail beyond
> `PRD.md` / `Architecture.md` / `Rules.md` / `Design.md`. If a decision changes those docs,
> update the relevant doc directly and just note it here briefly.

```
Example:
- Chose PostgreSQL over MySQL for JSON column support (quote raw-extraction storage).
- MockAIProvider extraction responses are stored as static JSON fixtures in
  backend/src/main/resources/mock-ai/ rather than generated at runtime, for determinism.
```

---

## Known Issues / TODO

> Anything incomplete, deferred, or intentionally out of scope for now. Reference the phase
> it belongs to so it isn't lost.

```
Example:
- [Phase 4] Benchmark dataset only covers the 3 demo laptop models — needs expansion if
  more product categories are demoed.
- [Phase 6] GmailEmailService not implemented — MockEmailService only, per Rules.md (this is
  intentional for v1, not a bug).
```

---

## Key File/Path Reference

> Quick pointers so a new session doesn't have to search the whole repo.

| Area | Path |
|---|---|
| AI provider abstraction | `backend/.../service/ai/` |
| Negotiation rule enforcement | `backend/.../service/negotiation/` |
| Benchmark demo dataset | `backend/.../service/benchmark/` (or resources, once decided) |
| Comparison table UI | `frontend/src/pages/Comparison.tsx` (or actual path once created) |
| Demo data (sample vendor quotes) | `demo-data/` |
| Core docs | `docs/PRD.md`, `docs/Architecture.md`, `docs/Rules.md`, `docs/Phases.md`, `docs/Design.md` |

---

## Next Step

> The single most important thing to do when work resumes.

_(e.g., "Implement OCR fallback in DocumentService for scanned PDFs — text extraction path is done, OCR path is stubbed with a TODO in DocumentService.java line ~80.")_
