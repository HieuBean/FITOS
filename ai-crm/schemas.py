from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

class ActivityItem(BaseModel):
    activity_type: str = Field(..., description="CALL, CONSULTATION, TRIAL, MESSAGE")
    notes: Optional[str] = Field(None, description="Notes from the staff/lead")
    outcome: Optional[str] = Field(None, description="INTERESTED, BUSY, PRICE_SENSITIVE, ATTENDED, NO_SHOW")
    created_at: Optional[str] = None

class LeadProfile(BaseModel):
    id: int
    name: str
    phone: Optional[str] = None
    email: Optional[str] = None
    fitness_goal: Optional[str] = Field("GENERAL_FITNESS", description="MUSCLE_GAIN, WEIGHT_LOSS, STRENGTH, REHAB")
    budget_range: Optional[str] = Field("MEDIUM", description="LOW, MEDIUM, HIGH, PREMIUM")
    preferred_schedule: Optional[str] = Field("EVENING", description="MORNING, AFTERNOON, EVENING, WEEKEND")
    source: Optional[str] = Field("ORGANIC", description="FACEBOOK, TIKTOK, REFERRAL, WALK_IN, ADS")
    status: Optional[str] = Field("NEW", description="NEW, CONTACTED, CONSULTED, TRIAL_SCHEDULED, WON, LOST")
    activities: List[ActivityItem] = Field(default_factory=list)

class LeadScoreRequest(BaseModel):
    lead: LeadProfile

class ScoringFactor(BaseModel):
    name: str
    points: float
    max_points: float
    reason: str

class LeadScoreResponse(BaseModel):
    lead_id: int
    score: int = Field(..., ge=0, le=100, description="Lead score from 0 to 100")
    tier: str = Field(..., description="HOT, WARM, COLD")
    intent: str = Field(..., description="HIGH_PURCHASE_INTENT, EXPLORING, LOW_ENGAGEMENT, PRICE_SHOPPING")
    breakdown: List[ScoringFactor]
    summary: str

class FollowUpRequest(BaseModel):
    lead: LeadProfile
    target_package: Optional[str] = "3_MONTHS_MEMBERSHIP_OR_PT"
    tone: Optional[str] = "FRIENDLY_PERSUASIVE"

class FollowUpResponse(BaseModel):
    lead_id: int
    persona: str
    primary_pain_point: str
    suggested_angle: str
    recommended_channel: str
    call_script: str
    zalo_sms_script: str
    best_contact_time: str

class ConversionTipsRequest(BaseModel):
    lead_id: int
    objections: List[str] = Field(default_factory=list, description="E.g.: ['Giá hơi cao', 'Chưa có thời gian']")
    fitness_goal: Optional[str] = "WEIGHT_LOSS"
    budget_range: Optional[str] = "MEDIUM"

class ConversionTip(BaseModel):
    objection: str
    handling_technique: str
    script: str

class ConversionTipsResponse(BaseModel):
    lead_id: int
    tips: List[ConversionTip]
    closing_hook: str

