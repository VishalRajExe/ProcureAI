"""
ProcureAI FastAPI AI Service — Main Application

Architecture:
  Spring Boot (port 8080) → HTTP → FastAPI (port 8000) → Gemini API

Security:
  - Internal service only (not publicly exposed)
  - Request size limits enforced
  - Input validation via Pydantic
  - No arbitrary file/model paths from HTTP requests
  - No direct DB access
  - API key auth via X-Internal-Token header (optional, disabled by default for local dev)
"""
from __future__ import annotations

import logging
import os
import time
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

import ai_engine
from config import settings, load_settings
from schemas import (
    AnalyzeQuoteRequest, AnalyzeQuoteResponse,
    AnalyzeVendorResponseRequest, AnalyzeVendorResponseResponse,
    CompareQuotesRequest, CompareQuotesResponse,
    ErrorResponse,
    EvaluateVendorRequest, EvaluateVendorResponse,
    GenerateNegotiationRequest, GenerateNegotiationResponse,
    HealthResponse,
    NegotiationStrategyRequest, NegotiationStrategyResponse,
    RecommendVendorRequest, RecommendVendorResponse,
)

# ─── Logging ───────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
log = logging.getLogger("procureai.ai-service")

# ─── Internal auth token (optional) ───────────────────────────────────────────
INTERNAL_TOKEN: Optional[str] = os.environ.get("AI_INTERNAL_TOKEN", "").strip() or None

# ─── Lifespan ──────────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Reload settings in case env changed
    s = load_settings()
    mode = "REAL AI MODE (Gemini)" if not s.demo_mode else "DEMO MODE (no Gemini key)"
    log.info("=" * 60)
    log.info("ProcureAI AI Service starting — %s", mode)
    log.info("Gemini model: %s", s.gemini_model)
    log.info("=" * 60)
    yield
    log.info("ProcureAI AI Service shutting down")


# ─── App ───────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="ProcureAI AI Service",
    description="Gemini-powered AI/ML service for ProcureAI procurement intelligence",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url=None,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[os.environ.get("SPRING_BOOT_ORIGIN", "http://localhost:8080")],
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


# ─── Auth middleware (optional) ────────────────────────────────────────────────
@app.middleware("http")
async def auth_middleware(request: Request, call_next):
    if request.url.path in ("/api/ai/health", "/docs", "/openapi.json"):
        return await call_next(request)
    if INTERNAL_TOKEN:
        token = request.headers.get("X-Internal-Token", "")
        if token != INTERNAL_TOKEN:
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN,
                content={"error": "Unauthorized", "detail": "Invalid or missing X-Internal-Token"},
            )
    return await call_next(request)


# ─── Request size limit ────────────────────────────────────────────────────────
MAX_REQUEST_BYTES = 2 * 1024 * 1024  # 2 MB

@app.middleware("http")
async def limit_request_size(request: Request, call_next):
    content_length = request.headers.get("content-length")
    if content_length and int(content_length) > MAX_REQUEST_BYTES:
        return JSONResponse(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            content={"error": "Request too large", "detail": f"Max {MAX_REQUEST_BYTES // 1024}KB"},
        )
    return await call_next(request)


# ─── Global error handler ──────────────────────────────────────────────────────
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    log.error("Unhandled error on %s: %s", request.url.path, exc, exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"error": "Internal AI service error", "detail": "Please try again or check service logs"},
    )


# ─────────────────────────────────────────────────────────────────────────────
# ENDPOINTS
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/api/ai/health", response_model=HealthResponse, tags=["Health"])
async def health():
    """Health check — Spring Boot polls this on startup to verify AI service is ready."""
    mode = "real_ai" if not settings.demo_mode else "demo"
    return HealthResponse(
        status="ok",
        mode=mode,
        gemini_configured=not settings.demo_mode,
    )


@app.post("/api/ai/analyze-quote", response_model=AnalyzeQuoteResponse, tags=["Quote Analysis"])
async def analyze_quote(req: AnalyzeQuoteRequest):
    """
    Extract structured quote data from raw document text.
    
    Spring Boot sends raw OCR/text from uploaded vendor quotation.
    Returns structured JSON that Spring Boot validates before DB persistence.
    """
    if not req.raw_text or len(req.raw_text.strip()) < 10:
        raise HTTPException(status_code=400, detail="raw_text must contain meaningful content")
    
    log.info("analyze-quote: vendor_hint=%s, doc_type=%s, text_len=%d",
             req.hinted_vendor_name, req.document_type, len(req.raw_text))
    
    result = ai_engine.analyze_quote(req)
    log.info("analyze-quote: vendor=%s, items=%d, confidence=%.2f, mode=%s",
             result.vendor_name, len(result.items), result.confidence, result.ai_mode)
    return result


