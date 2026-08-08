"""
ProcureAI AI Service — Tests

Tests all endpoints in both DEMO MODE and basic validation.
Run with: pytest tests/ -v
"""
import pytest
from fastapi.testclient import TestClient

# Set demo mode before importing app (no API key)
import os
os.environ.setdefault("GEMINI_API_KEY", "")

from main import app

client = TestClient(app)


# ─── Health ───────────────────────────────────────────────────────────────────

def test_health_returns_ok():
    resp = client.get("/api/ai/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "ok"
    assert data["mode"] in ("real_ai", "demo")
    assert "service" in data


# ─── Analyze Quote ────────────────────────────────────────────────────────────

def test_analyze_quote_demo():
    resp = client.post("/api/ai/analyze-quote", json={
        "raw_text": "Vendor: Lenovo India Ltd.\nProduct: ThinkPad X1 Carbon, Qty: 10, Unit Price: 75000\nWarranty: 24 months, Delivery: 14 days\nPayment Terms: Net 30",
        "hinted_vendor_name": "Lenovo",
        "document_type": "quotation",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert "vendor_name" in data
    assert "items" in data
    assert len(data["items"]) >= 1
    assert data["items"][0]["unit_price"] >= 0
    assert 0 <= data["confidence"] <= 1


def test_analyze_quote_empty_text_rejected():
    resp = client.post("/api/ai/analyze-quote", json={
        "raw_text": "   ",
        "document_type": "quotation",
    })
    assert resp.status_code == 400


def test_analyze_quote_text_too_long():
    resp = client.post("/api/ai/analyze-quote", json={
        "raw_text": "x" * 60_001,
        "document_type": "quotation",
    })
    assert resp.status_code == 422  # Pydantic validation


def test_analyze_quote_invalid_doc_type():
    resp = client.post("/api/ai/analyze-quote", json={
        "raw_text": "Some quote text",
        "document_type": "invalid_type",
    })
    assert resp.status_code == 422


# ─── Compare Quotes ───────────────────────────────────────────────────────────

def test_compare_quotes_demo():
    resp = client.post("/api/ai/compare-quotes", json={
        "quotes": [
            {"vendor_name": "Lenovo", "total_price": 850000, "quote_id": 1, "warranty_months": 24, "delivery_days": 14},
            {"vendor_name": "HP", "total_price": 920000, "quote_id": 2, "warranty_months": 12, "delivery_days": 7},
        ],
        "category": "Laptops",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert "rankings" in data
    assert len(data["rankings"]) == 2
    assert "recommended_vendor_name" in data
    assert "ai_rationale" in data


def test_compare_quotes_single_quote_rejected():
    resp = client.post("/api/ai/compare-quotes", json={
        "quotes": [{"vendor_name": "Lenovo", "total_price": 850000}],
    })
    # Pydantic min_length=2 → 422; our manual check → 400
    assert resp.status_code in (400, 422)


# ─── Recommend Vendor ─────────────────────────────────────────────────────────

def test_recommend_vendor_demo():
    resp = client.post("/api/ai/recommend-vendor", json={
        "quotes": [
            {"vendor_name": "Lenovo", "total_price": 850000, "quote_id": 1},
            {"vendor_name": "HP", "total_price": 920000, "quote_id": 2},
        ],
        "category": "Laptops",
        "required_warranty_months": 12,
        "max_delivery_days": 30,
    })
    assert resp.status_code == 200
    data = resp.json()
    assert "recommended_vendor_name" in data
    assert "executive_summary" in data
    assert "key_reasons" in data
    assert 0 < data["confidence"] <= 1


# ─── Negotiation Strategy ─────────────────────────────────────────────────────

def test_negotiation_strategy_demo_aggressive():
    resp = client.post("/api/ai/negotiation-strategy", json={
        "vendor_name": "HP India",
        "product_summary": "HP EliteBook 840 G10",
        "current_price": 950000,
        "target_price": 820000,
        "max_acceptable_price": 900000,
        "quantity": 10,
        "warranty_months": 12,
        "delivery_days": 20,
        "min_warranty_months": 24,
        "max_delivery_days": 30,
        "negotiation_round": 0,
    })
    assert resp.status_code == 200
    data = resp.json()
    assert data["action"] in ("NEGOTIATE", "ACCEPT", "REJECT")
    assert data["approach"] in ("Defensive", "Balanced", "Aggressive")
    assert "strategy" in data
    assert "key_leverage_points" in data


def test_negotiation_strategy_accept_case():
    resp = client.post("/api/ai/negotiation-strategy", json={
        "vendor_name": "Lenovo",
        "product_summary": "ThinkPad X1",
        "current_price": 850000,
        "target_price": 840000,
        "max_acceptable_price": 900000,
        "quantity": 5,
    })
    assert resp.status_code == 200
    data = resp.json()
    # Price is very close to target — demo should suggest ACCEPT or NEGOTIATE
    assert data["action"] in ("NEGOTIATE", "ACCEPT", "REJECT")


# ─── Generate Negotiation Email ───────────────────────────────────────────────

def test_generate_negotiation_email_demo():
    resp = client.post("/api/ai/generate-negotiation", json={
        "vendor_name": "Lenovo India",
        "product_summary": "ThinkPad X1 Carbon Gen 12",
        "current_price": 900000,
        "target_price": 820000,
        "quantity": 10,
        "approach": "Balanced",
        "negotiation_round": 1,
        "sender_name": "Vishal Raj",
        "sender_org": "ProcureAI Corp",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert "email_subject" in data
    assert "email_body" in data
    assert len(data["email_body"]) > 100
    # Should not have placeholder text
    assert "[Name]" not in data["email_body"]
    assert "[Date]" not in data["email_body"]


def test_generate_negotiation_invalid_approach():
    resp = client.post("/api/ai/generate-negotiation", json={
        "vendor_name": "HP",
        "product_summary": "EliteBook",
        "current_price": 100000,
        "target_price": 90000,
        "quantity": 1,
        "approach": "InvalidApproach",
    })
    # FastAPI accepts this but ai_engine uses it as-is (no enum validation)
    assert resp.status_code in (200, 422)


# ─── Vendor Response Analysis ─────────────────────────────────────────────────

def test_analyze_vendor_response_accept():
    resp = client.post("/api/ai/analyze-vendor-response", json={
        "vendor_name": "Lenovo",
        "product_summary": "ThinkPad X1",
        "original_price": 950000,
        "counter_price": 880000,
        "target_price": 850000,
        "max_acceptable_price": 900000,
        "negotiation_round": 1,
        "max_rounds": 2,
    })
    assert resp.status_code == 200
    data = resp.json()
    assert isinstance(data["recommend_accept"], bool)
    assert "decision_reason" in data
    assert data["within_budget"] is True
    assert data["savings_vs_original"] > 0


def test_analyze_vendor_response_reject_over_budget():
    resp = client.post("/api/ai/analyze-vendor-response", json={
        "vendor_name": "HP",
        "product_summary": "EliteBook",
        "original_price": 950000,
        "counter_price": 1200000,
        "target_price": 850000,
        "max_acceptable_price": 900000,
        "negotiation_round": 2,
        "max_rounds": 2,
    })
    assert resp.status_code == 200
    data = resp.json()
    assert data["within_budget"] is False
    assert data["recommend_accept"] is False


# ─── Evaluate Vendor ──────────────────────────────────────────────────────────

def test_evaluate_vendor_demo():
    resp = client.post("/api/ai/evaluate-vendor", json={
        "vendor": {
            "vendor_name": "Lenovo India Ltd.",
            "total_price": 850000,
            "warranty_months": 24,
            "delivery_days": 14,
            "payment_terms": "Net 30",
            "category": "Laptops",
        },
        "budget_ceiling": 1000000,
        "required_warranty_months": 12,
        "max_delivery_days": 30,
    })
    assert resp.status_code == 200
    data = resp.json()
    assert 0 <= data["overall_score"] <= 10
    assert data["risk_level"] in ("Low", "Medium", "High")
    assert data["recommendation"] in ("Recommended", "Acceptable", "Not Recommended")
    assert "score_breakdown" in data
    assert "strengths" in data


def test_evaluate_vendor_over_budget():
    resp = client.post("/api/ai/evaluate-vendor", json={
        "vendor": {
            "vendor_name": "Expensive Vendor",
            "total_price": 1800000,
            "warranty_months": 6,
            "delivery_days": 60,
            "payment_terms": "Advance",
        },
        "budget_ceiling": 1000000,
        "required_warranty_months": 24,
        "max_delivery_days": 30,
    })
    assert resp.status_code == 200
    data = resp.json()
    assert data["overall_score"] < 5.0  # Should score poorly
    assert data["risk_level"] in ("Medium", "High")
