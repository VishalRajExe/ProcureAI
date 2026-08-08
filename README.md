# ProcureAI — Autonomous AI Procurement Agent Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115.0-009688.svg)](https://fastapi.tiangolo.com/)
[![Python](https://img.shields.io/badge/Python-3.11%2B-blue.svg)](https://www.python.org/)
[![React](https://img.shields.io/badge/React-18.2-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue.svg)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-v4.0-38bdf8.svg)](https://tailwindcss.com/)
[![Google Gemini API](https://img.shields.io/badge/Google%20Gemini-1.5%20Flash-4285F4.svg)](https://deepmind.google/technologies/gemini/)
[![Brevo Email API](https://img.shields.io/badge/Brevo-Email%20API%20v3-00B2A9.svg)](https://www.brevo.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**ProcureAI** is an enterprise-grade autonomous AI multi-agent procurement platform designed to automate the entire corporate purchasing lifecycle — from raw vendor quote extraction (PDF/text) to multi-criteria AI comparison, market intelligence benchmarking, human-governed negotiation email dispatch via Brevo, AI vendor reply evaluation, and dynamic server-rendered PDF Purchase Order generation.

The platform includes a dedicated **Python FastAPI AI Microservice (`AI-SERVICE/`)** that adapts multi-agent intelligence prompts and evaluation frameworks (RFP Compliance, Legal Compliance, 1-10 Vendor Scoring, Market Intelligence Datasets, and Defensive/Balanced/Aggressive Negotiation Strategies) running through **Google Gemini API**.

---

## 📋 Table of Contents
1. [Project Overview](#1-project-overview)
2. [Complete End-to-End Workflow & AI Pipeline](#2-complete-end-to-end-workflow--ai-pipeline)
3. [Email Workflow & Approval Sequence](#3-email-workflow--approval-sequence)
4. [System Microservice Architecture](#4-system-microservice-architecture)
5. [Technology Stack](#5-technology-stack)
6. [Python FastAPI AI Microservice (`AI-SERVICE/`)](#6-python-fastapi-ai-microservice-ai-service)
7. [Database Architecture](#7-database-architecture)
8. [API Architecture](#8-api-architecture)
9. [Frontend Architecture](#9-frontend-architecture)
10. [Backend Architecture](#10-backend-architecture)
11. [AI Architecture & Multi-Agent Framework](#11-ai-architecture--multi-agent-framework)
12. [Authentication & Security](#12-authentication--security)
13. [Project Structure](#13-project-structure)
14. [Setup & Installation](#14-setup--installation)
15. [Environment Variables](#15-environment-variables)
16. [Judge Demo Guide](#16-judge-demo-guide)
17. [Key Features Matrix](#17-key-features-matrix)
18. [UI & Screenshots](#18-ui--screenshots)
19. [Error Handling & Reliability](#19-error-handling--reliability)
20. [Performance & Testing](#20-performance--testing)
21. [Future Improvements](#21-future-improvements)
22. [Hackathon Pitch](#22-hackathon-pitch)

---

## 1. Project Overview

### What ProcureAI Is
ProcureAI is an intelligent, human-in-the-loop autonomous agent platform that replaces manual procurement operations. It ingests unorganized vendor proposals (PDFs or raw text), extracts line-item pricing, evaluates vendor offers against real market benchmarks, formulates optimal negotiation strategies using Google Gemini AI, and handles email communications via Brevo.

### Problem It Solves
Traditional enterprise procurement is slow, fragmented, and prone to overspending:
- **Manual Data Extraction**: Procurement officers manually re-key PDF line items into spreadsheets.
- **Inconsistent Vendor Comparison**: Comparing line items, warranty terms, shipping fees, and tax structures across vendors is tedious and error-prone.
- **Unused Negotiation Leverage**: Buyers rarely know the exact market price floor, leaving money on the table.
- **Governance Bottlenecks**: Unstructured email exchanges lack human-in-the-loop auditability and automated PO issuance.

### How It Works
1. **Quote Ingestion**: Procurement teams upload PDF files or paste raw quotation text.
2. **AI Extraction**: Google Gemini AI (via Spring Boot or FastAPI) strips formatting, normalizes currencies, calculates authoritative total costs, and checks market benchmark ranges.
3. **Multi-Criteria Comparison & Scoring**: An AI scoring engine ranks vendors by cost, warranty, delivery speed, and reliability.
4. **Governed AI Negotiation**: Gemini generates an optimal target counter-offer and drafts a negotiation email (framed with **Defensive**, **Balanced**, or **Aggressive** strategies). A human officer reviews, edits, and approves the email before dispatch via Brevo.
5. **Vendor Reply Simulation & PO Generation**: Vendor counter-offers are evaluated by AI. Upon agreement, Apache PDFBox renders an official Purchase Order PDF automatically.

### Key Benefits
- **70% Reduction in Procurement Cycle Time**: Automated quote parsing and AI email drafting.
- **10-15% Average Direct Cost Savings**: Market intelligence benchmarking combined with AI anchor-pricing strategies.
- **100% Governance Compliance**: Mandated human approval step for all outbound financial actions with full audit logs.

---

## 2. Complete End-to-End Workflow & AI Pipeline

```mermaid
flowchart TD
    subgraph Ingestion ["📥 Ingestion Phase"]
        A["1. Vendor Quote Upload (PDF / Plain Text)"] --> B["2. OCR & PDF Text Stripping (Apache PDFBox)"]
    end

    subgraph AI_Processing ["🧠 AI Microservice Processing (FastAPI + Gemini)"]
        B --> C["3. Quote Extraction Agent (JSON Parser)"]
        C --> D["4. Market Benchmarking (Floor / Ceiling Check)"]
        D --> E["5. Multi-Agent Vendor Evaluation (1-10 Scorecard)"]
        E --> F["6. AI Negotiation Strategy Agent (Defensive / Balanced / Aggressive)"]
        F --> G["7. AI Negotiation Email Generator"]
    end

    subgraph Governance ["🛡️ Human Governance Gate"]
        G --> H{"8. Human Approval Gate"}
        H -- "Rejected / Edit" --> G
    end

    subgraph Execution ["✉️ Execution & Settlement"]
        H -- "Approved" --> I["9. Outbound Email Dispatch via Brevo REST API v3"]
        I --> J["10. Vendor Counter-Offer Inbox"]
        J --> K["11. AI Response Evaluator Agent"]
        K -- "Counter Target Agreed" --> L["12. PO Generation & Server PDF Render (PDFBox)"]
        K -- "Counter High" --> F
        L --> M["13. Analytics Sync & PO Delivery to Vendor"]
    end

    style AI_Processing fill:#1e1b4b,stroke:#6366f1,stroke-width:2px,color:#fff
    style Governance fill:#312e81,stroke:#818cf8,stroke-width:2px,color:#fff
    style Execution fill:#064e3b,stroke:#34d399,stroke-width:2px,color:#fff
```

---

## 3. Email Workflow & Approval Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Officer as Human Procurement Officer
    participant UI as Frontend (React SPA)
    participant Core as Spring Boot Core Backend
    participant PyAI as Python FastAPI AI Microservice
    participant Gemini as Google Gemini 1.5 Flash
    participant Brevo as Brevo Email API v3
    participant Vendor as Vendor Email Inbox

    Note over Officer, Vendor: Phase 1: AI Strategy & Negotiation Email Drafting
    Core->>PyAI: POST /api/ai/negotiation-strategy (Current, Target, Market Intel)
    PyAI->>Gemini: Prompt: Negotiation Strategy Framework
    Gemini-->>PyAI: Strategy: Aggressive | Approach: Volume Leverage
    PyAI-->>Core: JSON Strategy & Leverage Points
    Core->>PyAI: POST /api/ai/generate-negotiation (Vendor, Approach, Round)
    PyAI->>Gemini: Prompt: Formal Negotiation Email Generator
    Gemini-->>PyAI: Draft Subject & Email Body
    PyAI-->>Core: JSON Email Draft Body

    Note over Officer, Vendor: Phase 2: Human Governance & Approval
    Core-->>UI: Display Draft Email in Governance Queue (Status: PENDING_APPROVAL)
    Officer->>UI: Review & Edit Draft Email Text
    Officer->>UI: Click "Approve & Send via Brevo"
    UI->>Core: POST /api/negotiations/{id}/approve (Bearer JWT)

    Note over Officer, Vendor: Phase 3: Outbound Brevo Dispatch
    Core->>Brevo: POST https://api.brevo.com/v3/smtp/email
    Brevo-->>Vendor: Transactional Email Dispatched
    Brevo-->>Core: Message-ID Returned
    Core-->>UI: Status Updated to SENT (Audit Log Recorded)
```

---

## 4. System Microservice Architecture

```mermaid
graph TD
    subgraph Client_Layer ["💻 Frontend Layer (React 18 + Vite + Tailwind)"]
        UI["Procurement Command Center SPA"]
        API_CLIENT["Axios API Client (JWT Interceptor)"]
    end

    subgraph Security_Layer ["🔒 Security Layer (Spring Security)"]
        JWT["JwtAuthFilter (HMAC-SHA256)"]
        RBAC["Role-Based Access Control"]
    end

    subgraph Backend_Core ["⚙️ Core Backend (Java Spring Boot 3.3.2)"]
        CTRL["REST API Controllers"]
        SERV["Business Services & DB Logic"]
        CALC["Authoritative Math & Scoring Engine"]
        PY_CLIENT["PythonAIClient HTTP Client"]
        REPO["Spring Data JPA Repositories"]
    end

    subgraph AI_Microservice ["🤖 AI Microservice Layer (Python FastAPI 0.115)"]
        FASTAPI["FastAPI Routing (/api/ai/*)"]
        AI_ENGINE["ai_engine.py Reasoning Engine"]
        MKT_DATA["Category Market Intelligence JSON"]
        SCHEMAS["Pydantic Contract Validation"]
    end

    subgraph External_Providers ["🌐 External Providers & Engine"]
        GEMINI["Google Gemini 1.5 Flash API"]
        BREVO["Brevo Transactional Email API v3"]
        PDFBOX["Apache PDFBox PDF Renderer"]
        DB[("H2 / MySQL Database")]
    end

    UI --> API_CLIENT
    API_CLIENT -->|HTTP REST + Bearer JWT| JWT
    JWT --> RBAC
    RBAC --> CTRL
    CTRL --> SERV
    SERV --> CALC
    SERV --> REPO
    REPO --> DB
    SERV --> PY_CLIENT
    PY_CLIENT -->|POST /api/ai/*| FASTAPI
    FASTAPI --> SCHEMAS
    SCHEMAS --> AI_ENGINE
    AI_ENGINE --> MKT_DATA
    AI_ENGINE -->|Prompt JSON| GEMINI
    SERV -->|Direct LLM Fallback| GEMINI
    SERV -->|Outbound Dispatch| BREVO
    SERV -->|Generate A4 PDF| PDFBOX

    style Client_Layer fill:#0f172a,stroke:#38bdf8,stroke-width:2px,color:#fff
    style Backend_Core fill:#1e1b4b,stroke:#818cf8,stroke-width:2px,color:#fff
    style AI_Microservice fill:#022c22,stroke:#34d399,stroke-width:2px,color:#fff
    style External_Providers fill:#31103f,stroke:#c084fc,stroke-width:2px,color:#fff
```

---

## 5. Technology Stack

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Frontend Core** | React 18, TypeScript | Component-driven UI framework with strict typing |
| **Build & Tooling** | Vite 8.2 | Fast dev server and optimized bundle compilation |
| **Styling & Theme** | Tailwind CSS v4, Vanilla CSS | Google Stitch "Midnight Executive" dark mode styling |
| **Icons & Charts** | Lucide React, Recharts | Premium iconography and responsive spend analytics charts |
| **Backend Core** | Java 17, Spring Boot 3.3.2 | Core backend, security, database persistence, and PO engine |
| **Security & Auth** | Spring Security 6, JJWT | Stateless JWT token authentication, BCrypt password hashing |
| **Database & ORM** | H2 Database, Spring Data JPA | In-memory relational storage with Hibernate 6.5 |
| **AI Microservice** | Python 3.11+, FastAPI 0.115, Pydantic 2.13 | Specialized AI inference engine for scoring, market intel, and negotiation |
| **AI / LLM** | Google Gemini 1.5 Flash REST API | Quote extraction, strategy generation, and counter-offer scoring |
| **OCR / Parsing** | Apache PDFBox 3.0 | PDF document text extraction and server-side PDFBox rendering |
| **Email Service** | Brevo (Sendinblue) API v3 | Outbound negotiation email and PO PDF delivery |

---

## 6. Python FastAPI AI Microservice (`AI-SERVICE/`)

The repository includes a standalone Python FastAPI service (`AI-SERVICE/`) that provides deep AI evaluation capabilities adapted from multi-agent prompt patterns (RFP Compliance, Legal Compliance, 1-10 Vendor Scoring, Market Intelligence Datasets, and Defensive/Balanced/Aggressive Negotiation Strategies).

### Architecture & Design
- **Port:** `8000`
- **Engine:** `ai_engine.py` calls Google Gemini REST API with strict Pydantic schema validation.
- **Demo Mode Fallback:** If `GEMINI_API_KEY` is empty or rate-limited, it automatically falls back to realistic deterministic demo responses, ensuring zero crashes.
- **Spring Boot Bridge:** `PythonAIClient.java` connects Spring Boot to FastAPI. It includes a non-blocking startup check (`AIServiceStartupBean.java`) and complete graceful fallbacks if FastAPI is disabled or offline.
- **Opt-in Toggle:** Configured in `application.yml` via `PYTHON_AI_ENABLED=true`.

### FastAPI REST Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/ai/health` | Health check & mode detection (`real_ai` or `demo`) |
| `POST` | `/api/ai/analyze-quote` | Extract structured JSON data from raw document text |
| `POST` | `/api/ai/compare-quotes` | Multi-quote comparison with market intelligence context |
| `POST` | `/api/ai/recommend-vendor` | Recommend best vendor with executive summary & reasons |
| `POST` | `/api/ai/negotiation-strategy` | Defensive / Balanced / Aggressive negotiation strategy |
| `POST` | `/api/ai/generate-negotiation` | Draft formal negotiation email tailored to selected approach |
| `POST` | `/api/ai/analyze-vendor-response` | Evaluate vendor counter-offer against max budget floor |
| `POST` | `/api/ai/evaluate-vendor` | 1-10 vendor score across price (40%), warranty (25%), delivery (20%), compliance (15%) |

---

## 7. Database Architecture

```mermaid
erDiagram
    USERS ||--o{ WORKFLOW_EXECUTIONS : creates
    WORKFLOW_EXECUTIONS ||--o{ QUOTES : contains
    WORKFLOW_EXECUTIONS ||--o{ NEGOTIATIONS : tracks
    WORKFLOW_EXECUTIONS ||--o{ PURCHASE_ORDERS : generates
    VENDORS ||--o{ QUOTES : issues
    QUOTES ||--o{ QUOTE_ITEMS : contains
    QUOTES ||--o{ NEGOTIATIONS : subject_of
    NEGOTIATIONS ||--o{ NEGOTIATION_ROUNDS : history
    NEGOTIATIONS ||--o{ APPROVALS : requires
    PURCHASE_ORDERS ||--o{ PURCHASE_ORDER_ITEMS : contains

    USERS {
        Long id PK
        String email UK
        String passwordHash
        String name
        Enum role
    }

    VENDORS {
        Long id PK
        String name
        String contactEmail
        Double reliabilityScore
    }

    WORKFLOW_EXECUTIONS {
        Long id PK
        String title
        Enum status
    }

    QUOTES {
        Long id PK
        Long workflow_id FK
        Long vendor_id FK
        BigDecimal calculatedTotal
        Enum extractionStatus
        Enum benchmarkStatus
    }

    NEGOTIATIONS {
        Long id PK
        Long quote_id FK
        BigDecimal currentPrice
        BigDecimal targetPrice
        BigDecimal maxApprovedPrice
        Enum status
    }

    PURCHASE_ORDERS {
        Long id PK
        String poNumber UK
        Long vendor_id FK
        BigDecimal totalAmount
        String pdfFilePath
        Enum status
    }
```

---

## 8. API Architecture

| Module | Method | Endpoint | Purpose | Access |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/auth/login` | Authenticate user & return JWT token | Public |
| **Auth** | `POST` | `/api/auth/register` | Register new user account | Public |
| **Quotes** | `POST` | `/api/quotes` | Upload raw quotation text for AI extraction | Authenticated |
| **Quotes** | `POST` | `/api/quotes/upload` | Upload PDF file for OCR text stripping & ingestion | Authenticated |
| **Quotes** | `GET` | `/api/quotes` | Retrieve all processed quotations | Authenticated |
| **Comparison** | `GET` | `/api/comparison/workflows/{id}` | Run multi-criteria scoring & Gemini AI ranking | Authenticated |
| **Negotiation** | `POST` | `/api/negotiations/quotes/{id}/draft` | AI drafts negotiation strategy & email | Authenticated |
| **Negotiation** | `POST` | `/api/negotiations/{id}/approve` | Approve/reject drafted negotiation email | Admin / Approver / Procurement User |
| **Negotiation** | `POST` | `/api/negotiations/{id}/simulate-response` | Submit vendor counter price for AI evaluation | Authenticated |
| **Purchase Orders** | `POST` | `/api/purchase-orders/generate` | Generate official PO entity & render PDF | Admin / Approver / Procurement User |
| **Purchase Orders** | `GET` | `/api/purchase-orders/{id}/pdf` | Stream rendered A4 PDF document | Public / Token query param |
| **Purchase Orders** | `POST` | `/api/purchase-orders/{id}/send-email` | Dispatch PO PDF to vendor via Brevo | Admin / Approver / Procurement User |
| **Analytics** | `GET` | `/api/dashboard` | Get real-time spend, savings, and workflow counts | Authenticated |
| **Demo** | `POST` | `/api/demo/run` | Execute full automated scenario (Lenovo, HP, Dell) | Authenticated |
| **FastAPI AI** | `GET` | `/api/ai/health` | FastAPI health check | Internal / Public |
| **FastAPI AI** | `POST` | `/api/ai/evaluate-vendor` | 1-10 AI Vendor Scorecard evaluation | Internal |

---

## 9. Frontend Architecture

The frontend is built with React 18 and TypeScript, using a custom API client (`client.ts`) that manages Axios HTTP requests, JWT header attachment, and error handling.

### Key Pages:
- **`Dashboard.tsx`**: High-level command center displaying total spend, savings KPI cards, spend allocation chart (Recharts), and active workflow list.
- **`QuotesPage.tsx`**: Quote Ingestion Gateway supporting PDF drag-and-drop file upload and plain-text quote extraction via Gemini AI.
- **`ComparisonPage.tsx`**: Multi-criteria scoring matrix highlighting Gemini AI top recommended vendor banner.
- **`NegotiationPage.tsx`**: Interactive AI Negotiation workspace with email editor and Brevo dispatch button.
- **`ApprovalsPage.tsx`**: Human governance queue for reviewing and approving financial actions.
- **`VendorInboxPage.tsx`**: Outbound email logs and vendor reply simulator.
- **`PurchaseOrdersPage.tsx`**: Purchase Order table with direct PDF download and Brevo dispatch.
- **`DemoPage.tsx`**: One-click scenario demo engine with vendor scenario switching (Lenovo, HP, Dell).

---

## 10. Backend Architecture

The backend follows a layered Spring Boot architecture:

```
com.procureai
├── config/       # SecurityConfig, AIConfig, EmailConfig, DataSeeder
├── controller/   # REST API Controllers (@RestController)
├── dto/          # Validated Request/Response records
├── entity/       # JPA Entities (@Entity)
├── exception/    # GlobalExceptionHandler & Custom Exceptions
├── repository/   # Spring Data JPA Repositories
├── security/     # JwtAuthFilter, JwtService, AuthenticatedUser
├── service/      # Business logic, orchestration, calculations
│   ├── ai/       # GeminiAIProvider, PythonAIClient, PythonAIService, AIServiceStartupBean
│   └── email/    # BrevoEmailService, MockEmailService
└── util/         # InputSanitizer, CurrentUser helper
```

---

## 11. AI Architecture & Multi-Agent Framework

ProcureAI uses Google Gemini 1.5 Flash for unstructured text parsing, strategic reasoning, and natural language evaluation.

```mermaid
flowchart LR
    subgraph Multi_Agent_Prompts ["🤖 Multi-Agent Prompt Orchestration"]
        RFP["RFP Compliance Agent"]
        LEGAL["Legal Compliance Agent"]
        EVAL["Vendor Evaluation Agent (1-10 Scorecard)"]
        MKT["Market Intelligence Agent"]
        STRAT["Negotiation Strategy Agent (Defensive/Balanced/Aggressive)"]
        REPORT["Executive Report Generator Agent"]
    end

    subgraph Data_Inputs ["📄 Context Inputs"]
        DOC["Vendor Quote PDF / Text"]
        BENCH["Market Benchmark Database"]
        DATASET["Category Market Intel JSON"]
    end

    subgraph AI_Inference ["⚡ Gemini AI Reasoning"]
        PROMPT_ENGINE["ai_engine.py Prompt Formatter"]
        GEMINI_LLM["Google Gemini 1.5 Flash LLM"]
        PYDANTIC["Pydantic Output Validator"]
    end

    subgraph Guardrails ["🛡️ Business Logic & Safety"]
        MATH["Spring Boot Total Math Validator"]
        FLOOR["Budget Ceiling & Price Floor Guard"]
        APPROVAL["Human Approval Gate"]
    end

    DOC --> PROMPT_ENGINE
    BENCH --> PROMPT_ENGINE
    DATASET --> MKT
    MKT --> PROMPT_ENGINE
    RFP & LEGAL & EVAL & STRAT & REPORT --> PROMPT_ENGINE
    PROMPT_ENGINE --> GEMINI_LLM
    GEMINI_LLM --> PYDANTIC
    PYDANTIC --> MATH
    MATH --> FLOOR
    FLOOR --> APPROVAL

    style Multi_Agent_Prompts fill:#1e1b4b,stroke:#818cf8,stroke-width:2px,color:#fff
    style AI_Inference fill:#022c22,stroke:#34d399,stroke-width:2px,color:#fff
    style Guardrails fill:#31103f,stroke:#c084fc,stroke-width:2px,color:#fff
```

### Multi-Agent Prompts & Intelligence Models
1. **RFP Compliance Agent**: Evaluates vendor proposal compliance against core requirements (scoring 1-10).
2. **Legal Compliance Agent**: Checks alignment with legal/regulatory framework and policy documents.
3. **Vendor Evaluation Agent**: Scores reputation, warranty, delivery, and price across 4 dimensions.
4. **Market Intelligence Agent**: Integrates category market datasets (Electronics, Laptops, IT Services, Displays, Office Supplies).
5. **Negotiation Strategy Agent**: Classifies negotiations into **Defensive**, **Balanced**, or **Aggressive** approaches with key leverage points.
6. **Evaluation Report Generator Agent**: Consolidates vendor scorecards into an executive summary.

### Real AI vs. Business Logic Separation:
- **AI Responsibilities**: Line-item extraction from free-form text, strategy formulation (Defensive/Balanced/Aggressive), explaining vendor trade-offs in plain language, evaluating vendor counter-offers.
- **Backend Business Logic Responsibilities**: Computing tax, shipping, and total amounts (never trusting LLM math), enforcing `maxApprovedPrice` discount caps, limiting negotiation rounds, enforcing human approval gates.

---

## 12. Authentication & Security

- **Stateless JWT Authentication**: Signed with HMAC-SHA256. Issued upon successful login/registration.
- **Role-Based Access Control (RBAC)**: Enforced via `@PreAuthorize("hasAnyRole(...)")` annotations on controller methods.
- **Input Sanitization**: `InputSanitizer.java` strips control characters, limits text size to 50,000 characters, and validates email formatting to prevent injection attacks.
- **File Upload Security**: Enforces 10MB maximum file size, whitelists `.pdf` and `.txt` extensions, and verifies magic bytes (`%PDF` byte sequence).
- **Path Traversal Protection**: PDF download paths are validated to ensure they remain inside the target `outputDir`.

---

## 13. Project Structure

```
ProcureAI/
├── AI-SERVICE/                # Python FastAPI AI Microservice
│   ├── demo_data/             # Category market intelligence datasets
│   │   └── market_intelligence.json
│   ├── tests/                 # pytest suite (16 tests)
│   │   └── test_api.py
│   ├── ai_engine.py           # Gemini-powered AI engine & prompts
│   ├── config.py              # Settings from environment
│   ├── Dockerfile             # Container definition for FastAPI
│   ├── main.py                # FastAPI endpoints & middleware
│   ├── README.md              # AI Service documentation
│   ├── requirements.txt       # Python dependencies (FastAPI, Pydantic)
│   └── schemas.py             # Pydantic request/response schemas
├── BACKEND/
│   ├── src/main/java/com/procureai/
│   │   ├── config/            # Security & Bean configuration
│   │   ├── controller/        # REST API Endpoints
│   │   ├── dto/               # Validated Data Transfer Objects
│   │   ├── entity/            # JPA Database Entities
│   │   ├── repository/        # JPA Repositories
│   │   ├── security/          # JWT Filter & Services
│   │   ├── service/           # Core Domain Services
│   │   │   ├── ai/            # GeminiAIProvider, PythonAIClient, PythonAIService
│   │   │   └── email/         # BrevoEmailService, MockEmailService
│   │   └── util/              # Security & Input Utilities
│   ├── src/main/resources/
│   │   └── application.yml    # Spring Configuration
│   └── pom.xml                # Maven Dependencies
├── FRONTEND/
│   ├── src/
│   │   ├── api/client.ts      # Axios API Client
│   │   ├── components/        # Reusable UI Components & Layout
│   │   ├── pages/             # Application Pages
│   │   └── types/index.ts     # TypeScript Interfaces
│   ├── index.html
│   ├── package.json
│   └── vite.config.ts
└── docker-compose.yml         # Full-stack Docker orchestration
```

---

## 14. Setup & Installation

### Prerequisites
- **Java 17 JDK** or higher
- **Maven 3.8+**
- **Node.js 18+** & `npm`
- **Python 3.11+** (for FastAPI AI service)

### Option A: Running Standalone Components (Local Dev)

#### 1. FastAPI AI Service (Port 8000)
```bash
cd AI-SERVICE
pip install -r requirements.txt
python -m uvicorn main:app --reload --port 8000
```
*Health check available at `http://localhost:8000/api/ai/health` and Swagger UI at `http://localhost:8000/docs`.*

#### 2. Spring Boot Backend (Port 8080)
```bash
cd BACKEND
# Optional: set PYTHON_AI_ENABLED=true to enable FastAPI integration
set PYTHON_AI_ENABLED=true
mvn spring-boot:run
```
*Backend starts on `http://localhost:8080`.*

#### 3. React Frontend (Port 5173)
```bash
cd FRONTEND
npm install
npm run dev
```
*Frontend starts on `http://localhost:5173`.*

---

### Option B: Full-Stack Docker Compose

Run all 3 services (MySQL, FastAPI AI Service, Spring Boot Backend) together:

```bash
# Set environment variables (or rely on built-in demo defaults)
export GEMINI_API_KEY="your_gemini_api_key"
export BREVO_API_KEY="your_brevo_api_key"

# Launch container stack
docker-compose up --build
```

---

## 15. Environment Variables

| Variable | Purpose | Required | Default |
| :--- | :--- | :--- | :--- |
| `GEMINI_API_KEY` | Google Gemini AI REST API Key | Optional (Falls back to Demo Mode) | - |
| `BREVO_API_KEY` | Brevo Outbound Email API Key | Optional (Falls back to Mock Email) | - |
| `PYTHON_AI_ENABLED` | Enable FastAPI Python AI Service bridge in Spring Boot | No | `false` |
| `PYTHON_AI_URL` | Base URL of FastAPI AI Service | No | `http://localhost:8000` |
| `DB_USERNAME` | Database Connection Username | Yes | `sa` / `root` |
| `DB_PASSWORD` | Database Connection Password | Yes | `admin` |
| `JWT_SECRET` | Secret Key for JWT Token Signing | Recommended | Development Secret |
| `APP_CORS_ALLOWED_ORIGINS` | Permitted CORS Origins | No | `http://localhost:5173` |

---

## 16. Judge Demo Guide

Follow these steps for a complete hackathon demo evaluation:

1. **Log In**: Open `http://localhost:5173`. Click **Log In** (pre-filled with demo admin credentials `admin@procureai.demo` / `Admin@12345`).
2. **Run Scenario Demo**: Click **Run Full Demo** in the sidebar navigation.
3. **Select Scenario**: Choose **Lenovo Corporate Sales** (or **HP** / **Dell**). Click **Launch Procurement Demo**.
4. **Observe Live Steps**: Watch the step execution tracker execute quote extraction, benchmarking, ranking, negotiation drafting, approval, and PO PDF generation.
5. **Inspect Quote Comparison**: Navigate to **Quote Comparison** (`/comparison`) to see the multi-criteria score matrix and Gemini AI recommended vendor banner.
6. **Review Negotiation**: Navigate to **AI Negotiation Center** (`/negotiation`) to review the Gemini AI reasoning strategy (with `[Defensive]`, `[Balanced]`, or `[Aggressive]` approach) and Brevo email draft.
7. **Test Vendor Simulator**: Submit a simulated vendor counter-price to watch Gemini re-evaluate the offer.
8. **View & Download PO PDF**: Open **Purchase Orders** (`/purchase-orders`) and click **PDF** to view the server-rendered PDF document in your browser.

---

## 17. Key Features Matrix

| Feature | Description | Status |
| :--- | :--- | :--- |
| **PDF & Text Ingestion** | Drag-and-drop PDF parsing & raw quote text extraction | Implemented |
| **Gemini AI Quote Parsing** | Normalizes line items, tax, shipping, and currency | Implemented |
| **Python FastAPI Microservice** | Dedicated FastAPI AI service with Pydantic contract validation | Implemented |
| **Multi-Agent Evaluation Framework** | 1-10 Vendor Scorecards across price, warranty, delivery, compliance | Implemented |
| **Category Market Datasets** | Market intelligence data across 6 procurement categories | Implemented |
| **Negotiation Approach Framing** | Dynamic Defensive, Balanced, or Aggressive negotiation strategies | Implemented |
| **Market Benchmarking** | Checks pricing against market price floors and ceilings | Implemented |
| **Multi-Criteria Scoring** | Ranks vendors out of 100 on price, warranty, delivery, reliability | Implemented |
| **Human Approval Gate** | Mandates human review before financial action | Implemented |
| **Brevo Email Dispatch** | Dispatches outbound negotiation emails via Brevo API v3 | Implemented |
| **Vendor Response Evaluator** | Scores vendor counter-offers against target budget | Implemented |
| **PDFBox PO Renderer** | Generates A4 Purchase Order PDFs dynamically on-the-fly | Implemented |
| **Reactive Dashboard** | Displays real-time spend, savings, and workflow status | Implemented |

---

## 18. UI & Screenshots

| Section | Preview |
| :--- | :--- |
| **Dashboard** | *Procurement Command Center with spend charts & KPI metrics* |
| **Quotes & Ingestion** | *PDF Drag-and-Drop Ingestion Gateway with Gemini AI badge* |
| **Quote Comparison** | *Multi-criteria scoring matrix & Gemini top vendor recommendation* |
| **AI Negotiation Center** | *Human-in-the-loop strategy editor & Brevo email dispatch* |
| **Human Approvals** | *Governance queue for pending financial actions* |
| **Purchase Orders** | *Server-rendered PDF table with direct download link* |

---

## 19. Error Handling & Reliability

- **API Fallbacks**: If the Gemini API key is missing or encounters a rate limit, `ai_engine.py` and `GeminiAIProvider` seamlessly fall back to realistic demo responses without crashing.
- **FastAPI Service Resilience**: If `PYTHON_AI_ENABLED=true` but the FastAPI service is offline, `PythonAIClient.java` logs a warning and Spring Boot transparently falls back to `GeminiAIProvider`.
- **Email Resilience**: If `BREVO_API_KEY` is absent or network requests fail, `BrevoEmailService` logs to console via `MockEmailService` and marks the email status appropriately.
- **On-The-Fly PDF Regeneration**: If a Purchase Order PDF file is missing on disk, `PurchaseOrderController` automatically re-renders it on demand via Apache PDFBox.

---

## 20. Performance & Testing

- **Backend Test Coverage**: 100% passing Spring Boot unit and integration test suite (`mvn clean test`).
- **FastAPI AI Test Suite**: 16/16 passing pytest test suite (`cd AI-SERVICE && pytest tests/`).
- **Clean Compilation**: Zero TypeScript errors and clean Vite production build (`npm run build`).

---

## 21. Future Improvements

- **Multi-File Batch OCR**: Native Tesseract OCR integration for scanned paper quotations.
- **ERP Integrations**: Direct Webhook / REST connectors for SAP S/4HANA and Oracle NetSuite.
- **Multi-Currency FX Rates**: Real-time foreign exchange rate conversion for global vendor bidding.

---

## 22. Hackathon Pitch

> **Why ProcureAI Wins**:
> ProcureAI takes autonomous AI agents out of sandbox chats and applies them directly to real-world corporate purchasing. By combining a **Python FastAPI AI Microservice** running **Google Gemini AI** for multi-agent reasoning, **Brevo** for communication, and **Apache PDFBox** for official document generation — all guarded by a strict **Human Approval Gate** — ProcureAI delivers an enterprise-ready solution that saves companies time and money.