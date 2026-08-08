"""
ProcureAI FastAPI AI Service — Core AI Engine

Adapts agent prompts from demoprojects/AI-Powered-RFP-Analyzer-main
and runs them through Google Gemini (not Azure OpenAI / GPT-4o).

Prompt patterns reused from: agent_prompts.jinja, doc_summarization.py
Market intelligence data: built-in JSON (adapted from MarketIntelligencePlugin)

Two operating modes:
  REAL AI MODE  — Gemini API key configured → calls Gemini API
  DEMO MODE     — No key → deterministic, realistic responses
"""
from __future__ import annotations

import json
import logging
import os
import re
import time
from typing import Any, Dict, List, Optional, Tuple

import httpx

from config import settings
from schemas import (
    AnalyzeQuoteRequest, AnalyzeQuoteResponse,
    AnalyzeVendorResponseResponse,
    CompareQuotesRequest, CompareQuotesResponse, QuoteRanking,
    EvaluateVendorRequest, EvaluateVendorResponse, VendorScoreBreakdown,
    ExtractedItem, GenerateNegotiationRequest, GenerateNegotiationResponse,
    NegotiationStrategyRequest, NegotiationStrategyResponse,
    RecommendVendorRequest, RecommendVendorResponse, AnalyzeVendorResponseRequest,
)

log = logging.getLogger(__name__)

# ─────────────────────────────────────────────────────────────────
# Market intelligence data (adapted from MarketIntelligencePlugin)
# ─────────────────────────────────────────────────────────────────
_MARKET_DATA: Dict[str, Any] = {}

def _load_market_data() -> Dict[str, Any]:
    path = os.path.join(os.path.dirname(__file__), "demo_data", "market_intelligence.json")
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f).get("industries", {})
    except Exception as e:
        log.warning("Could not load market intelligence data: %s", e)
        return {}

_MARKET_DATA = _load_market_data()


def _get_market_intelligence(category: Optional[str]) -> str:
    """Retrieve market insights for a given category (adapted from MarketIntelligencePlugin)."""
    cat = (category or "General").strip()
    data = _MARKET_DATA.get(cat) or _MARKET_DATA.get("General", {})
    if not data:
        return f"No market intelligence available for category: {cat}"
    
    trends = "\n- ".join(data.get("trends", []))
    competitors = "\n- ".join(data.get("competitor_insights", []))
    risks = "\n- ".join(data.get("supply_chain_risks", []))
    regs = "\n- ".join(data.get("regulatory_changes", []))
    
    return f"""### Market Intelligence Report for {cat}

**Industry Trends:**
- {trends}

**Competitor Insights:**
- {competitors}

**Supply Chain Risks:**
- {risks}

**Regulatory Changes:**
- {regs}"""


# ─────────────────────────────────────────────────────────────────
# Gemini API caller (safe, with JSON mode support)
# ─────────────────────────────────────────────────────────────────

_GEMINI_MODEL_CANDIDATES = [
    "gemini-1.5-flash-latest",
    "gemini-2.0-flash",
    "gemini-1.5-pro",
]

def _call_gemini(prompt: str, json_mode: bool = True) -> str:
    """Call Gemini API with model fallback, return raw text response."""
    if not settings.gemini_api_key:
        raise RuntimeError("Gemini API key not configured — running in demo mode")

    safe_prompt = prompt[:settings.max_input_chars]
    model_name = settings.gemini_model
    candidates = [model_name] + [m for m in _GEMINI_MODEL_CANDIDATES if m != model_name]

    base_url = settings.gemini_base_url
    gen_config: Dict[str, Any] = {"temperature": 0.1}
    if json_mode:
        gen_config["responseMimeType"] = "application/json"

    body = {
        "contents": [{"parts": [{"text": safe_prompt}]}],
        "generationConfig": gen_config,
    }

    last_exc: Optional[Exception] = None
    for model in candidates:
        url = f"{base_url}{model}:generateContent?key={settings.gemini_api_key}"
        try:
            with httpx.Client(timeout=settings.request_timeout_s) as client:
                resp = client.post(url, json=body)
                resp.raise_for_status()
                data = resp.json()
                text = data["candidates"][0]["content"]["parts"][0]["text"]
                return text.strip()
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 404:
                log.debug("Gemini model %s returned 404, trying next candidate...", model)
                last_exc = e
                continue
            raise
        except Exception as e:
            last_exc = e
            log.debug("Gemini model %s error: %s", model, e)
            break

    raise RuntimeError(f"All Gemini candidates failed: {last_exc}")


