"""Kiểm thử task #116 — schedule context loader dựng ma trận lịch tuần."""

import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from services.schedule_context_loader import (  # noqa: E402
    SessionRow,
    build_schedule_matrix,
    needs_more_coverage,
    summarize,
)


def _row(start, capacity, booked, date="2026-03-16", trainer=None, name="HIIT"):
    return SessionRow(
        session_id=1,
        class_id=1,
        class_name=name,
        start_time=start,
        capacity=capacity,
        booked_count=booked,
        date=date,
        trainer_id=trainer,
    )


def test_empty_week_has_no_occupancy():
    matrix = build_schedule_matrix([])
    assert matrix.total_sessions == 0
    assert matrix.occupancy_rate == 0.0
    assert matrix.golden_gaps  # toàn bộ giờ vàng còn trống


def test_occupancy_is_weighted_by_capacity():
    matrix = build_schedule_matrix([
        _row("18:00", 10, 10),   # kín
        _row("07:00", 30, 0),    # trống
    ])
    assert matrix.total_capacity == 40
    assert matrix.total_booked == 10
    assert matrix.occupancy_rate == 0.25


def test_weekday_buckets_are_split():
    matrix = build_schedule_matrix([
        _row("18:00", 10, 8, date="2026-03-16"),  # thứ 2
        _row("18:00", 10, 2, date="2026-03-21"),  # thứ 7
    ])
    assert set(matrix.by_weekday) == {"MON", "SAT"}
    assert matrix.by_weekday["MON"]["occupancy"] == 0.8
    assert matrix.by_weekday["SAT"]["occupancy"] == 0.2


def test_golden_gaps_exclude_busy_peak_hours():
    matrix = build_schedule_matrix([_row("18:00", 10, 5)])
    assert "18:00" not in matrix.golden_gaps
    assert "19:00" in matrix.golden_gaps


def test_underfilled_and_overbooked_lists():
    matrix = build_schedule_matrix([
        _row("09:00", 20, 2),    # 10% → underfilled
        _row("17:00", 10, 10),   # 100% → overbooked
    ])
    assert len(matrix.underfilled) == 1
    assert matrix.underfilled[0]["free_spots"] == 18
    assert len(matrix.overbooked) == 1


def test_trainer_load_counts_sessions():
    matrix = build_schedule_matrix([
        _row("07:00", 10, 1, trainer=7),
        _row("09:00", 10, 1, trainer=7),
        _row("11:00", 10, 1, trainer=9),
    ])
    assert matrix.trainer_load == {7: 2, 9: 1}


def test_needs_more_coverage_threshold():
    empty = build_schedule_matrix([])
    busy = build_schedule_matrix([_row(hour, 10, 10) for hour in range(6, 21)])
    assert needs_more_coverage(empty) is True
    assert needs_more_coverage(busy) is False


def test_summary_is_readable():
    matrix = build_schedule_matrix([_row("18:00", 10, 3)])
    text = summarize(matrix)
    assert "buổi/tuần" in text
    assert "30%" in text
