import re
from typing import Dict, Any, List, Tuple
from schemas import LeadScoreResponse, ScoringFactor
from services.lead_context_loader import LeadContextLoader

class LeadScoringEngine:
    """
    Hybrid Lead Scoring:
    - Rule-based (0 to 100 max) assessing Profile, Engagement, Trial attendance & Budget.
    - NLP Intent detection from customer notes/inquiries.
    """

    @classmethod
    def score_lead(cls, lead_dict: Dict[str, Any]) -> LeadScoreResponse:
        lead_id = lead_dict.get("lead_id", 0)
        breakdown: List[ScoringFactor] = []
        total_score = 0.0

        # 1. Profile Completeness & Goals (Max 25 pts)
        goal = str(lead_dict.get("goal", "")).upper()
        budget = str(lead_dict.get("budget", "")).upper()
        profile_pts = 5.0  # base
        reasons = ["Cung cấp thông tin cơ bản"]

        if goal in ["WEIGHT_LOSS", "MUSCLE_GAIN", "REHAB", "STRENGTH"]:
            profile_pts += 10.0
            reasons.append(f"Mục tiêu rõ ràng ({goal})")
        if budget in ["HIGH", "PREMIUM"]:
            profile_pts += 10.0
            reasons.append(f"Ngân sách dồi dào ({budget})")
        elif budget == "MEDIUM":
            profile_pts += 6.0
            reasons.append("Ngân sách tầm trung")
        
        breakdown.append(ScoringFactor(
            name="Profile & Budget Alignment",
            points=min(profile_pts, 25.0),
            max_points=25.0,
            reason="; ".join(reasons)
        ))
        total_score += min(profile_pts, 25.0)

        # 2. Activity & Engagement Frequency (Max 25 pts)
        touchpoints = lead_dict.get("total_touchpoints", 0)
        consultation_count = lead_dict.get("consultation_count", 0)
        eng_pts = 0.0
        eng_reasons = []

        if touchpoints >= 3:
            eng_pts += 15.0
            eng_reasons.append(f"Tương tác tích cực ({touchpoints} lượt)")
        elif touchpoints >= 1:
            eng_pts += 8.0
            eng_reasons.append(f"Có tương tác ({touchpoints} lượt)")
        
        if consultation_count >= 1:
            eng_pts += 10.0
            eng_reasons.append("Đã tham gia tư vấn trực tiếp 1-1")

        breakdown.append(ScoringFactor(
            name="Engagement & Touchpoints",
            points=min(eng_pts, 25.0),
            max_points=25.0,
            reason="; ".join(eng_reasons) if eng_reasons else "Chưa có nhiều tương tác"
        ))
        total_score += min(eng_pts, 25.0)

        # 3. High-Value Action: Trial & Direct Experience (Max 30 pts)
        trial_attended = lead_dict.get("trial_attended", False)
        trial_no_show = lead_dict.get("trial_no_show", False)
        trial_pts = 0.0
        trial_reason = "Chưa đăng ký hoặc chưa trải nghiệm tập thử"

        if trial_attended:
            trial_pts = 30.0
            trial_reason = "Đã đến trải nghiệm tập thử thực tế tại phòng gym (Tín hiệu chuyển đổi cực cao)"
        elif trial_no_show:
            trial_pts = 5.0
            trial_reason = "Đã đặt lịch tập thử nhưng không đến (Cần chăm sóc lại lịch)"
        elif touchpoints > 0:
            trial_pts = 10.0
            trial_reason = "Đang trong phễu tìm hiểu trải nghiệm"

        breakdown.append(ScoringFactor(
            name="Trial Experience",
            points=trial_pts,
            max_points=30.0,
            reason=trial_reason
        ))
        total_score += trial_pts

        # 4. NLP Intent & Sentiment Detection from Notes (Max 20 pts)
        notes = lead_dict.get("notes_combined", "").lower()
        nlp_pts, intent_label, nlp_reason = cls._analyze_nlp_intent(notes, lead_dict)

        breakdown.append(ScoringFactor(
            name="Intent & Sentiment Analysis",
            points=nlp_pts,
            max_points=20.0,
            reason=nlp_reason
        ))
        total_score += nlp_pts

        final_score = int(round(min(max(total_score, 0), 100)))

        # Tier assignment
        if final_score >= 70:
            tier = "HOT"
            summary = "Khách hàng tiềm năng rất nóng! Nên liên hệ chốt gói ngay trong 24 giờ."
        elif final_score >= 35:
            tier = "WARM"
            summary = "Khách hàng quan tâm tích cực. Cần tư vấn giải tỏa thắc mắc về giá hoặc thời gian."
        else:
            tier = "COLD"
            summary = "Khách hàng mới ở giai đoạn tham khảo. Giữ liên hệ và gửi voucher dùng thử."

        return LeadScoreResponse(
            lead_id=lead_id,
            score=final_score,
            tier=tier,
            intent=intent_label,
            breakdown=breakdown,
            summary=summary
        )

    @classmethod
    def _analyze_nlp_intent(cls, notes: str, context: Dict[str, Any]) -> Tuple[float, str, str]:
        high_intent_keywords = [
            "mua", "đăng ký", "gói năm", "chuyển khoản", "tập ngay",
            "giá bao nhiêu", "thanh toán", "pt kèm", "kèm riêng", "gói pt"
        ]
        price_keywords = ["đắt", "giảm giá", "voucher", "khuyến mãi", "ưu đãi", "bớt", "chiết khấu"]
        hesitant_keywords = ["suy nghĩ", "bận", "hỏi ý kiến", "từ từ", "chưa thu xếp", "công tác"]

        points = 8.0  # neutral intent
        intent = "EXPLORING"
        reason = "Đang tìm hiểu thông tin tổng quan"

        has_high_intent = any(k in notes for k in high_intent_keywords)
        has_hesitant = any(k in notes for k in hesitant_keywords)
        has_price_intent = any(k in notes for k in price_keywords)

        if has_high_intent:
            points += 12.0
            intent = "HIGH_PURCHASE_INTENT"
            reason = "Từ khóa thể hiện ý định đăng ký hoặc mua gói tập rõ rệt"
        elif has_hesitant:
            points = 2.0
            intent = "LOW_ENGAGEMENT"
            reason = "Khách hàng còn ngần ngại hoặc gặp cản trở về thời gian/gia đình"
        elif has_price_intent:
            points += 5.0
            intent = "PRICE_SHOPPING"
            reason = "Khách quan tâm đến chi phí, săn ưu đãi hoặc chương trình khuyến mãi"

        return min(points, 20.0), intent, reason

