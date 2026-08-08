# PRD.md — ProcureAI

## Product Requirements Document

---

## 1. Product Name & Tagline

**Name:** ProcureAI
**Tagline:** "From vendor quotes to purchase order — automatically."

---

## 2. Problem Statement

Companies routinely receive multiple vendor quotations for the same product or service. Procurement employees currently handle this manually:

- Open emails and download PDFs
- Read quotations line by line
- Copy prices into Excel
- Normalize inconsistent formats and terminology across vendors
- Check taxes, shipping, and hidden costs
- Research whether prices are reasonable (market benchmarking)
- Negotiate with vendors over email
- Wait for replies and compare revised quotes
- Select a final vendor
- Manually create a purchase order (PO)

This process is slow (hours per procurement cycle), error-prone, and inconsistent across employees.

---

## 3. Solution

ProcureAI is an **AI procurement employee**. It automates the entire quote-to-PO lifecycle:

```
Vendor Quotes → AI/OCR Reading → Extraction → Normalization → Comparison
→ Market Benchmark → AI Negotiation Strategy → Human Approval → Send
→ Vendor Reply → AI Re-evaluation → Final Vendor Selection → PO Generation → Audit
```

The system must **understand, compare, decide, negotiate, and act** — not just extract and display data. Document extraction alone is explicitly out of scope as a standalone value proposition.

---

## 4. Target Users

| User | Needs |
|---|---|
| **Procurement Manager / Buyer** | Compare vendor quotes fast, negotiate confidently, stay within budget rules, generate POs without manual drafting |
| **Approver (e.g., Finance/Manager role)** | Review AI-drafted negotiations and final vendor selections before anything is sent or committed |
| **Admin** | Configure negotiation rules, scoring weights, benchmark data, and manage users/roles |
| **Auditor / Compliance** | Review a complete, timestamped trail of every automated and human decision |

---

## 5. Primary Demo Scenario

**Laptop Procurement — 50 units**, three vendor quotations (Dell Latitude 5450, HP EliteBook, Lenovo ThinkPad) with differing price, tax, shipping, warranty, and delivery terms, submitted in **differently formatted documents** (different terminology: "Price per Unit" vs. "Unit Cost" vs. "Rate") to demonstrate real normalization intelligence — not template matching.

---

## 6. Core Features (Functional Requirements)

### 6.1 Document Ingestion
- Accept PDF, scanned PDF, PNG, JPG, plain email text, JSON
- Pipeline: Upload → Validation → Text Extraction → OCR fallback → AI Understanding → Structured JSON → Schema Validation → DB persistence
- Never assume a perfectly structured document

### 6.2 Structured AI Extraction
- AI must return **structured JSON only**, validated against a backend schema
- Missing fields must be explicitly marked `MISSING_FIELD`, never silently omitted or hallucinated
- Extraction includes a `confidence` score
- Invalid AI output must never directly trigger downstream actions

### 6.3 Quote Normalization Engine
- Normalize vendor-specific terminology (e.g., "Unit Cost", "Rate", "Price per Unit" → `unitPrice`)
- Normalize currency, tax formats, discounts, shipping into one common internal model
- Recalculate totals independently — never trust a vendor's stated total blindly

### 6.4 Comparison Engine
- Side-by-side vendor comparison table (Vendor, Product, Quantity, Unit Price, Discount, Tax, Shipping, Total, Warranty, Delivery, Payment, Benchmark, Score, Recommendation)
- Sortable columns, visual indicators (Good / Above Benchmark / Best Price / Recommended)

### 6.5 Total Cost of Ownership (TCO)
- `effectiveCost` = product price + shipping + taxes − discounts (+ optional service costs)
- Comparisons always use TCO, not headline unit price

### 6.6 AI Vendor Scoring
- Configurable weighted scoring model (default: Price 40%, Warranty 20%, Delivery 15%, Payment Terms 10%, Vendor Reliability 15%)
- **Backend computes the numeric score deterministically.** AI only explains the result in natural language — AI may never invent or override a score.

