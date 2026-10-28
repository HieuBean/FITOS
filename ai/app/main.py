import os
from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel

app = FastAPI(title="FITOS AI Service")

bearer = HTTPBearer()
async def verify_token(creds: HTTPAuthorizationCredentials = Depends(bearer)):
    token = os.getenv("AI_SERVICE_TOKEN", "fitos-ai-token")
    if creds.credentials != token:
        raise HTTPException(status_code=403, detail="Invalid token")

class PromptRequest(BaseModel):
    prompt: str

@app.post("/llm/chat", dependencies=[Depends(verify_token)])
async def chat(req: PromptRequest):
    # Dummy response for now. Will connect to OpenAI later.
    return {"answer": f"Received prompt: {req.prompt}. AI integration pending."}

class LeadScoringRequest(BaseModel):
    leadId: int | None = None
    fullName: str = ''
    goal: str | None = None
    source: str | None = None
    status: str = 'NEW'
    activitiesCount: int = 0

class LeadScoringResponse(BaseModel):
    score: int
    persona: str
    recommendedAction: str

@app.post("/crm/score", response_model=LeadScoringResponse, dependencies=[Depends(verify_token)])
async def score_lead(req: LeadScoringRequest):
    score = 40
    if req.status.upper() == "TRIAL":
        score += 30
    elif req.status.upper() == "INTERESTED":
        score += 20
    elif req.status.upper() == "CONTACTED":
        score += 10
    score = min(max(score + min(req.activitiesCount * 5, 25), 10), 95)
    return LeadScoringResponse(
        score=score,
        persona=f"Warm Prospect ({req.goal or 'Fitness'})",
        recommendedAction="High priority: Call for Trial" if score >= 70 else "Nurture with messages",
    )