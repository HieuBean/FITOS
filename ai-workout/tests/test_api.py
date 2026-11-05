import pytest
import sys
import os
from unittest.mock import AsyncMock, MagicMock, patch
from fastapi.testclient import TestClient

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from main import app
from schemas import FitnessProfile, BodyMetrics, ChatMessage

client = TestClient(app)
VALID_TOKEN = "fitos-ai-token"
AUTH_HEADER = {"Authorization": f"Bearer {VALID_TOKEN}"}


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "ai-workout"
    assert data["port"] == 8004


def test_auth_rejection():
    # Không có header Authorization -> FastAPI HTTPBearer trả về 401
    res_no_auth = client.post("/safety-check", json={"message": "hello"})
    assert res_no_auth.status_code == 401

    # Token sai -> verify_token trả về 403
    res_wrong_token = client.post(
        "/safety-check",
        json={"message": "hello"},
        headers={"Authorization": "Bearer wrong-secret-token"}
    )
    assert res_wrong_token.status_code == 403


def test_safety_check_endpoint():
    # Câu hỏi hợp lệ
    res_safe = client.post(
        "/safety-check",
        json={"message": "Hôm nay tôi tập squat và lunge"},
        headers=AUTH_HEADER
    )
    assert res_safe.status_code == 200
    assert res_safe.json()["is_safe"] is True

    # Câu hỏi vi phạm chất cấm
    res_unsafe = client.post(
        "/safety-check",
        json={"message": "Tôi muốn tiêm steroid và anavar để tăng cơ nhanh"},
        headers=AUTH_HEADER
    )
    assert res_unsafe.status_code == 200
    data = res_unsafe.json()
    assert data["is_safe"] is False
    assert data["violation_type"] == "BANNED_SUBSTANCE"
    assert "anavar" in data["flagged_terms"]


def test_chat_blocked_by_safety():
    payload = {
        "message": "Có nên dùng trenbolone kết hợp clen để cắt nét không?",
        "fitness_profile": {
            "user_id": 1,
            "full_name": "Lê Văn Hùng"
        }
    }
    response = client.post("/chat", json=payload, headers=AUTH_HEADER)
    assert response.status_code == 200
    data = response.json()
    assert data["is_safe"] is False
    assert "CẢNH BÁO AN TOÀN Y TẾ" in data["reply"]
    assert "tren" in data["safety_warning"] or "clen" in data["safety_warning"]


def test_chat_safe_with_deterministic_engine():
    # Kiểm tra kịch bản hỏi bài tập chân khi hội viên có lưu ý về lưng
    payload = {
        "message": "Hôm nay tập chân và mông thế nào coach?",
        "fitness_profile": {
            "user_id": 12,
            "full_name": "Phạm Minh Tuấn",
            "fitness_goal": "MUSCLE_GAIN",
            "injuries": ["Đau thắt lưng khi khuân vác"],
            "fatigue_level": 3,
            "body_metrics": {
                "weight_kg": 70.0,
                "height_cm": 175.0
            }
        }
    }
    with patch("main.gemini_api_key", None), patch("main.openai_client", None):
        response = client.post("/chat", json=payload, headers=AUTH_HEADER)
        assert response.status_code == 200
        data = response.json()
        assert data["is_safe"] is True
        assert "Goblet Squat" in data["reply"]
        assert "thắt lưng" in data["reply"]
        assert len(data["suggested_actions"]) > 0
        assert data["context_used"]["bmi"] == 22.9


def test_chat_with_mocked_openai():
    # Giả lập phản hồi từ OpenAI API khi có configured client
    fake_completion = MagicMock()
    fake_choice = MagicMock()
    fake_choice.message.content = "HLV FITOS: Chào bạn! Hãy khởi động kỹ khớp háng và khớp gối 10 phút nhé!"
    fake_completion.choices = [fake_choice]

    mock_client = AsyncMock()
    mock_client.chat.completions.create = AsyncMock(return_value=fake_completion)

    payload = {
        "message": "Hướng dẫn khởi động trước khi tập",
        "fitness_profile": {
            "user_id": 10,
            "full_name": "Hoàng Nam"
        }
    }

    with patch("main.gemini_api_key", None), patch("main.openai_client", mock_client):
        response = client.post("/chat", json=payload, headers=AUTH_HEADER)
        assert response.status_code == 200
        data = response.json()
        assert data["is_safe"] is True
        assert "HLV FITOS" in data["reply"]
        assert mock_client.chat.completions.create.called


