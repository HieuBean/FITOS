from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

class ChatMessage(BaseModel):
    role: str = Field(description="Role: 'user', 'assistant', or 'system'")
    content: str = Field(description="Content of the message")

class BodyMetrics(BaseModel):
    weight_kg: Optional[float] = None
    height_cm: Optional[float] = None
    body_fat_pct: Optional[float] = None
    recorded_at: Optional[str] = None

class FitnessProfile(BaseModel):
    user_id: Optional[int] = None
    full_name: Optional[str] = "Hội viên"
    age: Optional[int] = None
    gender: Optional[str] = None
    fitness_goal: Optional[str] = Field(default=None, description="WEIGHT_LOSS | MUSCLE_GAIN | STRENGTH | GENERAL_FITNESS | MOBILITY")
    fitness_level: Optional[str] = Field(default=None, description="BEGINNER | INTERMEDIATE | ADVANCED")
    injuries: List[str] = Field(default_factory=list, description="Danh sách chấn thương hiện tại (ví dụ: 'đau thắt lưng', 'viêm khớp gối')")
    medical_clearance: Optional[bool] = Field(default=None, description="Đã có xác nhận đủ sức khỏe từ bác sĩ")
    body_metrics: Optional[BodyMetrics] = None
    recent_workouts: Optional[List[Dict[str, Any]]] = Field(default_factory=list, description="Lịch sử các bài tập và mức tạ gần nhất")
    fatigue_level: Optional[int] = Field(default=None, description="Mức độ mệt mỏi từ 1 (rất sung sức) đến 10 (kiệt sức)")

class SafetyCheckResult(BaseModel):
    is_safe: bool
    violation_type: Optional[str] = None  # "BANNED_SUBSTANCE", "MEDICAL_CONTRAINDICATION", "UNSAFE_EXERCISE"
    flagged_terms: List[str] = Field(default_factory=list)
    warning_message: Optional[str] = None

class ChatRequest(BaseModel):
    message: str = Field(..., description="Câu hỏi hoặc chia sẻ của hội viên")
    conversation_history: List[ChatMessage] = Field(default_factory=list, description="Lịch sử hội thoại trước đó")
    fitness_profile: Optional[FitnessProfile] = Field(default=None, description="Ngữ cảnh thể lực của hội viên")

class ChatResponse(BaseModel):
    reply: str
    is_safe: bool = True
    safety_warning: Optional[str] = None
    suggested_actions: List[str] = Field(default_factory=list)
    context_used: Optional[Dict[str, Any]] = None

class ExerciseChange(BaseModel):
    action: str = Field(description="SUBSTITUTE | REDUCE_LOAD | INCREASE_WARMUP | REMOVE")
    exercise_name: str
    replacement_or_modification: str
    reason: str

class WorkoutRecommendationRequest(BaseModel):
    fitness_profile: FitnessProfile
    current_plan_name: Optional[str] = None
    target_muscle_group: Optional[str] = None
    current_exercises: List[Dict[str, Any]] = Field(default_factory=list, description="Danh sách các bài tập trong buổi hôm nay")
    feedback_or_pain_note: Optional[str] = Field(default=None, description="Ghi chú cảm giác mỏi cơ, đau khớp của hội viên")

class WorkoutRecommendationResponse(BaseModel):
    analysis: str
    suggested_changes: List[ExerciseChange]
    recovery_advice: str
    confidence_score: float = Field(ge=0.0, le=1.0, default=0.95)
