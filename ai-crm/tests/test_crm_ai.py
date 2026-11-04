import pytest
from main import health_check, score_lead, suggest_followup, get_conversion_tips, verify_token
from schemas import (
    LeadProfile, ActivityItem,
    LeadScoreRequest, FollowUpRequest, ConversionTipsRequest
)
from services.lead_context_loader import LeadContextLoader
from services.scoring_engine import LeadScoringEngine
from services.followup_recommender import FollowUpRecommender
from fastapi import HTTPException

# =========================================================================
# 1. HEALTH & SECURITY TESTS
# =========================================================================
def test_health_check_payload():
    data = health_check()
    assert data["status"] == "UP"
    assert data["service"] == "ai-crm"
    assert data["port"] == 8001
    assert data["version"] == "1.0.0"

def test_verify_token_valid():
    assert verify_token("Bearer crm-secret-ai-token-2026") is True
    assert verify_token(None) is True

def test_verify_token_invalid():
    with pytest.raises(HTTPException) as exc_info:
        verify_token("Bearer invalid-token-xyz")
    assert exc_info.value.status_code == 401
    assert "Invalid or missing Bearer token" in exc_info.value.detail


# =========================================================================
# 2. CONTEXT LOADER ISOLATION & EDGE CASES
# =========================================================================
def test_context_loader_empty_lead():
    lead = LeadProfile(id=99, name="Test Empty")
    context = LeadContextLoader.load_context(lead)
    assert context["lead_id"] == 99
    assert context["total_touchpoints"] == 0
    assert context["call_count"] == 0
    assert context["trial_attended"] is False
    assert context["price_objection"] is False

def test_context_loader_rich_history():
    lead = LeadProfile(
        id=1,
        name="Nguyễn Văn A",
        fitness_goal="MUSCLE_GAIN",
        budget_range="HIGH",
        activities=[
            ActivityItem(activity_type="CALL", outcome="INTERESTED", notes="Khách muốn tăng cơ"),
            ActivityItem(activity_type="CONSULTATION", outcome="PRICE_SENSITIVE", notes="Bảo giá hơi đắt"),
            ActivityItem(activity_type="TRIAL", outcome="ATTENDED", notes="Tập thử rất hào hứng, hỏi giá gói năm"),
            ActivityItem(activity_type="MESSAGE", outcome="BUSY", notes="Bận đi công tác 2 ngày")
        ]
    )
    context = LeadContextLoader.load_context(lead)
    assert context["total_touchpoints"] == 4
    assert context["call_count"] == 1
    assert context["consultation_count"] == 1
    assert context["trial_attended"] is True
    assert context["price_objection"] is True
    assert context["time_objection"] is True


# =========================================================================
# 3. SCORING ENGINE TESTS (RULE-BASED + NLP INTENT)
# =========================================================================
def test_hybrid_scoring_hot_tier():
    lead = LeadProfile(
        id=2,
        name="Trần Thị B",
        fitness_goal="WEIGHT_LOSS",
        budget_range="PREMIUM",
        activities=[
            ActivityItem(activity_type="CONSULTATION", outcome="INTERESTED", notes="Đã đo InBody mỡ 28%"),
            ActivityItem(activity_type="TRIAL", outcome="ATTENDED", notes="Tập thử xong muốn mua gói năm kèm PT ngay chuyển khoản"),
        ]
    )
    context = LeadContextLoader.load_context(lead)
    res = LeadScoringEngine.score_lead(context)

    assert 75 <= res.score <= 100
    assert res.tier == "HOT"
    assert res.intent == "HIGH_PURCHASE_INTENT"
    assert len(res.breakdown) == 4
    for b in res.breakdown:
        assert b.points <= b.max_points

def test_hybrid_scoring_warm_tier():
    lead = LeadProfile(
        id=4,
        name="Phạm Văn D",
        fitness_goal="GENERAL_FITNESS",
        budget_range="MEDIUM",
        activities=[
            ActivityItem(activity_type="CALL", outcome="INTERESTED", notes="Đang tham khảo phòng tập"),
            ActivityItem(activity_type="MESSAGE", outcome="INTERESTED", notes="Hỏi xem có khuyến mãi tháng này không")
        ]
    )
    context = LeadContextLoader.load_context(lead)
    res = LeadScoringEngine.score_lead(context)

    assert 40 <= res.score <= 74
    assert res.tier == "WARM"
    assert res.intent == "PRICE_SHOPPING"

