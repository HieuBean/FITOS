import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_health():
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json()["service"] == "ai-commerce"
    assert res.json()["port"] == 8002

def test_predict_renewal_high_attendance():
    payload = {
        "customer_id": 1,
        "attendance_last_30d": 14,
        "days_since_last_visit": 2,
        "total_months_active": 6,
        "has_pt_package": True
    }
    res = client.post("/predict-renewal", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["customer_id"] == 1
    assert data["renewal_probability"] >= 0.80
    assert data["risk_level"] == "LOW"

def test_predict_renewal_absent_member():
    payload = {
        "customer_id": 2,
        "attendance_last_30d": 0,
        "days_since_last_visit": 25,
        "total_months_active": 1,
        "has_pt_package": False
    }
    res = client.post("/predict-renewal", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["renewal_probability"] <= 0.35
    assert data["risk_level"] == "HIGH"
    assert any("Absent" in f for f in data["key_factors"])

def test_renewal_action_urgent_for_high_risk():
    payload = {
        "customer_id": 10,
        "customer_name": "Nguyen Van A",
        "renewal_probability": 0.25,
        "days_since_last_visit": 20,
        "current_plan": "Move",
        "membership_expiry_days": 5
    }
    res = client.post("/renewal-action", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["action_type"] == "PERSONAL_CHECKIN"
    assert data["priority"] == "URGENT"

def test_suggest_upsell_move_to_perform():
    payload = {
        "customer_id": 20,
        "current_plan": "Move",
        "monthly_attendance": 10,
        "attended_classes_count": 0
    }
    res = client.post("/suggest-upsell", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["suggested_plan"] == "Perform"

def test_suggest_upsell_perform_to_pt_focus():
    payload = {
        "customer_id": 25,
        "current_plan": "Perform",
        "monthly_attendance": 14,
        "attended_classes_count": 12
    }
    res = client.post("/suggest-upsell", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["suggested_plan"] == "PT Focus"

def test_renewal_action_discount_near_expiry():
    payload = {
        "customer_id": 30,
        "customer_name": "Tran Van B",
        "renewal_probability": 0.65,
        "days_since_last_visit": 3,
        "current_plan": "Perform",
        "membership_expiry_days": 4
    }
    res = client.post("/renewal-action", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["action_type"] == "DISCOUNT_OFFER"
    assert data["priority"] == "NORMAL"
    assert "10%" in data["suggested_message"]

def test_predict_renewal_bounds():
    # Extreme worst case: probability must not go below 0.05
    res_worst = client.post("/predict-renewal", json={
        "customer_id": 99,
        "attendance_last_30d": 0,
        "days_since_last_visit": 100,
        "total_months_active": 1,
        "has_pt_package": False
    })
    assert res_worst.status_code == 200
    assert res_worst.json()["renewal_probability"] >= 0.05

    # Extreme best case: probability must not exceed 0.98
    res_best = client.post("/predict-renewal", json={
        "customer_id": 100,
        "attendance_last_30d": 30,
        "days_since_last_visit": 0,
        "total_months_active": 24,
        "has_pt_package": True
    })
    assert res_best.status_code == 200
    assert res_best.json()["renewal_probability"] <= 0.98

