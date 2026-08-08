"""
ProcureAI FastAPI AI Service — Request/Response Schemas
All Spring Boot ↔ FastAPI communication uses these typed contracts.
"""
from __future__ import annotations
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


# ──────────────────────────────────────────────
# Shared primitives
# ──────────────────────────────────────────────

class QuoteItem(BaseModel):
    product_name: str
    model: Optional[str] = None
    quantity: int = 1
    unit_price: float = 0.0


class ExtractedItem(BaseModel):
    product_name: str
    model: Optional[str] = None
    quantity: int = 1
    unit_price: float = 0.0


# ──────────────────────────────────────────────
# /api/ai/health
# ──────────────────────────────────────────────

class HealthResponse(BaseModel):
    status: str                   # "ok" | "degraded"
    mode: str                     # "real_ai" | "demo"
    version: str = "1.0.0"
    gemini_configured: bool = False
    service: str = "procureai-ai"


# ──────────────────────────────────────────────
# /api/ai/analyze-quote
# ──────────────────────────────────────────────

class AnalyzeQuoteRequest(BaseModel):
    raw_text: str = Field(..., max_length=50_000)
    hinted_vendor_name: Optional[str] = Field(None, max_length=200)
    document_type: str = Field("quotation", pattern="^(quotation|rfp|proposal|invoice)$")


class AnalyzeQuoteResponse(BaseModel):
    vendor_name: str
    items: List[ExtractedItem]
    discount_percent: float = 0.0
    tax_percent: float = 18.0
    shipping_cost: float = 0.0
    vendor_declared_total: Optional[float] = None
    warranty_months: int = 12
    delivery_days: int = 7
    payment_terms: str = "Net 30"
    valid_until: Optional[str] = None
    confidence: float = 0.9
    missing_fields: List[str] = []
    ai_mode: str = "demo"


# ──────────────────────────────────────────────
# /api/ai/compare-quotes
# ──────────────────────────────────────────────

class QuoteSummary(BaseModel):
    quote_id: Optional[int] = None
    vendor_name: str
    total_price: float
    items: List[QuoteItem] = []
    warranty_months: int = 12
    delivery_days: int = 7
    payment_terms: str = "Net 30"


class CompareQuotesRequest(BaseModel):
    quotes: List[QuoteSummary] = Field(..., min_length=2, max_length=20)
    category: Optional[str] = None
    budget_ceiling: Optional[float] = None


class QuoteRanking(BaseModel):
    rank: int
    quote_id: Optional[int] = None
    vendor_name: str
    total_price: float
    score: float  # 0-10
    strengths: List[str] = []
    concerns: List[str] = []


class CompareQuotesResponse(BaseModel):
    rankings: List[QuoteRanking]
    recommended_vendor_name: str
    recommended_quote_id: Optional[int] = None
    ai_rationale: str
    ai_mode: str = "demo"


# ──────────────────────────────────────────────
# /api/ai/recommend-vendor
# ──────────────────────────────────────────────

class RecommendVendorRequest(BaseModel):
    quotes: List[QuoteSummary] = Field(..., min_length=1, max_length=20)
    category: Optional[str] = None
    budget_ceiling: Optional[float] = None
    required_warranty_months: int = 12
    max_delivery_days: int = 30


class RecommendVendorResponse(BaseModel):
    recommended_vendor_name: str
    recommended_quote_id: Optional[int] = None
    confidence: float = 0.8
    executive_summary: str
    key_reasons: List[str] = []
    risk_flags: List[str] = []
    ai_mode: str = "demo"


# ──────────────────────────────────────────────
# /api/ai/negotiation-strategy
# ──────────────────────────────────────────────

class NegotiationStrategyRequest(BaseModel):
    vendor_name: str = Field(..., max_length=200)
    product_summary: str = Field(..., max_length=500)
    current_price: float
    target_price: float
    max_acceptable_price: float
    quantity: int = 1
    warranty_months: int = 12
    delivery_days: int = 7
    min_warranty_months: int = 24
    max_delivery_days: int = 30
    benchmark_min_price: Optional[float] = None
    benchmark_max_price: Optional[float] = None
    negotiation_round: int = 0


class NegotiationStrategyResponse(BaseModel):
    action: str  # "NEGOTIATE" | "ACCEPT" | "REJECT"
    approach: str  # "Defensive" | "Balanced" | "Aggressive"
    target_price: float
    max_approved_price: float
    strategy: str
    reason: str
    key_leverage_points: List[str] = []
    risk_mitigation: List[str] = []
    confidence: float = 0.85
    ai_mode: str = "demo"


# ──────────────────────────────────────────────
# /api/ai/generate-negotiation
# ──────────────────────────────────────────────

class GenerateNegotiationRequest(BaseModel):
    vendor_name: str = Field(..., max_length=200)
    product_summary: str = Field(..., max_length=500)
    current_price: float
    target_price: float
    quantity: int = 1
    strategy: str = Field("", max_length=1000)
    approach: str = "Balanced"  # Defensive | Balanced | Aggressive
    negotiation_round: int = 1
    sender_name: str = "Procurement Team"
    sender_org: str = "ProcureAI"


class GenerateNegotiationResponse(BaseModel):
    email_subject: str
    email_body: str
    ai_mode: str = "demo"


# ──────────────────────────────────────────────
# /api/ai/analyze-vendor-response
# ──────────────────────────────────────────────

class AnalyzeVendorResponseRequest(BaseModel):
    vendor_name: str = Field(..., max_length=200)
    product_summary: str = Field(..., max_length=500)
    original_price: float
    counter_price: float
    target_price: float
    max_acceptable_price: float
    negotiation_round: int = 1
    max_rounds: int = 2


class AnalyzeVendorResponseResponse(BaseModel):
    recommend_accept: bool
    decision_reason: str
    notes: str
    within_budget: bool
    savings_vs_original: float
    savings_percent: float
    confidence: float = 0.85
    ai_mode: str = "demo"


# ──────────────────────────────────────────────
# /api/ai/evaluate-vendor
# ──────────────────────────────────────────────

class VendorProfile(BaseModel):
    vendor_name: str = Field(..., max_length=200)
    total_price: float
    warranty_months: int = 12
    delivery_days: int = 7
    payment_terms: str = "Net 30"
    category: Optional[str] = None
    past_performance_notes: Optional[str] = Field(None, max_length=1000)


class EvaluateVendorRequest(BaseModel):
    vendor: VendorProfile
    budget_ceiling: Optional[float] = None
    required_warranty_months: int = 12
    max_delivery_days: int = 30


class VendorScoreBreakdown(BaseModel):
    price_score: float        # 0-10
    warranty_score: float     # 0-10
    delivery_score: float     # 0-10
    compliance_score: float   # 0-10
    overall_score: float      # 0-10


class EvaluateVendorResponse(BaseModel):
    vendor_name: str
    overall_score: float      # 0-10
    score_breakdown: VendorScoreBreakdown
    risk_level: str           # "Low" | "Medium" | "High"
    recommendation: str       # "Recommended" | "Acceptable" | "Not Recommended"
    strengths: List[str] = []
    concerns: List[str] = []
    ai_evaluation_summary: str
    ai_mode: str = "demo"


# ──────────────────────────────────────────────
# Error response
# ──────────────────────────────────────────────

class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None
    ai_mode: str = "demo"