def test_chat_with_mocked_gemini():
    # Giả lập phản hồi từ Google Gemini API
    mock_gemini_func = AsyncMock(return_value="Gemini HLV FITOS: Hãy tập trung vào nhịp thở và gồng cơ bụng.")

    payload = {
        "message": "Tư vấn nhịp thở khi squat",
        "fitness_profile": {
            "user_id": 11,
            "full_name": "Trần Tuấn"
        }
    }

    with patch("main.gemini_api_key", "test-gemini-key"), patch("main.call_gemini_native", mock_gemini_func):
        response = client.post("/chat", json=payload, headers=AUTH_HEADER)
        assert response.status_code == 200
        data = response.json()
        assert data["is_safe"] is True
        assert "Gemini HLV FITOS" in data["reply"]
        assert mock_gemini_func.called


def test_recommend_workout_adjustments_for_back_injury(monkeypatch):
    monkeypatch.setattr("main.gemini_api_key", None)
    monkeypatch.setattr("main.openai_client", None)
    payload = {
        "current_exercises": [{"name": "Barbell Back Squat"}, {"name": "Barbell Bent-over Row"}],
        "current_plan_name": "Giáo án Tăng cơ Chân - Lưng",
        "feedback_or_pain_note": "Cảm thấy nhói nhẹ ở lưng dưới sau buổi deadlift",
        "fitness_profile": {
            "user_id": 99,
            "full_name": "Đặng Quốc Bảo",
            "injuries": ["Mỏi cơ thắt lưng"],
            "body_metrics": {
                "weight_kg": 68.0,
                "height_cm": 172.0
            }
        }
    }

    response = client.post("/recommend", json=payload, headers=AUTH_HEADER)
    assert response.status_code == 200
    data = response.json()
    assert 0 <= data["confidence_score"] <= 1
    assert "lưng" in data["analysis"]
    assert len(data["suggested_changes"]) >= 2
    
    # Kiểm tra thay thế Barbell Back Squat -> Goblet Squat
    squat_change = next(c for c in data["suggested_changes"] if "Squat" in c["exercise_name"])
    assert squat_change["action"] == "SUBSTITUTE"
    assert "Goblet Squat" in squat_change["replacement_or_modification"]


def test_recommend_workout_adjustments_for_knee_injury(monkeypatch):
    monkeypatch.setattr("main.gemini_api_key", None)
    monkeypatch.setattr("main.openai_client", None)
    payload = {
        "current_exercises": [{"name": "Leg Extension"}, {"name": "Barbell Squat"}],
        "current_plan_name": "Giáo án Hạ Trọng Tâm Chân Đùi",
        "feedback_or_pain_note": "Hơi đau nhói khớp gối khi ngồi xổm sâu",
        "fitness_profile": {
            "user_id": 88,
            "full_name": "Ngô Mai Anh",
            "injuries": ["Đau khớp gối"],
            "body_metrics": {
                "weight_kg": 52.0,
                "height_cm": 160.0
            }
        }
    }

    response = client.post("/recommend", json=payload, headers=AUTH_HEADER)
    assert response.status_code == 200
    data = response.json()
    assert "gối" in data["analysis"]
    leg_ext_change = next(c for c in data["suggested_changes"] if "Leg Extension" in c["exercise_name"])
    assert "Romanian Deadlift" in leg_ext_change["replacement_or_modification"]


def test_recommend_without_plan_does_not_invent_exercises(monkeypatch):
    monkeypatch.setattr("main.gemini_api_key", None)
    monkeypatch.setattr("main.openai_client", None)
    response = client.post("/recommend", json={"fitness_profile": {"user_id": 42}}, headers=AUTH_HEADER)
    assert response.status_code == 200
    assert response.json()["suggested_changes"] == []
    assert response.json()["confidence_score"] == 0.0
