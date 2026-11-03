"""Task #116 — loader ngữ cảnh lịch cho bài toán tối ưu ca dạy (ai-operations).

Nhận dữ liệu thô từ backend Spring (danh sách buổi học trong tuần) rồi dựng
ma trận lịch: occupancy theo ngày/khung giờ, tỉ lệ lấp chỗ toàn tuần, các
"giờ vàng" còn trống và mức tải của từng huấn luyện viên. Toàn bộ logic thuần
túy tất định để kiểm thử được và không phụ thuộc DB.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Iterable, Optional

PEAK_HOURS = frozenset(range(17, 21))
WEEKDAY_KEYS = ("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")


@dataclass(frozen=True)
class SessionRow:
    """Một buổi học như backend gửi sang."""

    session_id: int
    class_id: int
    class_name: str
    start_time: str
    capacity: int
    booked_count: int
    date: Optional[str] = None
    trainer_id: Optional[int] = None
    room: Optional[str] = None

    @property
    def hour(self) -> int:
        # Chấp nhận cả "18:00" và 18 để loader không vỡ khi backend đổi định dạng.
        try:
            return int(str(self.start_time).split(":")[0])
        except (ValueError, AttributeError, IndexError):
            return 12

    @property
    def fill_rate(self) -> float:
        if self.capacity <= 0:
            return 0.0
        return round(min(1.0, self.booked_count / self.capacity), 3)

    @property
    def free_spots(self) -> int:
        return max(0, self.capacity - self.booked_count)

    @property
    def is_peak(self) -> bool:
        return self.hour in PEAK_HOURS


@dataclass
class ScheduleMatrix:
    """Ma trận lịch tuần đã tổng hợp."""

    total_sessions: int = 0
    total_capacity: int = 0
    total_booked: int = 0
    occupancy_rate: float = 0.0
    by_weekday: dict[str, dict] = field(default_factory=dict)
    by_hour: dict[int, dict] = field(default_factory=dict)
    golden_gaps: list[str] = field(default_factory=list)
    trainer_load: dict[int, int] = field(default_factory=dict)
    underfilled: list[dict] = field(default_factory=list)
    overbooked: list[dict] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "total_sessions": self.total_sessions,
            "total_capacity": self.total_capacity,
            "total_booked": self.total_booked,
            "occupancy_rate": self.occupancy_rate,
            "by_weekday": self.by_weekday,
            "by_hour": self.by_hour,
            "golden_gaps": self.golden_gaps,
            "trainer_load": self.trainer_load,
            "underfilled": self.underfilled,
            "overbooked": self.overbooked,
        }


def _weekday_key(date: Optional[str]) -> str:
    """Đổi 'YYYY-MM-DD' sang MON..SUN; không có ngày thì gom vào UNKNOWN."""
    if not date:
        return "UNKNOWN"
    try:
        from datetime import date as date_cls

        year, month, day = (int(part) for part in date.split("-")[:3])
        return WEEKDAY_KEYS[date_cls(year, month, day).weekday()]
    except (ValueError, TypeError):
        return "UNKNOWN"


def build_schedule_matrix(sessions: Iterable[SessionRow]) -> ScheduleMatrix:
    """Tổng hợp danh sách buổi học thành ma trận lịch tuần."""
    rows = list(sessions)
    matrix = ScheduleMatrix(total_sessions=len(rows))

    for row in rows:
        matrix.total_capacity += row.capacity
        matrix.total_booked += row.booked_count

        weekday = _weekday_key(row.date)
        bucket = matrix.by_weekday.setdefault(
            weekday, {"sessions": 0, "capacity": 0, "booked": 0, "occupancy": 0.0}
        )
        bucket["sessions"] += 1
        bucket["capacity"] += row.capacity
        bucket["booked"] += row.booked_count

        hour_bucket = matrix.by_hour.setdefault(
            row.hour, {"sessions": 0, "capacity": 0, "booked": 0, "peak": row.is_peak}
        )
        hour_bucket["sessions"] += 1
        hour_bucket["capacity"] += row.capacity
        hour_bucket["booked"] += row.booked_count

        if row.trainer_id is not None:
            matrix.trainer_load[row.trainer_id] = matrix.trainer_load.get(row.trainer_id, 0) + 1

        if row.fill_rate < 0.5:
            matrix.underfilled.append({
                "session_id": row.session_id,
                "class_name": row.class_name,
                "start_time": row.start_time,
                "fill_rate": row.fill_rate,
                "free_spots": row.free_spots,
                "peak": row.is_peak,
            })
        elif row.fill_rate >= 0.9:
            matrix.overbooked.append({
                "session_id": row.session_id,
                "class_name": row.class_name,
                "start_time": row.start_time,
                "fill_rate": row.fill_rate,
            })

    if matrix.total_capacity > 0:
        matrix.occupancy_rate = round(matrix.total_booked / matrix.total_capacity, 3)

    for bucket in matrix.by_weekday.values():
        if bucket["capacity"]:
            bucket["occupancy"] = round(bucket["booked"] / bucket["capacity"], 3)
    for bucket in matrix.by_hour.values():
        if bucket["capacity"]:
            bucket["occupancy"] = round(bucket["booked"] / bucket["capacity"], 3)

    busy_hours = {hour for hour, bucket in matrix.by_hour.items() if bucket["sessions"] > 0}
    matrix.golden_gaps = [f"{hour:02d}:00" for hour in sorted(PEAK_HOURS - busy_hours)]

    matrix.underfilled.sort(key=lambda item: item["fill_rate"])
    matrix.overbooked.sort(key=lambda item: item["fill_rate"], reverse=True)
    return matrix


def needs_more_coverage(matrix: ScheduleMatrix, threshold: float = 0.45) -> bool:
    """Studio nên mở thêm lớp khi công suất tuần chưa dùng tới ngưỡng."""
    return matrix.occupancy_rate < threshold or bool(matrix.golden_gaps)


def summarize(matrix: ScheduleMatrix) -> str:
    """Câu tóm tắt ngắn để nhúng vào prompt LLM hoặc log."""
    parts = [
        f"{matrix.total_sessions} buổi/tuần, lấp {matrix.occupancy_rate:.0%} "
        f"({matrix.total_booked}/{matrix.total_capacity} chỗ)",
    ]
    if matrix.golden_gaps:
        parts.append("giờ vàng còn trống: " + ", ".join(matrix.golden_gaps))
    if matrix.underfilled:
        parts.append(f"{len(matrix.underfilled)} buổi dưới 50% sức chứa")
    if matrix.overbooked:
        parts.append(f"{len(matrix.overbooked)} buổi đã kín ≥90%")
    return "; ".join(parts)