### 6.7 Market Benchmark Service
- Classifies each quote as `BELOW MARKET` / `WITHIN MARKET` / `ABOVE MARKET` against a benchmark range
- Demo uses a curated, clearly labeled demo/reference dataset
- Architected as a `BenchmarkService` abstraction so a real pricing API can be swapped in later
- Demo data must never be presented as live market data

### 6.8 Negotiation Agent
- Receives current price, benchmark, target price, max acceptable price, quantity, delivery/warranty requirements, negotiation rules
- Produces a structured recommendation (`action`, `targetUnitPrice`, `maximumApprovedPrice`, `strategy`, `reason`, `confidence`)
- **Backend enforces hard negotiation limits.** The AI cannot propose or accept a price above the configured maximum under any circumstance.

### 6.9 Negotiation Rules Configuration
- User-configurable: target discount %, maximum acceptable price, minimum warranty, maximum delivery days, auto-negotiation toggle
- Rules are enforced in backend business logic, not merely suggested by the AI

### 6.10 Negotiation Email Generation & Human-in-the-Loop
- AI drafts a professional vendor negotiation email
- **Emails are never sent automatically.** Every negotiation requires human Edit / Approve & Send / Reject
- This is a mandatory, non-negotiable requirement for this version

### 6.11 Email Simulation (Vendor Inbox Simulator)
- Internal mock vendor inbox simulates sent messages and vendor replies so the full demo works without external email credentials
- Abstracted behind an `EmailService` interface (`MockEmailService`, `GmailEmailService`) so real integration can be added later without redesign

### 6.12 Multi-Round Negotiation
- Supports at least 2 rounds of negotiation
- AI evaluates each vendor counter-offer against the configured maximum
- Automatically stops after a configured maximum round count

### 6.13 Final Vendor Decision
- Combines scoring engine + negotiation outcomes to recommend a final vendor
- Shows full reasoning and alternative vendors — reasoning is never hidden

### 6.14 Purchase Order Generation
- Generates a professional PDF PO (PO Number, Date, Buyer, Vendor, Products, Quantity, Pricing, Tax, Shipping, Total, Warranty, Delivery, Payment Terms, Notes)
- Downloadable PDF

### 6.15 Dashboard & Analytics
- KPIs: Quotes Processed, Negotiations Automated, Estimated Savings, Time Saved, Negotiation Success Rate, Pending Approvals
- Real executions computed from DB records; demo data clearly labeled as seed/demo

### 6.16 Audit Log
- Every workflow event logged with timestamp, user, workflow, event, input, output, status
- Full traceability for enterprise/compliance credibility

### 6.17 Demo Mode
- One-click "Run Demo Procurement" button executes the entire pipeline end-to-end reliably, without external accounts or API keys

---

## 7. Non-Functional Requirements

- **Reliability first:** the full demo path must work every time, offline-capable via mock providers
- **Human-in-the-loop is mandatory** for negotiation emails and PO finalization
- **AI safety:** AI never directly executes actions; all AI output passes through structured validation and an allowed-action registry before backend execution
- **Security:** authentication, authorization, RBAC, input/file validation, no secrets in source control
- Responsive, modern B2B SaaS UI (see `Design.md`)

---

## 8. Explicit Out-of-Scope (v1)

- Automatic email sending without human approval
- Treating demo/benchmark data as real-time market pricing
- Arbitrary AI-executed actions (code execution, unrestricted API calls)
- Complex microservices/Kubernetes/distributed infra
- More than a small, focused set of AI agents

---

## 9. Success Criteria

- A judge/user can understand the product story in under 2 minutes
- The complete workflow (Upload → Extract → Normalize → Compare → Benchmark → Negotiate → Approve → Vendor Reply → Re-evaluate → Select → Generate PO) runs successfully end-to-end in demo mode
- Reported metrics (time saved, savings, automation %) are computed from actual demo workflow execution, not fabricated
