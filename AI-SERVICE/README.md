# ProcureAI AI Service

FastAPI-based AI/ML service for ProcureAI procurement intelligence.

Adapted from:
- `demoprojects/AI-Powered-RFP-Analyzer-main` — multi-agent prompts (vendor evaluation, negotiation strategy, market intelligence)
- `demoprojects/quotation-agent-main` — FastAPI WebSocket agent architecture pattern

Runs through **Google Gemini** (not Azure OpenAI / GPT-4o).

---

## Architecture

```
React Frontend
      ↓
Java Spring Boot (port 8080)   ← Business logic, auth, DB, PO, audit
      ↓ HTTP
FastAPI AI Service (port 8000) ← AI inference, scoring, strategy
      ↓
Gemini API                     ← LLM reasoning
```

---

## Quick Start (Local Dev)

### 1. Install Python dependencies

```bash
cd AI-SERVICE
pip install -r requirements.txt
```

### 2. Configure (optional — leave blank for DEMO MODE)

```bash
# Copy and fill in your Gemini key
cp .env.example .env
# Then edit .env and set GEMINI_API_KEY=your_key
```

> **DEMO MODE** (no API key): All endpoints return deterministic, realistic responses.
> Perfect for development without burning API quota.

### 3. Start the service

```bash
uvicorn main:app --reload --port 8000
```

### 4. Verify

```bash
# Health check
curl http://localhost:8000/api/ai/health

# Interactive docs
open http://localhost:8000/docs
```

---

## Enable in Spring Boot

By default Spring Boot uses only its built-in GeminiAIProvider.
To enable FastAPI integration:

```bash
# Add to Spring Boot environment before starting:
PYTHON_AI_ENABLED=true
PYTHON_AI_URL=http://localhost:8000
```

Spring Boot gracefully falls back to GeminiAIProvider if FastAPI is offline.

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/ai/health` | Health check — returns mode (real_ai or demo) |
| POST | `/api/ai/analyze-quote` | Extract structured data from raw quote text |
| POST | `/api/ai/compare-quotes` | Compare vendor quotes with market intelligence |
| POST | `/api/ai/recommend-vendor` | Recommend best vendor with executive summary |
| POST | `/api/ai/negotiation-strategy` | Defensive/Balanced/Aggressive strategy |
| POST | `/api/ai/generate-negotiation` | Draft negotiation email with approach |
| POST | `/api/ai/analyze-vendor-response` | Evaluate vendor counter-offer |
| POST | `/api/ai/evaluate-vendor` | 1-10 vendor score with dimension breakdown |

---

## Agent Prompts

Adapted from `AI-Powered-RFP-Analyzer-main/src/src/agent_prompts.jinja`:

| Prompt | Adapted To |
|---|---|
| `rfp_compliance` | Quote validation logic |
| `legal_compliance` | Terms & compliance checking |
| `vendor_evaluation` | `/api/ai/evaluate-vendor` (1-10 score) |
| `market_intelligence` | Built-in JSON dataset per category |
| `negotiation_strategy` | `/api/ai/negotiation-strategy` (Defensive/Balanced/Aggressive) |
| `evaluation_report` | `/api/ai/recommend-vendor` executive summary |

---

## Run Tests

```bash
cd AI-SERVICE
pytest tests/ -v
```

---

## Docker

```bash
# Build and run standalone
docker build -t procureai-ai .
docker run -p 8000:8000 -e GEMINI_API_KEY=your_key procureai-ai

# Or use docker-compose (starts all services)
cd ..
docker-compose up
```
