import os
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from schemas import (
    PredictRenewalRequest, PredictRenewalResponse,
    RenewalActionRequest, RenewalActionResponse,
    SuggestUpsellRequest, SuggestUpsellResponse
)

app = FastAPI(
    title="FITOS AI Commerce Service",
    description="Microservice for renewal risk prediction, member retention, and plan upsell recommendations",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
def health():
    return {"status": "ok", "service": "ai-commerce", "port": 8002}

@app.post("/predict-renewal", response_model=PredictRenewalResponse)
def predict_renewal(req: PredictRenewalRequest):
    """
    Task #107: Rule-based renewal prediction.
    Calculates probability (0.0 to 1.0) based on recency, frequency, and tenure.
    """
    score = 0.50 # baseline

    # Frequency factor
    if req.attendance_last_30d >= 12:
        score += 0.35
    elif req.attendance_last_30d >= 8:
        score += 0.25
    elif req.attendance_last_30d >= 4:
        score += 0.10
    elif req.attendance_last_30d == 0:
        score -= 0.35
    else:
        score -= 0.15

    # Recency factor
    if req.days_since_last_visit <= 3:
        score += 0.15
    elif req.days_since_last_visit <= 7:
        score += 0.05
    elif req.days_since_last_visit > 21:
        score -= 0.30
    elif req.days_since_last_visit > 14:
        score -= 0.20

    # Tenure loyalty bonus
    if req.total_months_active >= 6:
        score += 0.10
    elif req.total_months_active >= 3:
        score += 0.05

    # PT attachment bonus
    if req.has_pt_package:
        score += 0.10

    # Bound strictly between 0.05 and 0.98
    prob = max(0.05, min(0.98, round(score, 2)))

    factors = []
    if req.days_since_last_visit > 14:
        factors.append(f"Absent for {req.days_since_last_visit} days")
    if req.attendance_last_30d >= 8:
        factors.append(f"Strong attendance: {req.attendance_last_30d} visits/mo")
    if req.attendance_last_30d < 4:
        factors.append(f"Low monthly attendance: {req.attendance_last_30d} visits")
    if req.has_pt_package:
        factors.append("Active PT engagement")

    risk = "LOW" if prob >= 0.70 else ("MEDIUM" if prob >= 0.40 else "HIGH")

    return PredictRenewalResponse(
        customer_id=req.customer_id,
        renewal_probability=prob,
        risk_level=risk,
        key_factors=factors or ["Standard engagement pattern"]
    )

@app.post("/renewal-action", response_model=RenewalActionResponse)
def renewal_action(req: RenewalActionRequest):
    """
    Task #108: Suggests targeted retention actions for at-risk members.
    """
    first_name = req.customer_name.split()[0] if req.customer_name else "there"

    if req.renewal_probability < 0.40 or req.days_since_last_visit > 14:
        return RenewalActionResponse(
            customer_id=req.customer_id,
            action_type="PERSONAL_CHECKIN",
            suggested_message=f"Hi {first_name}, we missed you at FITOS recently! Our head coach wanted to invite you for a 1-on-1 strategy check-in and complimentary recovery session this week.",
            recommended_channel="WHATSAPP",
            priority="URGENT"
        )
    elif req.membership_expiry_days <= 7:
        return RenewalActionResponse(
            customer_id=req.customer_id,
            action_type="DISCOUNT_OFFER",
            suggested_message=f"Hi {first_name}, your FITOS membership renews in {req.membership_expiry_days} days. Renew before Friday to lock in a 10% loyalty discount for your next cycle!",
            recommended_channel="SMS",
            priority="NORMAL"
        )
    else:
        return RenewalActionResponse(
            customer_id=req.customer_id,
            action_type="CLASS_INVITE",
            suggested_message=f"Hey {first_name}, high-energy classes are booking fast for next week. Reserve your spot early via the FITOS app!",
            recommended_channel="EMAIL",
            priority="LOW"
        )

@app.post("/suggest-upsell", response_model=SuggestUpsellResponse)
def suggest_upsell(req: SuggestUpsellRequest):
    """
    Task #109: Suggests plan upgrade based on usage patterns.
    """
    if "MOVE" in req.current_plan.upper() and req.monthly_attendance >= 8:
        return SuggestUpsellResponse(
            customer_id=req.customer_id,
            suggested_plan="Perform",
            upsell_reasons=[
                f"You attended {req.monthly_attendance} sessions this month, hitting your plan's cap.",
                "Perform offers unlimited studio access and group classes."
            ],
            pitch_message="Upgrade to Perform to train without monthly session caps and join unlimited group classes."
        )
    elif "PERFORM" in req.current_plan.upper() and req.attended_classes_count >= 10:
        return SuggestUpsellResponse(
            customer_id=req.customer_id,
            suggested_plan="PT Focus",
            upsell_reasons=[
                "Consistent group class participant with dedicated workout habits.",
                "PT sessions will help accelerate personal strength PRs and form precision."
            ],
            pitch_message="Take your training to the next level with 4 monthly 1-on-1 personal training sessions on PT Focus."
        )
    else:
        return SuggestUpsellResponse(
            customer_id=req.customer_id,
            suggested_plan="PT Pack 5 Sessions",
            upsell_reasons=["Personalized technique calibration"],
            pitch_message="Boost your workout efficiency with a 5-session PT kickstart package."
        )
