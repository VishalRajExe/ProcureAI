# Project Brain v2
### AI Runtime Architecture for Long-Term Vibecoding

## Vision
Project Brain is an AI-native engineering runtime that eliminates repeated codebase analysis by maintaining a continuously synchronized knowledge system.
Instead of forcing the AI to rediscover the project on every session, Project Brain provides persistent architectural memory, graph-based retrieval, deterministic execution workflows, and incremental knowledge synchronization.
The primary objective is to minimize context tokens while improving code quality through structured engineering workflows.

## Core Principles
Project Brain follows several non-negotiable principles:
- Never analyze the entire codebase unless explicitly requested.
- Always retrieve only the required context.
- Every prompt follows the same deterministic execution pipeline.
- Memory is modular instead of monolithic.
- Graph retrieval is the primary source of project context.
- Documentation updates automatically after accepted changes.
- Knowledge grows incrementally with every completed task.

## Project Structure
```
project-brain/
├── system/
│   ├── system.md
│   ├── planner.md
│   └── workflow.md
├── graph/
│   ├── graph.json
│   ├── graph-index.json
│   ├── embeddings.json
│   ├── nodes.json
│   └── edges.json
├── memory/
│   ├── overview.md
│   ├── architecture.md
│   ├── backend.md
│   ├── frontend.md
│   ├── database.md
│   ├── routing.md
│   ├── api.md
│   ├── dependencies.md
│   └── patterns.md
├── tasks/
│   ├── active.md
│   ├── completed.md
│   ├── failed.md
│   └── changelog.md
├── standards/
│   ├── typescript.md
│   ├── react.md
│   ├── nextjs.md
│   ├── security.md
│   ├── performance.md
│   ├── naming.md
│   └── documentation.md
├── reviews/
│   ├── architecture-review.md
│   ├── performance-review.md
│   ├── security-review.md
│   ├── code-quality.md
│   └── documentation-review.md
├── templates/
│   ├── task-template.md
│   ├── review-template.md
│   └── confidence-score-template.md
├── runtime/
│   ├── context-loader.md
│   ├── execution-engine.md
│   ├── graph-retriever.md
│   ├── memory-updater.md
│   ├── graph-updater.md
│   ├── review-engine.md
│   └── task-classifier.md
└── cache/
    ├── recent-context.md
    ├── last-plan.md
    ├── recent-files.md
    └── recent-review.md
```

---

## Phase 1 — System Layer
The System layer defines how the AI thinks, not what the project contains. See `system/system.md`, `system/planner.md`, `system/workflow.md`.

## Phase 2 — Memory Layer
Instead of maintaining one massive memory file, Project Brain divides project knowledge into independent domains (see `memory/`). Each file should ideally remain under 300–500 lines, allowing selective loading instead of injecting thousands of unnecessary tokens into context.

## Phase 3 — Graph Layer
The graph becomes the structural source of truth (see `graph/`). Graphify is responsible for updating these files. The AI must never manually edit graph structures. Graph responsibilities include module relationships, component hierarchy, route mapping, API connections, database relationships, dependency graph, and import/export relationships.

## Phase 4 — Runtime Layer
The Runtime layer acts as the execution engine (see `runtime/`). Each runtime component performs exactly one responsibility.

### Example Runtime Execution
```
User Prompt: "Add Dark Mode"

Task Classifier
↓
Medium Complexity
↓
Graph Retriever
↓
Affected Nodes: Navbar, Theme Context, Settings, Theme Provider
↓
Context Loader
↓
Loads only: frontend.md, architecture.md, Theme Context
↓
Planning
↓
Execution
```

## Phase 5 — Standards Layer
Standards define engineering quality (see `standards/`). Review agents reference standards rather than project memory. This separates implementation knowledge from engineering rules.

## Phase 6 — Review Layer
Each reviewer focuses on one engineering discipline (see `reviews/`).

Example — Security Review:
```
Checklist:
SQL Injection
XSS
Authentication
Authorization
Secret Exposure
Validation
Dependency Risks

Output:
Score: 94/100

Issues Found:
...

Recommendations:
...
```

