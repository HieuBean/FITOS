"""Pydantic schemas cho service ai-operations (port 8003)."""

from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field


class NoShowRequest(BaseModel):
    """Đặc trưng đầu vào cho bài toán dự đoán no-show (task #111)."""

    session_id: Optional[int] = None
    customer_id: Optional[int] = None
    days_ahead: int = Field(default=0, ge=0, le=90, description="Số ngày từ hôm nay tới buổi học")
    hour_of_day: int = Field(default=18, ge=0, le=23, description="Giờ bắt đầu buổi học")
    weekday: int = Field(default=1, ge=1, le=7, description="ISO weekday: 1=Thứ 2, 7=Chủ nhật")
    prior_bookings: int = Field(default=0, ge=0, description="Tổng số lượt đặt trước đây")
    prior_no_shows: int = Field(default=0, ge=0, description="Số lượt no-show trước đây")
    capacity: int = Field(default=1, ge=1)
    booked_count: int = Field(default=0, ge=0)


class NoShowFactor(BaseModel):
    name: str
    impact: float
    detail: str


class NoShowResponse(BaseModel):
    probability: float = Field(ge=0.0, le=1.0)
    risk_level: str
    reason: str
    factors: List[NoShowFactor] = []
    model: str = "rule-v1"
    engine: str = "deterministic"


class ScheduleSession(BaseModel):
    session_id: int
    class_id: int
    class_name: str
    start_time: str
    capacity: int = Field(ge=1)
    booked_count: int = Field(ge=0)
    date: Optional[str] = None
    trainer_id: Optional[int] = None
    room: Optional[str] = None


class ScheduleMatrixOut(BaseModel):
    total_sessions: int = 0
    total_capacity: int = 0
    total_booked: int = 0
    occupancy_rate: float = 0.0
    by_weekday: dict = {}
    by_hour: dict = {}
    golden_gaps: List[str] = []
    trainer_load: dict = {}
    underfilled: List[dict] = []
    overbooked: List[dict] = []


class ScheduleRequest(BaseModel):
    date: str = Field(description="Ngày cần gợi ý, định dạng YYYY-MM-DD")
    sessions: List[ScheduleSession] = []


class SlotSuggestion(BaseModel):
    slot: str
    class_id: Optional[int] = None
    class_name: str = ""
    reason: str = ""
    priority: float = Field(default=0.5, ge=0.0, le=1.0)


class ScheduleResponse(BaseModel):
    date: str
    suggestions: List[SlotSuggestion]
    engine: str = "deterministic"
    matrix: Optional[ScheduleMatrixOut] = None


class CustomerProfile(BaseModel):
    customer_id: Optional[int] = None
    full_name: Optional[str] = None
    fitness_goal: Optional[str] = None
    attendance_last_30_days: int = Field(default=0, ge=0)
    no_shows_last_30_days: int = Field(default=0, ge=0)


class TrainerTipsRequest(BaseModel):
    customer: CustomerProfile


class TrainerTipsResponse(BaseModel):
    tips: List[str]
    engine: str = "deterministic"


class HealthResponse(BaseModel):
    status: str
    service: str
    port: int
    llm_configured: bool