def _parse_json_response(text: str) -> Dict[str, Any]:
    """Strip markdown code fences and parse JSON safely."""
    cleaned = re.sub(r"^```(?:json)?\s*", "", text.strip(), flags=re.MULTILINE)
    cleaned = re.sub(r"\s*```$", "", cleaned.strip(), flags=re.MULTILINE)
    return json.loads(cleaned)


# ─────────────────────────────────────────────────────────────────
# QUOTE ANALYSIS
# Adapts doc_summarization.py chunk + extract pattern
# ─────────────────────────────────────────────────────────────────

def analyze_quote(req: AnalyzeQuoteRequest) -> AnalyzeQuoteResponse:
    ai_mode = "real_ai" if not settings.demo_mode else "demo"

    if settings.demo_mode:
        return _demo_analyze_quote(req)

    prompt = f"""You are an expert procurement document AI parser. Extract structured quote details from the raw text into exact JSON format.

Rules:
- Output ONLY a JSON object matching this schema.
- Do NOT guess or hallucinate missing prices or terms.
- If a field is not present in the document, use the default value shown.

Required JSON Schema:
{{
  "vendorName": "string",
  "items": [
    {{ "productName": "string", "model": "string or null", "quantity": 1, "unitPrice": 100.0 }}
  ],
  "discountPercent": 0.0,
  "taxPercent": 18.0,
  "shippingCost": 0.0,
  "vendorDeclaredTotal": null,
  "warrantyMonths": 12,
  "deliveryDays": 7,
  "paymentTerms": "Net 30",
  "validUntil": "YYYY-MM-DD or null",
  "confidence": 0.95,
  "missingFields": ["list of field names that were not found in document"]
}}

Document Type: {req.document_type}
Hinted Vendor Name: {req.hinted_vendor_name or "Unknown"}

Raw Document Text:
{req.raw_text}"""

    try:
        raw = _call_gemini(prompt, json_mode=True)
        data = _parse_json_response(raw)

        items = [
            ExtractedItem(
                product_name=i.get("productName", "Product"),
                model=i.get("model"),
                quantity=int(i.get("quantity", 1)),
                unit_price=float(i.get("unitPrice", 0.0)),
            )
            for i in data.get("items", [])
        ]
        if not items:
            items = [ExtractedItem(product_name="Product Item", quantity=1, unit_price=0.0)]

        return AnalyzeQuoteResponse(
            vendor_name=data.get("vendorName", req.hinted_vendor_name or "Unknown Vendor"),
            items=items,
            discount_percent=float(data.get("discountPercent", 0.0)),
            tax_percent=float(data.get("taxPercent", 18.0)),
            shipping_cost=float(data.get("shippingCost", 0.0)),
            vendor_declared_total=data.get("vendorDeclaredTotal"),
            warranty_months=int(data.get("warrantyMonths", 12)),
            delivery_days=int(data.get("deliveryDays", 7)),
            payment_terms=data.get("paymentTerms", "Net 30"),
            valid_until=data.get("validUntil"),
            confidence=float(data.get("confidence", 0.9)),
            missing_fields=data.get("missingFields", []),
            ai_mode=ai_mode,
        )
    except Exception as e:
        log.warning("Gemini quote analysis failed: %s — using demo fallback", e)
        result = _demo_analyze_quote(req)
        result.ai_mode = "demo_fallback"
        return result


def _demo_analyze_quote(req: AnalyzeQuoteRequest) -> AnalyzeQuoteResponse:
    vendor = req.hinted_vendor_name or "Demo Vendor Ltd."
    return AnalyzeQuoteResponse(
        vendor_name=vendor,
        items=[ExtractedItem(product_name="Laptop Computer", model="Demo-2026", quantity=10, unit_price=75000.0)],
        discount_percent=5.0,
        tax_percent=18.0,
        shipping_cost=2500.0,
        vendor_declared_total=886750.0,
        warranty_months=24,
        delivery_days=14,
        payment_terms="Net 30",
        valid_until="2026-09-30",
        confidence=0.70,
        missing_fields=["gst_number", "hsn_code"],
        ai_mode="demo",
    )


# ─────────────────────────────────────────────────────────────────
# QUOTE COMPARISON
# Adapted from RFP Compliance + Vendor Evaluation agent patterns
# ─────────────────────────────────────────────────────────────────