## Phase 7 — Task History
See `tasks/`. Each completed task appends: Task Description, Goal, Files Changed, Modules Changed, Review Score, Date, Memory Updated, Graph Updated. This provides long-term engineering history.

## Phase 8 — Orchestrator
Every user prompt follows the same deterministic pipeline:
```
User Prompt
↓
Task Classification
↓
Graph Retrieval
↓
Context Loading
↓
Planning
↓
Execution
↓
Static Validation
↓
AI Review
↓
Knowledge Synchronization
↓
Response
```
No stage may be skipped.

## Phase 9 — Graph Retrieval
The graph determines the minimum context required. Instead of "Analyze the project," the runtime performs:
```
Find affected nodes
↓
Resolve dependencies
↓
Return connected files
↓
Return APIs
↓
Return Routes
↓
Return Components
```
Only relevant context enters the model.

## Phase 10 — Planning
Before writing code, the planner produces: Goal, Affected Modules, Execution Order, Estimated Complexity, Risk Analysis, Testing Strategy, Rollback Strategy, Estimated Files. Only after planning is approved does execution begin.

## Phase 11 — Execution
The Execution Engine generates implementation changes.
Responsibilities: Modify code, respect project standards, avoid unrelated refactoring, preserve architecture. No review occurs during execution.

## Phase 12 — Static Validation
Before consuming AI review tokens, deterministic tooling validates the implementation:
```
npm run lint
npm run typecheck
npm run build
tests
```
If any validation fails, execution returns directly to the coding phase and AI review is skipped. This minimizes token consumption.

## Phase 13 — AI Review
AI review only begins after static validation succeeds.
Evaluation areas: Architecture, Maintainability, Readability, Intent Matching, Side Effects, Scalability, Regression Risk. Reviewer outputs structured feedback.

## Phase 14 — Confidence Engine
Every accepted implementation receives quantitative scoring.
```
Architecture     96
Performance      91
Security         98
Naming           95
Documentation    90
Testing          92
Overall          93
```
Acceptance Threshold: Overall ≥ 90. Otherwise: Improve → Review Again. This prevents unnecessary review loops.

## Phase 15 — Knowledge Synchronization
Once a task is accepted, only affected knowledge is updated. Possible updates: overview.md, architecture.md, routing.md, api.md, dependencies.md, task history, graph metadata. Entire documentation is never regenerated — only incremental changes are applied.

## Phase 16 — Incremental Graph Updates
Example — a new AuthService is introduced:
```
AuthService
↓
Middleware
↓
User
↓
API
↓
Database
```
The remainder of the graph remains unchanged.

## Phase 17 — Cache Layer
See `cache/`. Purpose: reduce repeated graph retrieval for sequential prompts.

Example:
```
Prompt 1: "Implement Authentication"
Prompt 2: "Now add Forgot Password"
```
The Runtime loads the recent authentication context directly from cache instead of rebuilding project context.

## Phase 18 — Complete Prompt Lifecycle
```
Receive Prompt
↓
Task Classification
↓
Graph Retrieval
↓
Load Relevant Memory
↓
Planning
↓
Execution
↓
Static Validation
↓
AI Review
↓
Confidence Scoring
↓
Score ≥ 90 ?
├── No
│    ↓
│  Improve
│    ↓
│  Review Again
↓
Yes
↓
Incremental Memory Update
↓
Incremental Graph Update
↓
Append Task History
↓
Return Final Response
```

---

## Design Philosophy
Project Brain is not a prompt. It is an AI engineering runtime.
Its purpose is to transform large language models into persistent software engineers capable of maintaining long-term project knowledge while minimizing context usage.

- Every component has a single responsibility.
- Every workflow is deterministic.
- Every accepted change improves the project's collective knowledge.

Rather than repeatedly rediscovering the codebase, Project Brain continuously evolves alongside it, becoming progressively faster, more context-aware, and more reliable with every engineering session.
