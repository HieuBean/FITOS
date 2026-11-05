"""Rule engine dự đoán khả năng hội viên không đến buổi học (task #111).

Thuần túy tất định để có thể kiểm thử và giải thích được. Mọi hệ số nằm trong một
chỗ, kèm giải thích, để dễ hiệu chỉnh khi có dữ liệu thật.
"""

from __future__ import annotations

from schemas import NoShowFactor, NoShowRequest, NoShowResponse

# Hệ số ảnh hưởng tới xác suất, cộng dồn rồi kẹp về [0, 0.95].
BASE_PROBABILITY = 0.15
HISTORY_WEIGHT = 0.40          # trọng số tối đa cho lịch sử no-show
MAX_DAYS_AHEAD_BONUS = 0.15
LOW_FILL_BONUS = 0.05


def _days_ahead_bonus(days_ahead: int) -> tuple[float, str]:
    if days_ahead <= 1:
        return -0.05, "Đặt sát giờ nên ít khả năng quên"
    if days_ahead <= 3:
        return 0.0, "Đặt trước 2-3 ngày"
    if days_ahead <= 7:
        return 0.08, "Đặt trước gần một tuần"
    return MAX_DAYS_AHEAD_BONUS, "Đặt trước hơn một tuần — dễ quên hoặc đổi kế hoạch"


def _hour_bonus(hour: int) -> tuple[float, str]:
    if hour < 7:
        return 0.06, "Khung giờ sáng sớm dễ vắng"
    if hour <= 9:
        return 0.03, "Khung giờ đi làm buổi sáng"
    if 12 <= hour <= 14:
        return 0.02, "Khung nghỉ trưa"
    if hour >= 20:
        return 0.05, "Khung tối muộn dễ huỷ"
    return 0.0, "Khung giờ tập trung đông"


def _weekday_bonus(weekday: int) -> tuple[float, str]:
    if weekday == 1:
        return 0.03, "Đầu tuần thường bận việc"
    if weekday >= 6:
        return -0.04, "Cuối tuần dễ sắp xếp hơn"
    return 0.0, "Ngày giữa tuần ổn định"


def predict_no_show(request: NoShowRequest) -> NoShowResponse:
    factors: list[NoShowFactor] = []

    probability = BASE_PROBABILITY
    factors.append(NoShowFactor(name="base", impact=BASE_PROBABILITY, detail="Mức nền của studio"))

    if request.prior_bookings > 0:
        rate = min(1.0, request.prior_no_shows / request.prior_bookings)
        impact = HISTORY_WEIGHT * rate
        probability += impact
        factors.append(
            NoShowFactor(
                name="history",
                impact=round(impact, 3),
                detail=f"{request.prior_no_shows}/{request.prior_bookings} lượt trước đây là no-show",
            )
        )

    days_impact, days_detail = _days_ahead_bonus(request.days_ahead)
    probability += days_impact
    factors.append(NoShowFactor(name="days_ahead", impact=days_impact, detail=days_detail))

    hour_impact, hour_detail = _hour_bonus(request.hour_of_day)
    probability += hour_impact
    factors.append(NoShowFactor(name="hour_of_day", impact=hour_impact, detail=hour_detail))

    weekday_impact, weekday_detail = _weekday_bonus(request.weekday)
    probability += weekday_impact
    factors.append(NoShowFactor(name="weekday", impact=weekday_impact, detail=weekday_detail))

    fill = request.booked_count / request.capacity if request.capacity else 0.0
    if fill < 0.4:
        probability += LOW_FILL_BONUS
        factors.append(
            NoShowFactor(
                name="low_fill",
                impact=LOW_FILL_BONUS,
                detail=f"Lớp mới lấp {fill:.0%} sức chứa — cam kết thấp",
            )
        )

    probability = max(0.0, min(0.95, round(probability, 3)))
    risk_level = "HIGH" if probability >= 0.6 else "MEDIUM" if probability >= 0.35 else "LOW"

    top = sorted(factors, key=lambda item: abs(item.impact), reverse=True)[:3]
    reason = "; ".join(
        f"{item.detail} ({item.impact:+.2f})" for item in top if item.impact != 0
    ) or "Không có yếu tố rủi ro"

    return NoShowResponse(
        probability=probability,
        risk_level=risk_level,
        reason=reason,
        factors=factors,
        model="rule-v1",
        engine="deterministic",
    )