def compare_quotes(req: CompareQuotesRequest) -> CompareQuotesResponse:
    if settings.demo_mode:
        return _demo_compare_quotes(req)

    quotes_summary = "\n".join([
        f"  Quote {i+1}: Vendor={q.vendor_name}, Total=₹{q.total_price:,.0f}, "
        f"Warranty={q.warranty_months}mo, Delivery={q.delivery_days}d, Terms={q.payment_terms}"
        for i, q in enumerate(req.quotes)
    ])

    budget_note = f"Budget ceiling: ₹{req.budget_ceiling:,.0f}" if req.budget_ceiling else "No budget ceiling specified"
    market = _get_market_intelligence(req.category)

    # Adapted from vendor_evaluation agent prompt (agent_prompts.jinja)
    prompt = f"""You are an expert procurement analyst. Compare these vendor quotes and rank them.

## Scoring Criteria (adapted from vendor evaluation framework)
- **Score: 1-10 (10 = Best Value, 1 = Poor Value)**
- If all key requirements met and price is competitive: score 7-10
- If minor concerns on delivery or warranty: score 5-7  
- If significant issues with price/terms: score 1-4

## Quotes to Compare
{quotes_summary}

Category: {req.category or "General"}
{budget_note}

## Market Intelligence Context
{market}

Return JSON:
{{
  "rankings": [
    {{
      "rank": 1,
      "vendor_name": "string",
      "score": 8.5,
      "strengths": ["list"],
      "concerns": ["list"]
    }}
  ],
  "recommended_vendor_name": "string",
  "ai_rationale": "2-3 sentence executive summary"
}}

Order rankings by rank (1=best). Include ALL vendors."""

    try:
        raw = _call_gemini(prompt, json_mode=True)
        data = _parse_json_response(raw)

        quote_by_vendor: Dict[str, Any] = {q.vendor_name.lower(): q for q in req.quotes}

        rankings = []
        for r in data.get("rankings", []):
            vendor = r.get("vendor_name", "Unknown")
            matched_quote = quote_by_vendor.get(vendor.lower())
            rankings.append(QuoteRanking(
                rank=int(r.get("rank", 99)),
                quote_id=matched_quote.quote_id if matched_quote else None,
                vendor_name=vendor,
                total_price=matched_quote.total_price if matched_quote else 0.0,
                score=float(r.get("score", 5.0)),
                strengths=r.get("strengths", []),
                concerns=r.get("concerns", []),
            ))

        rec_vendor = data.get("recommended_vendor_name", req.quotes[0].vendor_name)
        rec_quote = quote_by_vendor.get(rec_vendor.lower())

        return CompareQuotesResponse(
            rankings=rankings,
            recommended_vendor_name=rec_vendor,
            recommended_quote_id=rec_quote.quote_id if rec_quote else None,
            ai_rationale=data.get("ai_rationale", "Best overall value based on price-quality ratio."),
            ai_mode="real_ai",
        )
    except Exception as e:
        log.warning("Gemini quote comparison failed: %s — using demo fallback", e)
        result = _demo_compare_quotes(req)
        result.ai_mode = "demo_fallback"
        return result


def _demo_compare_quotes(req: CompareQuotesRequest) -> CompareQuotesResponse:
    sorted_quotes = sorted(req.quotes, key=lambda q: q.total_price)
    rankings = [
        QuoteRanking(
            rank=i + 1,
            quote_id=q.quote_id,
            vendor_name=q.vendor_name,
            total_price=q.total_price,
            score=round(10.0 - i * 1.5, 1),
            strengths=["Competitive pricing"] if i == 0 else ["Established vendor"],
            concerns=[] if i == 0 else ["Higher price point"],
        )
        for i, q in enumerate(sorted_quotes)
    ]
    best = sorted_quotes[0]
    return CompareQuotesResponse(
        rankings=rankings,
        recommended_vendor_name=best.vendor_name,
        recommended_quote_id=best.quote_id,
        ai_rationale=f"{best.vendor_name} offers the best value at ₹{best.total_price:,.0f} with competitive terms.",
        ai_mode="demo",
    )


# ─────────────────────────────────────────────────────────────────
# VENDOR RECOMMENDATION
# ─────────────────────────────────────────────────────────────────

