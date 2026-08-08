# Memory.md — ProcureAI

## Purpose

This file tracks project decisions and status continuously so any session has a single source of truth for current state and next steps.

---

## Current Status

- **Phase:** Phase 0 & Phase 1 Initial Setup Complete
- **Last updated:** 2026-08-08
- **Overall completion:** 1 of 15 phases completed
- **Git Repository:** [https://github.com/VishalRajExe/ProcureAI.git](https://github.com/VishalRajExe/ProcureAI.git)

---

## Completed Work Log

```
[Phase 0 & 1 Setup] — Initial scaffolding & repository push —
  - Scaffolded React + Vite + TypeScript frontend in FRONTEND/ with Tailwind CSS, Lucide icons, Recharts, Framer Motion, Axios, React Router, and Firebase SDK (firebase).
  - Scaffolded Java 17 + Spring Boot 3 backend in BACKEND/ with MySQL driver, Spring Security, JPA, Dockerfile, and Railway.json deployment configuration.
  - Executed graphify indexing on 137 documentation files generating graphify-out/graph.json & GRAPH_REPORT.md.
  - Pushed initial project setup to https://github.com/VishalRajExe/ProcureAI.git.
```

---

## Decisions Made

- Tech stack confirmed:
  - Frontend: React + Vite + TypeScript + Tailwind CSS + Firebase SDK.
  - Backend: Java 17 + Spring Boot 3 + MySQL + Spring Security + Spring Data JPA.
  - Deployment: Docker containerized deployment prepared for Railway.
- Document graph generated via `graphify` and saved to `graphify-out/`.

---

## Key File/Path Reference

| Area | Path |
|---|---|
| Frontend app & Firebase config | `FRONTEND/src/App.tsx`, `FRONTEND/src/config/firebase.ts` |
| Backend entrypoint & config | `BACKEND/src/main/java/com/procureai/ProcureAiApplication.java`, `BACKEND/src/main/resources/application.yml` |
| Docker & Railway setup | `BACKEND/Dockerfile`, `BACKEND/railway.json` |
| Knowledge Graph | `graphify-out/graph.json`, `graphify-out/GRAPH_REPORT.md` |
| Core docs | `PRD.md`, `Architecture.md`, `Rules.md`, `Phases.md`, `Design.md` |

---

## Next Step

Begin Phase 1 authentication & base entity implementations (User, Vendor, Quote, QuoteItem, WorkflowExecution, AuditLog) in the Spring Boot backend and wire up React Auth/Login views.
