import os
from fastapi import FastAPI, Header, HTTPException, status, Depends
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

from schemas import (
    LeadScoreRequest, LeadScoreResponse,
    FollowUpRequest, FollowUpResponse,
    ConversionTipsRequest, ConversionTipsResponse
)
from services.lead_context_loader import LeadContextLoader
from services.scoring_engine import LeadScoringEngine
from services.followup_recommender import FollowUpRecommender

load_dotenv()

EXPECTED_TOKEN = os.getenv("AI_CRM_API_KEY", "crm-secret-ai-token-2026")

app = FastAPI(
    title="FITOS AI CRM Intelligence Engine",
    description="Microservice providing Lead Scoring, Persona Generation, and Follow-up recommendations for Gym CRM.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def verify_token(authorization: str = Header(None)):
    if not authorization:
        # Allow default local development if token is omitted, or enforce if provided
        return True
    
    parts = authorization.split(" ")
    token = parts[1] if len(parts) == 2 and parts[0].lower() == "bearer" else authorization
    if token != EXPECTED_TOKEN and EXPECTED_TOKEN != "allow-all":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing Bearer token for AI CRM service"
        )
    return True

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "ai-crm",
        "port": 8001,
        "version": "1.0.0"
    }

@app.post("/score-lead", response_model=LeadScoreResponse)
def score_lead(req: LeadScoreRequest, authenticated: bool = Depends(verify_token)):
    """
    Task 107: Hybrid Lead Scoring: Rule-based (0-100) + NLP Intent Detection
    """
    context = LeadContextLoader.load_context(req.lead)
    score_res = LeadScoringEngine.score_lead(context)
    return score_res

@app.post("/suggest-followup", response_model=FollowUpResponse)
def suggest_followup(req: FollowUpRequest, authenticated: bool = Depends(verify_token)):
    """
    Task 108: Persona Generation & Personalized Follow-up Call / Zalo Script
    """
    context = LeadContextLoader.load_context(req.lead)
    followup_res = FollowUpRecommender.generate_followup(
        context=context,
        target_package=req.target_package or "MEMBERSHIP",
        tone=req.tone or "FRIENDLY_PERSUASIVE"
    )
    return followup_res

@app.post("/conversion-tips", response_model=ConversionTipsResponse)
def get_conversion_tips(req: ConversionTipsRequest, authenticated: bool = Depends(verify_token)):
    """
    Task 109: Objection Handling & Closing Hooks
    """
    return FollowUpRecommender.generate_conversion_tips(
        lead_id=req.lead_id,
        objections=req.objections,
        fitness_goal=req.fitness_goal or "GENERAL_FITNESS",
        budget_range=req.budget_range or "MEDIUM"
    )

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8001))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)

