"""FITOS AI Operations Service — task #110, #111, #112, #113.

Microservice tất định cho vận hành studio: dự đoán no-show, gợi ý lấp lịch,
và coaching tips cho huấn luyện viên. LLM chỉ là lớp bổ sung tuỳ chọn; mọi
endpoint đều trả kết quả hợp lệ khi không có OPENAI_API_KEY.
"""

from __future__ import annotations

import logging
import os

from fastapi import Depends, FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

import llm
from noshow import predict_no_show
from scheduling import suggest_slots
from schemas import (
    CustomerProfile,
    HealthResponse,
    NoShowRequest,
    NoShowResponse,
    ScheduleRequest,
    ScheduleResponse,
    TrainerTipsRequest,
    TrainerTipsResponse,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ai-operations")

PORT = int(os.getenv("PORT", "8003"))

app = FastAPI(
    title="FITOS AI Operations Service",
    description="Dự đoán no-show, gợi ý lịch và coaching tips cho vận hành phòng gym.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

security = HTTPBearer()


def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)) -> str:
    expected = os.getenv("AI_SERVICE_TOKEN", "fitos-ai-token")
    if credentials.credentials != expected:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN,
                            detail="Forbidden: Invalid FITOS AI Service Token")
    return credentials.credentials


def _fallback_tips(profile: CustomerProfile) -> list[str]:
    tips: list[str] = []
    attendance = profile.attendance_last_30_days
    no_shows = profile.no_shows_last_30_days

    if attendance == 0:
        tips.append("Hội viên chưa điểm danh trong 30 ngày — gọi lại và xếp một buổi làm quen ngay tuần này.")
    elif attendance >= 12:
        tips.append(f"Tần suất rất tốt ({attendance} buổi/30 ngày) — tăng khối lượng 5-10% và thêm 1 buổi biến thể.")
    elif attendance >= 6:
        tips.append(f"Duy trì {attendance} buổi/30 ngày — đặt mục tiêu thêm 1 buổi mỗi tuần để đạt ngưỡng thích nghi.")
    else:
        tips.append(f"Chỉ {attendance} buổi/30 ngày — chốt lịch cố định 2 buổi/tuần để tạo thói quen.")

    if no_shows >= 2:
        tips.append(f"Có {no_shows} lượt no-show gần đây — nhắc lịch trước 24 giờ và xác nhận qua Zalo/SMS.")
    else:
        tips.append("Tỉ lệ đi tập ổn định — tiếp tục nhắc lịch tự động trước 24 giờ.")

    if profile.fitness_goal:
        tips.append(f"Bám sát mục tiêu {profile.fitness_goal}: đánh giá lại giáo án mỗi 4 tuần và ghi log buổi tập.")
    else:
        tips.append("Chưa có mục tiêu rõ ràng — đặt mục tiêu SMART trong buổi tập tới.")

    return tips[:3]


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(status="healthy", service="ai-operations", port=PORT,
                          llm_configured=llm.is_configured())


@app.post("/predict-noshow", response_model=NoShowResponse, dependencies=[Depends(verify_token)])
async def predict_noshow(request: NoShowRequest) -> NoShowResponse:
    """Task #111: xác suất 0..1 hội viên không đến, kèm yếu tố giải thích."""
    return predict_no_show(request)


@app.post("/suggest-schedule", response_model=ScheduleResponse, dependencies=[Depends(verify_token)])
async def suggest_schedule(request: ScheduleRequest) -> ScheduleResponse:
    """Task #112: gợi ý lấp chỗ trống / mở thêm suất cho một ngày."""
    suggestions = suggest_slots(request.date, request.sessions)
    return ScheduleResponse(date=request.date, suggestions=suggestions, engine="deterministic")


@app.post("/trainer-tips", response_model=TrainerTipsResponse, dependencies=[Depends(verify_token)])
async def trainer_tips(request: TrainerTipsRequest) -> TrainerTipsResponse:
    """Task #113: lời khuyên coaching cho huấn luyện viên theo tiến độ hội viên."""
    profile = request.customer
    context = (
        f"Hội viên: {profile.full_name or 'ẩn danh'}\n"
        f"Mục tiêu: {profile.fitness_goal or 'chưa rõ'}\n"
        f"Điểm danh 30 ngày: {profile.attendance_last_30_days}\n"
        f"No-show 30 ngày: {profile.no_shows_last_30_days}\n"
        "Hãy đưa 3 lời khuyên ngắn, cụ thể cho huấn luyện viên."
    )
    enriched = await llm.enrich_tips(context)
    if enriched:
        return TrainerTipsResponse(tips=enriched, engine="llm")
    return TrainerTipsResponse(tips=_fallback_tips(profile), engine="deterministic")