def recommend_vendor(req: RecommendVendorRequest) -> RecommendVendorResponse:
    if settings.demo_mode:
        return _demo_recommend_vendor(req)

    quotes_text = "\n".join([
        f"- {q.vendor_name}: ₹{q.total_price:,.0f}, {q.warranty_months}mo warranty, "
        f"{q.delivery_days} day delivery, {q.payment_terms}"
        for q in req.quotes
    ])

    prompt = f"""You are an expert procurement advisor. Recommend the best vendor.

## Evaluation Report Generator (adapted from agent_prompts.jinja)
Consolidate key factors: price, warranty, delivery, compliance with requirements.
Final Score: 1-10 (10 = Highly Recommended, 1 = High Risk)

## Vendor Quotes
{quotes_text}

Requirements:
- Budget ceiling: {f"₹{req.budget_ceiling:,.0f}" if req.budget_ceiling else "Not specified"}
- Minimum warranty: {req.required_warranty_months} months
- Max delivery: {req.max_delivery_days} days
- Category: {req.category or "General"}

Market Intelligence: {_get_market_intelligence(req.category)[:1500]}

Return JSON:
{{
  "recommended_vendor_name": "string",
  "confidence": 0.85,
  "executive_summary": "2-3 sentence recommendation summary",
  "key_reasons": ["reason1", "reason2", "reason3"],
  "risk_flags": ["flag1 or empty list"]
}}"""

    try:
        raw = _call_gemini(prompt, json_mode=True)
        data = _parse_json_response(raw)

        rec_vendor = data.get("recommended_vendor_name", req.quotes[0].vendor_name)
        rec_quote = next((q for q in req.quotes if q.vendor_name.lower() == rec_vendor.lower()), req.quotes[0])

        return RecommendVendorResponse(
            recommended_vendor_name=rec_vendor,
            recommended_quote_id=rec_quote.quote_id,
            confidence=float(data.get("confidence", 0.85)),
            executive_summary=data.get("executive_summary", "Selected based on best value analysis."),
            key_reasons=data.get("key_reasons", []),
            risk_flags=data.get("risk_flags", []),
            ai_mode="real_ai",
        )
    except Exception as e:
        log.warning("Gemini vendor recommendation failed: %s — using demo fallback", e)
        result = _demo_recommend_vendor(req)
        result.ai_mode = "demo_fallback"
        return result


def _demo_recommend_vendor(req: RecommendVendorRequest) -> RecommendVendorResponse:
    best = min(req.quotes, key=lambda q: q.total_price)
    return RecommendVendorResponse(
        recommended_vendor_name=best.vendor_name,
        recommended_quote_id=best.quote_id,
        confidence=0.75,
        executive_summary=f"{best.vendor_name} is recommended based on competitive pricing of ₹{best.total_price:,.0f} and satisfactory terms.",
        key_reasons=["Lowest total cost", "Meets warranty requirements", "Within delivery window"],
        risk_flags=[],
        ai_mode="demo",
    )


# ─────────────────────────────────────────────────────────────────
# NEGOTIATION STRATEGY
# Adapted from negotiation_strategy agent (agent_prompts.jinja)
# Key innovation: Defensive / Balanced / Aggressive framework
# ─────────────────────────────────────────────────────────────────

