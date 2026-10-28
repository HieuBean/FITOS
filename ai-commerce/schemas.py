from typing import Optional, List
from pydantic import BaseModel, Field

class PredictRenewalRequest(BaseModel):
    customer_id: int
    attendance_last_30d: int = Field(..., ge=0, description="Number of check-ins in last 30 days")
    days_since_last_visit: int = Field(..., ge=0, description="Days since member was last in studio")
    total_months_active: int = Field(default=1, ge=1, description="Total tenure in months")
    has_pt_package: bool = False
    classes_attended_last_30d: int = Field(default=0, ge=0)

class PredictRenewalResponse(BaseModel):
    customer_id: int
    renewal_probability: float = Field(..., ge=0.0, le=1.0)
    risk_level: str # "LOW", "MEDIUM", "HIGH"
    key_factors: List[str]

class RenewalActionRequest(BaseModel):
    customer_id: int
    customer_name: str
    renewal_probability: float = Field(..., ge=0.0, le=1.0)
    days_since_last_visit: int
    current_plan: str
    membership_expiry_days: int

class RenewalActionResponse(BaseModel):
    customer_id: int
    action_type: str # "DISCOUNT_OFFER", "PERSONAL_CHECKIN", "FREE_PT_SESSION", "CLASS_INVITE"
    suggested_message: str
    recommended_channel: str # "SMS", "WHATSAPP", "CALL", "EMAIL"
    priority: str # "URGENT", "NORMAL", "LOW"

class SuggestUpsellRequest(BaseModel):
    customer_id: int
    current_plan: str
    monthly_attendance: int
    attended_classes_count: int
    expressed_goal: Optional[str] = None

class SuggestUpsellResponse(BaseModel):
    customer_id: int
    suggested_plan: str
    upsell_reasons: List[str]
    pitch_message: str
