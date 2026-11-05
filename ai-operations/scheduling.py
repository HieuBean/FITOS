"""Heuristic gợi ý lấp chỗ trống cho lịch lớp học (task #112).

Xếp hạng các buổi học có tỉ lệ lấp chỗ thấp và đề xuất hành động cụ thể.
Kết quả tất định nên có thể so sánh trước/sau khi đổi chiến lược.
"""

from __future__ import annotations

from schemas import ScheduleSession, SlotSuggestion

PEAK_HOURS = range(17, 21)
UNDERFILLED_THRESHOLD = 0.5
MAX_SUGGESTIONS = 5


def _hour_of(start_time: str) -> int:
    try:
        return int(start_time.split(":")[0])
    except (ValueError, AttributeError, IndexError):
        return 12


def suggest_slots(date: str, sessions: list[ScheduleSession]) -> list[SlotSuggestion]:
    if not sessions:
        return [
            SlotSuggestion(
                slot="18:00",
                class_id=None,
                class_name="Khung giờ vàng còn trống",
                reason=(
                    "Ngày này chưa có buổi học nào — nên mở một lớp vào 18:00 "
                    "vì đây là giờ cao điểm."
                ),
                priority=0.9,
            )
        ]

    suggestions: list[SlotSuggestion] = []
    for session in sessions:
        fill = session.booked_count / session.capacity if session.capacity else 0.0
        free = max(0, session.capacity - session.booked_count)

        if fill < UNDERFILLED_THRESHOLD:
            peak = _hour_of(session.start_time) in PEAK_HOURS
            priority = round(min(0.95, (1 - fill) * (0.85 if peak else 0.7)), 3)
            reason = (
                f"Còn {free}/{session.capacity} chỗ ({fill:.0%} sức chứa)"
                + (
                    " — giờ cao điểm nên dễ lấp bằng thông báo đẩy."
                    if peak
                    else " — nên gộp hoặc đổi khung giờ."
                )
            )
            suggestions.append(
                SlotSuggestion(
                    slot=session.start_time,
                    class_id=session.class_id,
                    class_name=session.class_name,
                    reason=reason,
                    priority=priority,
                )
            )
        elif fill >= 0.9:
            suggestions.append(
                SlotSuggestion(
                    slot=session.start_time,
                    class_id=session.class_id,
                    class_name=session.class_name,
                    reason=(
                        f"Lớp đã kín {fill:.0%} — cân nhắc mở thêm suất "
                        "hoặc tăng sức chứa."
                    ),
                    priority=round(fill * 0.5, 3),
                )
            )

    suggestions.sort(key=lambda item: item.priority, reverse=True)
    return suggestions[:MAX_SUGGESTIONS]