def negotiation_strategy(req: NegotiationStrategyRequest) -> NegotiationStrategyResponse:
    if settings.demo_mode:
        return _demo_negotiation_strategy(req)

    market = _get_market_intelligence(None)
    bench_text = (
        f"Benchmark range: ₹{req.benchmark_min_price:,.0f} – ₹{req.benchmark_max_price:,.0f}"
        if req.benchmark_min_price and req.benchmark_max_price
        else "No benchmark data available"
    )

    # Adapted from negotiation_strategy agent prompt in agent_prompts.jinja
    prompt = f"""You are an expert AI procurement negotiator. Develop a negotiation strategy.

## Negotiation Strategy Framework (from procurement agent methodology)
**Negotiation Approach Selection:**
- If vendor risk is low and offer is close to target → **Balanced Approach**
- If vendor offer is significantly above target → **Defensive Approach** (hold firm)
- If alternatives exist and risks are high → **Aggressive Approach** (push hard or walk away)

**Required JSON output:**
{{
  "action": "NEGOTIATE" | "ACCEPT" | "REJECT",
  "approach": "Defensive" | "Balanced" | "Aggressive",
  "target_price": {req.target_price},
  "max_approved_price": {req.max_acceptable_price},
  "strategy": "detailed strategy description (2-3 sentences)",
  "reason": "concise reasoning (1-2 sentences)",
  "key_leverage_points": ["point1", "point2", "point3"],
  "risk_mitigation": ["mitigation1", "mitigation2"],
  "confidence": 0.85
}}

## Context
Vendor: {req.vendor_name}
Product: {req.product_summary}
Current Quoted Price: ₹{req.current_price:,.0f}
Target Price: ₹{req.target_price:,.0f}
Max Approved Budget: ₹{req.max_acceptable_price:,.0f}
{bench_text}
Quantity: {req.quantity} units
Warranty Offered: {req.warranty_months} months (Required: {req.min_warranty_months} months)
Delivery: {req.delivery_days} days (Max allowed: {req.max_delivery_days} days)
Negotiation Round: {req.negotiation_round}

## Market Intelligence (Supply Chain Risks)
{market[:1000]}

Evaluate whether to NEGOTIATE, ACCEPT, or REJECT based on price vs target and risk assessment."""

    try:
        raw = _call_gemini(prompt, json_mode=True)
        data = _parse_json_response(raw)

        action = data.get("action", "NEGOTIATE").upper()
        if action not in ("NEGOTIATE", "ACCEPT", "REJECT"):
            action = "NEGOTIATE"

        approach = data.get("approach", "Balanced")
        if approach not in ("Defensive", "Balanced", "Aggressive"):
            approach = "Balanced"

        return NegotiationStrategyResponse(
            action=action,
            approach=approach,
            target_price=float(data.get("target_price", req.target_price)),
            max_approved_price=float(data.get("max_approved_price", req.max_acceptable_price)),
            strategy=data.get("strategy", "Negotiate toward target price based on market benchmark."),
            reason=data.get("reason", "Current offer exceeds target by significant margin."),
            key_leverage_points=data.get("key_leverage_points", []),
            risk_mitigation=data.get("risk_mitigation", []),
            confidence=float(data.get("confidence", 0.85)),
            ai_mode="real_ai",
        )
    except Exception as e:
        log.warning("Gemini negotiation strategy failed: %s — using demo fallback", e)
        result = _demo_negotiation_strategy(req)
        result.ai_mode = "demo_fallback"
        return result


def _demo_negotiation_strategy(req: NegotiationStrategyRequest) -> NegotiationStrategyResponse:
    gap = req.current_price - req.target_price
    gap_pct = (gap / req.current_price * 100) if req.current_price else 0

    if gap_pct > 12:
        action, approach = "NEGOTIATE", "Aggressive"
        strategy = f"Current price ₹{req.current_price:,.0f} is {gap_pct:.1f}% above target. Apply aggressive counter-offer strategy citing benchmark data and volume commitment."
        leverage = ["Volume discount leverage", "Competitive alternative vendors available", "Market benchmark data supports lower price"]
    elif gap_pct > 5:
        action, approach = "NEGOTIATE", "Balanced"
        strategy = f"Current price is {gap_pct:.1f}% above target. A balanced negotiation emphasizing long-term partnership value should achieve ₹{req.target_price:,.0f}."
        leverage = ["Long-term partnership value", "Early payment terms offered", "Simplified procurement process"]
    else:
        action, approach = "ACCEPT", "Balanced"
        strategy = f"Current price ₹{req.current_price:,.0f} is close to target. Accept with request for extended warranty."
        leverage = ["Relationship continuity", "Payment term flexibility"]

    return NegotiationStrategyResponse(
        action=action,
        approach=approach,
        target_price=req.target_price,
        max_approved_price=req.max_acceptable_price,
        strategy=strategy,
        reason=f"Gap of ₹{gap:,.0f} ({gap_pct:.1f}%) between current and target price.",
        key_leverage_points=leverage,
        risk_mitigation=["Validate with alternative vendor quote", "Document all commitments in writing"],
        confidence=0.75,
        ai_mode="demo",
    )


# ─────────────────────────────────────────────────────────────────
# NEGOTIATION EMAIL GENERATION
# Enhanced from GeminiAIProvider.draftNegotiationEmail
# ─────────────────────────────────────────────────────────────────

