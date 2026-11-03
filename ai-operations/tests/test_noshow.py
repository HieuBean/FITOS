"""Kiểm thử rule engine dự đoán no-show (task #152 — pytest cho ai-operations)."""

import os
import sys

import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from main import app  # noqa: E402
from noshow import predict_no_show  # noqa: E402
from schemas import NoShowRequest  # noqa: E402

client = TestClient(app)
AUTH = {"Authorization": "Bearer fitos-ai-token"}


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "ai-operations"
    assert data["port"] == 8003


def test_requires_bearer_token():
    assert client.post("/predict-noshow", json={}).status_code == 401
    assert client.post("/predict-noshow", json={},
                       headers={"Authorization": "Bearer wrong"}).status_code == 403


def test_probability_is_bounded():
    for days in (0, 1, 5, 30, 90):
        for prior in (0, 3, 20):
            for no_shows in (0, 1, 5, prior):
                result = predict_no_show(NoShowRequest(
                    days_ahead=days, prior_bookings=prior, prior_no_shows=min(no_shows, prior)))
                assert 0.0 <= result.probability <= 1.0


def test_more_history_never_lowers_risk():
    low = predict_no_show(NoShowRequest(days_ahead=2, prior_bookings=10, prior_no_shows=0))
    high = predict_no_show(NoShowRequest(days_ahead=2, prior_bookings=10, prior_no_shows=8))
    assert high.probability > low.probability


def test_far_future_booking_raises_risk():
    near = predict_no_show(NoShowRequest(days_ahead=1))
    far = predict_no_show(NoShowRequest(days_ahead=21))
    assert far.probability > near.probability


def test_risk_level_thresholds():
    safe = predict_no_show(NoShowRequest(days_ahead=0, prior_bookings=20, prior_no_shows=0, weekday=7))
    risky = predict_no_show(NoShowRequest(days_ahead=30, prior_bookings=20, prior_no_shows=20, hour_of_day=21))
    assert safe.risk_level == "LOW"
    assert risky.risk_level == "HIGH"


def test_reason_is_explainable():
    result = predict_no_show(NoShowRequest(days_ahead=10, prior_bookings=10, prior_no_shows=6))
    assert result.reason
    assert any(factor.name == "history" for factor in result.factors)


def test_endpoint_returns_json_contract():
    response = client.post("/predict-noshow", headers=AUTH, json={
        "session_id": 1, "customer_id": 2, "days_ahead": 4,
        "hour_of_day": 18, "weekday": 3, "prior_bookings": 6, "prior_no_shows": 2,
        "capacity": 12, "booked_count": 4,
    })
    assert response.status_code == 200
    data = response.json()
    assert set(["probability", "risk_level", "reason", "model"]).issubset(data)
    assert data["risk_level"] in {"LOW", "MEDIUM", "HIGH"}


def test_invalid_input_rejected():
    response = client.post("/predict-noshow", headers=AUTH,
                           json={"days_ahead": -5, "hour_of_day": 40})
    assert response.status_code == 422
