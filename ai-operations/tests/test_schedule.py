"""Kiểm thử gợi ý lịch và coaching tips."""

import os
import sys

from fastapi.testclient import TestClient

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from main import app  # noqa: E402
from scheduling import suggest_slots  # noqa: E402
from schemas import CustomerProfile, ScheduleSession  # noqa: E402

client = TestClient(app)
AUTH = {"Authorization": "Bearer fitos-ai-token"}


def _session(session_id, start, capacity, booked, name="HIIT"):
    return ScheduleSession(session_id=session_id, class_id=session_id, class_name=name,
                           start_time=start, capacity=capacity, booked_count=booked)


def test_empty_day_suggests_prime_slot():
    suggestions = suggest_slots("2026-01-15", [])
    assert len(suggestions) == 1
    assert suggestions[0].slot == "18:00"
    assert suggestions[0].priority > 0.5


def test_underfilled_classes_rank_first():
    suggestions = suggest_slots("2026-01-15", [
        _session(1, "07:00", 20, 2),
        _session(2, "18:00", 20, 18),
        _session(3, "12:00", 10, 1),
    ])
    assert suggestions
    assert suggestions[0].priority >= suggestions[-1].priority
    assert all(item.priority <= 1.0 for item in suggestions)


def test_full_class_recommends_extra_slot():
    suggestions = suggest_slots("2026-01-15", [_session(5, "19:00", 10, 10)])
    assert len(suggestions) == 1
    assert "mở thêm suất" in suggestions[0].reason


def test_schedule_endpoint_contract():
    response = client.post("/suggest-schedule", headers=AUTH, json={
        "date": "2026-01-15",
        "sessions": [
            {"session_id": 1, "class_id": 1, "class_name": "Yoga", "start_time": "08:00",
             "capacity": 10, "booked_count": 1},
        ],
    })
    assert response.status_code == 200
    data = response.json()
    assert data["date"] == "2026-01-15"
    assert data["engine"] == "deterministic"
    assert data["suggestions"][0]["class_name"] == "Yoga"


def test_trainer_tips_fallback_without_llm():
    response = client.post("/trainer-tips", headers=AUTH, json={
        "customer": {"customer_id": 1, "full_name": "Nguyen Van A", "fitness_goal": "WEIGHT_LOSS",
                     "attendance_last_30_days": 0, "no_shows_last_30_days": 3},
    })
    assert response.status_code == 200
    data = response.json()
    assert data["engine"] in {"deterministic", "llm"}
    assert 1 <= len(data["tips"]) <= 3


def test_trainer_tips_mentions_absence():
    response = client.post("/trainer-tips", headers=AUTH, json={
        "customer": CustomerProfile(attendance_last_30_days=0, no_shows_last_30_days=0).model_dump(),
    })
    tips = " ".join(response.json()["tips"]).lower()
    assert "chưa điểm danh" in tips or "no-show" in tips