def generate_negotiation_email(req: GenerateNegotiationRequest) -> GenerateNegotiationResponse:
    if settings.demo_mode:
        return _demo_generate_email(req)

    approach_guidance = {
        "Defensive": "Hold firm on the target price. Use market data as justification. Be professional but firm.",
        "Balanced": "Express genuine interest in partnership while requesting a price adjustment. Be collaborative.",
        "Aggressive": "Push strongly for the target price. Mention competitive alternatives. Set a clear deadline for response.",
    }.get(req.approach, "Be professional and collaborative.")

    prompt = f"""Draft a formal, persuasive procurement negotiation email from a procurement team to a vendor.

## Approach: {req.approach}
{approach_guidance}

## Context
Vendor: {req.vendor_name}
Item: {req.product_summary} (Qty: {req.quantity} units)
Current Quoted Price: ₹{req.current_price:,.0f}
Proposed Target Price: ₹{req.target_price:,.0f}
Savings Requested: ₹{req.current_price - req.target_price:,.0f} ({((req.current_price - req.target_price)/req.current_price*100):.1f}%)
Strategy Note: {req.strategy}
Round: {req.negotiation_round}
Sender: {req.sender_name}, {req.sender_org}

## Requirements
- Professional, formal tone
- Reference the specific product and quantities
- Clearly state the target price
- Express intent for long-term partnership
- Request response within 2 business days
- DO NOT use placeholder text like [Name] or [Date]
- Email length: 200-350 words

Return JSON:
{{
  "email_subject": "subject line",
  "email_body": "full email body text (plain text, no HTML)"
}}"""

    try:
        raw = _call_gemini(prompt, json_mode=True)
        data = _parse_json_response(raw)
        return GenerateNegotiationResponse(
            email_subject=data.get("email_subject", f"Quotation Discussion — {req.product_summary}"),
            email_body=data.get("email_body", _demo_generate_email(req).email_body),
            ai_mode="real_ai",
        )
    except Exception as e:
        log.warning("Gemini email generation failed: %s — using demo fallback", e)
        result = _demo_generate_email(req)
        result.ai_mode = "demo_fallback"
        return result


def _demo_generate_email(req: GenerateNegotiationRequest) -> GenerateNegotiationResponse:
    savings = req.current_price - req.target_price
    savings_pct = (savings / req.current_price * 100) if req.current_price else 0

    body = f"""Dear {req.vendor_name} Sales Team,

Thank you for your quotation for {req.product_summary} (Quantity: {req.quantity} units), received as part of our procurement evaluation process.

We have completed our internal review of your proposal. While we appreciate the quality and completeness of your offer at ₹{req.current_price:,.0f}, our procurement committee has identified an opportunity to align on more competitive terms given the current market benchmark for this category.

We would like to propose a revised price of ₹{req.target_price:,.0f} — a reduction of approximately {savings_pct:.1f}% from your quoted amount. This target is based on current market rates and competitive proposals we have received for equivalent specifications.

We believe there is strong alignment between our organizations, and we are keen to establish a long-term procurement relationship with {req.vendor_name}. Volume consistency, timely payments, and a simplified procurement process are benefits we are prepared to offer in support of this pricing.

We kindly request that you review this proposal and respond within 2 business days with your best possible offer, along with any flexibility on warranty or delivery terms.

We look forward to a mutually beneficial agreement.

Best regards,
{req.sender_name}
{req.sender_org} Procurement Team"""

    return GenerateNegotiationResponse(
        email_subject=f"Quotation Discussion — {req.product_summary} (Round {req.negotiation_round})",
        email_body=body,
        ai_mode="demo",
    )


# ─────────────────────────────────────────────────────────────────
# VENDOR RESPONSE ANALYSIS
# Adapted from evaluateVendorResponse + RFP compliance scoring
# ─────────────────────────────────────────────────────────────────

def analyze_vendor_response(req: AnalyzeVendorResponseRequest) -> AnalyzeVendorResponseResponse:
    savings = req.original_price - req.counter_price
    savings_pct = (savings / req.original_price * 100) if req.original_price else 0
    within_budget = req.counter_price <= req.max_acceptable_price

    if settings.demo_mode:
        return _demo_analyze_vendor_response(req, savings, savings_pct, within_budget)

    prompt = f"""Evaluate a vendor's counter-offer in a procurement negotiation.

## Assessment Criteria (adapted from RFP compliance scoring methodology)
- Score the counter-offer against budget and target price
- Consider negotiation round context
- Recommend ACCEPT if within budget and movement is satisfactory
- Recommend counter-negotiation if within budget but above target and rounds remain
- Recommend REJECT only if over budget with no room for compromise

Return JSON:
{{
  "recommend_accept": true | false,
  "decision_reason": "ACCEPT | NEGOTIATE_AGAIN | REJECT — with brief reason",
  "notes": "detailed evaluation explanation (2-3 sentences)",
  "confidence": 0.85
}}

## Context
Vendor: {req.vendor_name}
Product: {req.product_summary}
Original Quoted Price: ₹{req.original_price:,.0f}
Vendor Counter Price: ₹{req.counter_price:,.0f}
Target Price: ₹{req.target_price:,.0f}
Max Budget: ₹{req.max_acceptable_price:,.0f}
Savings vs Original: ₹{savings:,.0f} ({savings_pct:.1f}%)
Within Budget: {within_budget}
Negotiation Round: {req.negotiation_round} of {req.max_rounds}"""

    try:
        raw = _call_gemini(prompt, json_mode=True)
        data = _parse_json_response(raw)

        return AnalyzeVendorResponseResponse(
            recommend_accept=bool(data.get("recommend_accept", within_budget)),
            decision_reason=data.get("decision_reason", "ACCEPT" if within_budget else "REJECT"),
            notes=data.get("notes", "Evaluated against budget parameters."),
            within_budget=within_budget,
            savings_vs_original=round(savings, 2),
            savings_percent=round(savings_pct, 2),
            confidence=float(data.get("confidence", 0.85)),
            ai_mode="real_ai",
        )
    except Exception as e:
        log.warning("Gemini vendor response analysis failed: %s — using demo fallback", e)
        result = _demo_analyze_vendor_response(req, savings, savings_pct, within_budget)
        result.ai_mode = "demo_fallback"
        return result