def test_hybrid_scoring_cold_tier():
    lead = LeadProfile(
        id=3,
        name="Lê Văn C",
        fitness_goal="GENERAL_FITNESS",
        budget_range="LOW",
        activities=[
            ActivityItem(activity_type="CALL", outcome="BUSY", notes="Khách bận, bảo suy nghĩ thêm và chê đắt")
        ]
    )
    context = LeadContextLoader.load_context(lead)
    res = LeadScoringEngine.score_lead(context)

    assert 0 <= res.score < 45
    assert res.tier == "COLD"
    assert res.intent in ["PRICE_SHOPPING", "LOW_ENGAGEMENT"]


# =========================================================================
# 4. PERSONA GENERATION & FOLLOW-UP TESTS
# =========================================================================
def test_followup_high_value_achiever_persona():
    lead = LeadProfile(
        id=20,
        name="Đặng Tuấn",
        fitness_goal="MUSCLE_GAIN",
        budget_range="PREMIUM",
        activities=[
            ActivityItem(activity_type="TRIAL", outcome="ATTENDED", notes="Tập thử buổi đầu")
        ]
    )
    context = LeadContextLoader.load_context(lead)
    res = FollowUpRecommender.generate_followup(context, target_package="1_YEAR_PT", tone="PROFESSIONAL")
    assert "High-Value Achiever" in res.persona
    assert "Đặng Tuấn" in res.call_script
    assert "InBody" in res.call_script
    assert res.recommended_channel != ""

def test_followup_price_conscious_persona():
    lead = LeadProfile(
        id=21,
        name="Ngọc Lan",
        budget_range="LOW",
        activities=[
            ActivityItem(activity_type="CALL", outcome="PRICE_SENSITIVE", notes="Hỏi có giảm giá voucher không")
        ]
    )
    context = LeadContextLoader.load_context(lead)
    res = FollowUpRecommender.generate_followup(context, target_package="3_MONTHS", tone="FRIENDLY")
    assert "Value-Conscious" in res.persona
    assert "Voucher" in res.zalo_sms_script or "ưu đãi" in res.call_script

def test_followup_busy_professional_persona():
    lead = LeadProfile(
        id=22,
        name="Quốc Huy",
        activities=[
            ActivityItem(activity_type="CALL", outcome="BUSY", notes="Rất bận đi công tác")
        ]
    )
    context = LeadContextLoader.load_context(lead)
    res = FollowUpRecommender.generate_followup(context, target_package="FLEXIBLE", tone="CONCISE")
    assert "Busy Professional" in res.persona
    assert "bảo lưu" in res.call_script or "bảo lưu" in res.zalo_sms_script


# =========================================================================
# 5. OBJECTION HANDLING & CONVERSION TIPS TESTS
# =========================================================================
def test_conversion_tips_price_and_time():
    res = FollowUpRecommender.generate_conversion_tips(
        lead_id=30,
        objections=["Giá hơi chát", "Tôi chưa sắp xếp được thời gian"],
        fitness_goal="WEIGHT_LOSS",
        budget_range="MEDIUM"
    )
    assert res.lead_id == 30
    assert len(res.tips) == 2
    assert any("Chi phí" in t.handling_technique for t in res.tips)
    assert any("Lịch linh hoạt" in t.handling_technique for t in res.tips)
    assert "WEIGHT_LOSS" in res.closing_hook

def test_conversion_tips_unmapped_fallback():
    res = FollowUpRecommender.generate_conversion_tips(
        lead_id=31,
        objections=["Để tôi cân nhắc việc khác"],
        fitness_goal="STRENGTH",
        budget_range="HIGH"
    )
    assert len(res.tips) == 1
    assert "thấu cảm" in res.tips[0].handling_technique


# =========================================================================
# 6. FASTAPI DIRECT ENDPOINT INVOCATION TESTS
# =========================================================================
def test_endpoint_score_lead():
    req = LeadScoreRequest(
        lead=LeadProfile(
            id=101,
            name="Test API Lead",
            fitness_goal="WEIGHT_LOSS",
            budget_range="HIGH",
            activities=[
                ActivityItem(activity_type="TRIAL", outcome="ATTENDED", notes="Khách muốn giảm 5kg mỡ")
            ]
        )
    )
    res = score_lead(req)
    assert res.lead_id == 101
    assert 0 <= res.score <= 100
    assert res.tier in ["HOT", "WARM", "COLD"]

def test_endpoint_suggest_followup():
    req = FollowUpRequest(
        lead=LeadProfile(
            id=102,
            name="Test Followup Lead"
        )
    )
    res = suggest_followup(req)
    assert res.lead_id == 102
    assert res.persona != ""
    assert res.call_script != ""

def test_endpoint_conversion_tips():
    req = ConversionTipsRequest(
        lead_id=103,
        objections=["Giá đắt quá", "Gia đình chưa đồng ý"],
        fitness_goal="GENERAL_FITNESS"
    )
    res = get_conversion_tips(req)
    assert res.lead_id == 103
    assert len(res.tips) == 2