@app.post("/api/ai/compare-quotes", response_model=CompareQuotesResponse, tags=["Vendor Intelligence"])
async def compare_quotes(req: CompareQuotesRequest):
    """
    Compare multiple vendor quotes and produce ranked recommendations.
    
    Spring Boot sends normalized quote summaries.
    Returns ranked list with scores, strengths, and concerns.
    """
    if len(req.quotes) < 2:
        raise HTTPException(status_code=400, detail="At least 2 quotes required for comparison")
    
    log.info("compare-quotes: %d quotes, category=%s", len(req.quotes), req.category)
    result = ai_engine.compare_quotes(req)
    log.info("compare-quotes: recommended=%s, mode=%s", result.recommended_vendor_name, result.ai_mode)
    return result


@app.post("/api/ai/recommend-vendor", response_model=RecommendVendorResponse, tags=["Vendor Intelligence"])
async def recommend_vendor(req: RecommendVendorRequest):
    """
    Recommend the best vendor with executive summary and key reasons.
    
    Spring Boot uses this recommendation as input for human review — not for automatic vendor selection.
    """
    log.info("recommend-vendor: %d quotes, category=%s", len(req.quotes), req.category)
    result = ai_engine.recommend_vendor(req)
    log.info("recommend-vendor: recommended=%s, confidence=%.2f, mode=%s",
             result.recommended_vendor_name, result.confidence, result.ai_mode)
    return result


@app.post("/api/ai/negotiation-strategy", response_model=NegotiationStrategyResponse, tags=["Negotiation"])
async def negotiation_strategy(req: NegotiationStrategyRequest):
    """
    Generate negotiation strategy: Defensive / Balanced / Aggressive approach.
    
    Spring Boot validates the strategy against procurement rules before applying.
    Target price is advisory — Spring Boot enforces the actual max approved price.
    """
    log.info("negotiation-strategy: vendor=%s, current=%.0f, target=%.0f, max=%.0f",
             req.vendor_name, req.current_price, req.target_price, req.max_acceptable_price)
    result = ai_engine.negotiation_strategy(req)
    log.info("negotiation-strategy: action=%s, approach=%s, confidence=%.2f, mode=%s",
             result.action, result.approach, result.confidence, result.ai_mode)
    return result


@app.post("/api/ai/generate-negotiation", response_model=GenerateNegotiationResponse, tags=["Negotiation"])
async def generate_negotiation(req: GenerateNegotiationRequest):
    """
    Draft a negotiation email tailored to the approach (Defensive/Balanced/Aggressive).
    
    Email goes through human review and approval in Spring Boot before Brevo sends it.
    """
    log.info("generate-negotiation: vendor=%s, approach=%s, round=%d",
             req.vendor_name, req.approach, req.negotiation_round)
    result = ai_engine.generate_negotiation_email(req)
    log.info("generate-negotiation: subject='%s', body_len=%d, mode=%s",
             result.email_subject, len(result.email_body), result.ai_mode)
    return result


@app.post("/api/ai/analyze-vendor-response", response_model=AnalyzeVendorResponseResponse, tags=["Negotiation"])
async def analyze_vendor_response(req: AnalyzeVendorResponseRequest):
    """
    Evaluate vendor counter-offer and recommend ACCEPT / NEGOTIATE_AGAIN / REJECT.
    
    Spring Boot validates this recommendation against procurement rules:
    - max approved price
    - negotiation round limits
    - user permissions
    Final decision authority stays with Spring Boot.
    """
    log.info("analyze-vendor-response: vendor=%s, counter=%.0f, max=%.0f, round=%d/%d",
             req.vendor_name, req.counter_price, req.max_acceptable_price,
             req.negotiation_round, req.max_rounds)
    result = ai_engine.analyze_vendor_response(req)
    log.info("analyze-vendor-response: recommend_accept=%s, within_budget=%s, mode=%s",
             result.recommend_accept, result.within_budget, result.ai_mode)
    return result


@app.post("/api/ai/evaluate-vendor", response_model=EvaluateVendorResponse, tags=["Vendor Intelligence"])
async def evaluate_vendor(req: EvaluateVendorRequest):
    """
    Full vendor evaluation: 1-10 score across 4 dimensions + risk level.
    
    Adapted from vendor_evaluation agent (agent_prompts.jinja).
    Score breakdown: price (40%), warranty (25%), delivery (20%), compliance (15%).
    """
    log.info("evaluate-vendor: vendor=%s, price=%.0f", req.vendor.vendor_name, req.vendor.total_price)
    result = ai_engine.evaluate_vendor(req)
    log.info("evaluate-vendor: score=%.1f, risk=%s, rec=%s, mode=%s",
             result.overall_score, result.risk_level, result.recommendation, result.ai_mode)
    return result


# ─── Dev entry point ───────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True, log_level="info")