def _demo_analyze_vendor_response(
    req: AnalyzeVendorResponseRequest,
    savings: float, savings_pct: float, within_budget: bool,
) -> AnalyzeVendorResponseResponse:
    rounds_left = req.max_rounds - req.negotiation_round
    at_target = req.counter_price <= req.target_price

    if at_target or (within_budget and rounds_left <= 0):
        rec, reason = True, "ACCEPT — Counter offer is within budget and acceptable terms"
        notes = f"Vendor moved to ₹{req.counter_price:,.0f}, saving ₹{savings:,.0f} ({savings_pct:.1f}%) from original. This is within approved budget."
    elif within_budget and rounds_left > 0:
        rec, reason = False, "NEGOTIATE_AGAIN — Counter is within budget but above target; negotiate further"
        notes = f"Counter price ₹{req.counter_price:,.0f} is within budget but ₹{req.counter_price - req.target_price:,.0f} above target. {rounds_left} round(s) remaining."
    else:
        rec, reason = False, "REJECT — Counter price exceeds maximum approved budget"
        notes = f"Counter price ₹{req.counter_price:,.0f} exceeds max budget ₹{req.max_acceptable_price:,.0f} by ₹{req.counter_price - req.max_acceptable_price:,.0f}."

    return AnalyzeVendorResponseResponse(
        recommend_accept=rec,
        decision_reason=reason,
        notes=notes,
        within_budget=within_budget,
        savings_vs_original=round(savings, 2),
        savings_percent=round(savings_pct, 2),
        confidence=0.80,
        ai_mode="demo",
    )


# ─────────────────────────────────────────────────────────────────
# VENDOR EVALUATION
# Adapted from vendor_evaluation_plugin.py scoring + agent prompt
# Score: 1-10 across 4 dimensions
# ─────────────────────────────────────────────────────────────────

def evaluate_vendor(req: EvaluateVendorRequest) -> EvaluateVendorResponse:
    v = req.vendor

    # Compute deterministic sub-scores (business logic, not AI)
    price_score = _score_price(v.total_price, req.budget_ceiling)
    warranty_score = _score_warranty(v.warranty_months, req.required_warranty_months)
    delivery_score = _score_delivery(v.delivery_days, req.max_delivery_days)
    compliance_score = 8.0  # default — no document compliance data at this point

    overall_score = round(
        price_score * 0.40 + warranty_score * 0.25 +
        delivery_score * 0.20 + compliance_score * 0.15,
        1
    )

    if overall_score >= 7.5:
        risk_level, recommendation = "Low", "Recommended"
    elif overall_score >= 5.0:
        risk_level, recommendation = "Medium", "Acceptable"
    else:
        risk_level, recommendation = "High", "Not Recommended"

    if settings.demo_mode:
        return _demo_evaluate_vendor(req, price_score, warranty_score, delivery_score, compliance_score, overall_score, risk_level, recommendation)

    market = _get_market_intelligence(v.category)

    # Adapted from vendor_evaluation agent prompt (agent_prompts.jinja)
    prompt = f"""You are a Vendor Evaluation Agent. Analyze this vendor's profile for a procurement decision.

## Scoring & Assessment Criteria (from procurement evaluation methodology)
- **Score: 1-10 (10 = Highly Reputable, 1 = Major Concerns)**
- If all criteria met with strong delivery track record: 7-10
- If minor concerns exist: 5-7
- If serious issues: 1-4

## Vendor Profile
Vendor: {v.vendor_name}
Total Price: ₹{v.total_price:,.0f}
Warranty: {v.warranty_months} months (Required: {req.required_warranty_months} months)
Delivery: {v.delivery_days} days (Max: {req.max_delivery_days} days)
Payment Terms: {v.payment_terms}
Category: {v.category or "General"}

## Pre-Calculated Scores
Price Score: {price_score}/10
Warranty Score: {warranty_score}/10
Delivery Score: {delivery_score}/10

## Market Intelligence
{market[:1000]}

Return JSON:
{{
  "strengths": ["strength1", "strength2", "strength3"],
  "concerns": ["concern1 or empty list"],
  "ai_evaluation_summary": "2-3 sentence comprehensive vendor assessment"
}}"""

    try:
        raw = _call_gemini(prompt, json_mode=True)
        data = _parse_json_response(raw)
        return EvaluateVendorResponse(
            vendor_name=v.vendor_name,
            overall_score=overall_score,
            score_breakdown=VendorScoreBreakdown(
                price_score=price_score, warranty_score=warranty_score,
                delivery_score=delivery_score, compliance_score=compliance_score,
                overall_score=overall_score,
            ),
            risk_level=risk_level,
            recommendation=recommendation,
            strengths=data.get("strengths", []),
            concerns=data.get("concerns", []),
            ai_evaluation_summary=data.get("ai_evaluation_summary", "Vendor meets baseline procurement requirements."),
            ai_mode="real_ai",
        )
    except Exception as e:
        log.warning("Gemini vendor evaluation failed: %s — using demo fallback", e)
        result = _demo_evaluate_vendor(req, price_score, warranty_score, delivery_score, compliance_score, overall_score, risk_level, recommendation)
        result.ai_mode = "demo_fallback"
        return result


def _score_price(price: float, ceiling: Optional[float]) -> float:
    if not ceiling or ceiling <= 0:
        return 7.0
    ratio = price / ceiling
    if ratio <= 0.80:
        return 10.0
    elif ratio <= 0.90:
        return 8.5
    elif ratio <= 1.00:
        return 7.0
    elif ratio <= 1.10:
        return 5.0
    else:
        return 2.0


def _score_warranty(offered: int, required: int) -> float:
    if offered >= required * 2:
        return 10.0
    elif offered >= required:
        return 8.0
    elif offered >= required * 0.75:
        return 5.0
    else:
        return 2.0


def _score_delivery(offered_days: int, max_days: int) -> float:
    if offered_days <= max_days * 0.5:
        return 10.0
    elif offered_days <= max_days * 0.75:
        return 8.0
    elif offered_days <= max_days:
        return 7.0
    elif offered_days <= max_days * 1.25:
        return 4.0
    else:
        return 1.0


def _demo_evaluate_vendor(
    req: EvaluateVendorRequest,
    price_score: float, warranty_score: float, delivery_score: float,
    compliance_score: float, overall_score: float,
    risk_level: str, recommendation: str,
) -> EvaluateVendorResponse:
    v = req.vendor
    strengths = []
    concerns = []

    if price_score >= 7:
        strengths.append(f"Competitive pricing at ₹{v.total_price:,.0f}")
    else:
        concerns.append(f"Price ₹{v.total_price:,.0f} is above budget ceiling")

    if warranty_score >= 7:
        strengths.append(f"Warranty of {v.warranty_months} months meets requirements")
    else:
        concerns.append(f"Warranty {v.warranty_months} months below required {req.required_warranty_months} months")

    if delivery_score >= 7:
        strengths.append(f"Delivery in {v.delivery_days} days is within acceptable window")
    else:
        concerns.append(f"Delivery of {v.delivery_days} days exceeds max {req.max_delivery_days} days")

    return EvaluateVendorResponse(
        vendor_name=v.vendor_name,
        overall_score=overall_score,
        score_breakdown=VendorScoreBreakdown(
            price_score=price_score, warranty_score=warranty_score,
            delivery_score=delivery_score, compliance_score=compliance_score,
            overall_score=overall_score,
        ),
        risk_level=risk_level,
        recommendation=recommendation,
        strengths=strengths,
        concerns=concerns,
        ai_evaluation_summary=f"{v.vendor_name} scored {overall_score}/10 overall. Risk level: {risk_level}. {recommendation} based on evaluation criteria.",
        ai_mode="demo",
    )
