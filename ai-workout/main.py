import os
import json
import logging
from typing import Optional
from dotenv import load_dotenv
from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.middleware.cors import CORSMiddleware
from openai import AsyncOpenAI

from schemas import (
    ChatRequest, ChatResponse,
    WorkoutRecommendationRequest, WorkoutRecommendationResponse, ExerciseChange,
    SafetyCheckResult, FitnessProfile
)
from safety_filter import check_safety
from services.fitness_context_loader import FitnessContextLoader

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ai-workout")

app = FastAPI(
    title="FITOS AI Workout Coach Service",
    description="Microservice tư vấn giáo án tập luyện, bảo vệ an toàn y tế và tối ưu bài tập thích ứng thể lực.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

security = HTTPBearer()

def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    expected_token = os.getenv("AI_SERVICE_TOKEN", "fitos-ai-token")
    if credentials.credentials != expected_token:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Forbidden: Invalid FITOS AI Service Token"
        )
    return credentials.credentials

# Khởi tạo client LLM (Google AI Studio Gemini hoặc OpenAI)
# Khởi tạo client LLM (Google AI Studio Gemini hoặc OpenAI)
gemini_api_key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY")
gemini_model = os.getenv("GEMINI_MODEL", "gemini-flash-latest")
openai_api_key = os.getenv("OPENAI_API_KEY")

llm_client: Optional[AsyncOpenAI] = None
llm_model: str = gemini_model if gemini_api_key else "gpt-4o-mini"
llm_provider: str = "google_gemini" if gemini_api_key else ("openai" if openai_api_key else "deterministic_engine")

if gemini_api_key:
    logger.info(f"Loaded Google AI Studio Gemini Key. Model: {gemini_model}")
elif openai_api_key:
    llm_client = AsyncOpenAI(api_key=openai_api_key)
    logger.info(f"Loaded OpenAI Key. Model: {llm_model}")

# Đảm bảo tương thích ngược với biến openai_client và tests
openai_client = llm_client

def call_gemini_native_sync(key: str, model: str, system_prompt: str, history: list, message: str) -> str:
    """Gọi trực tiếp Google Gemini API qua endpoint native v1beta với gemini-flash-latest."""
    import urllib.request
    import urllib.error

    candidate_models = [model, "gemini-3.5-flash", "gemini-3.6-flash"]
    seen = set()
    models_to_try = [m for m in candidate_models if not (m in seen or seen.add(m))]

    contents = []
    for msg in history[-6:]:
        role = "model" if getattr(msg, "role", "") in ["assistant", "ai"] else "user"
        content_text = getattr(msg, "content", "")
        if content_text:
            contents.append({"role": role, "parts": [{"text": content_text}]})
    contents.append({"role": "user", "parts": [{"text": message}]})

    payload = {
        "system_instruction": {
            "parts": [{"text": system_prompt}]
        },
        "contents": contents,
        "generationConfig": {
            "temperature": 0.7,
            "maxOutputTokens": 2500
        }
    }

    last_error = None
    for m in models_to_try:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{m}:generateContent?key={key}"
        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"}
        )
        try:
            with urllib.request.urlopen(req, timeout=20) as res:
                res_data = json.loads(res.read().decode("utf-8"))
                candidate = res_data.get("candidates", [{}])[0]
                parts = candidate.get("content", {}).get("parts", [])
                
                text_parts = [p.get("text", "") for p in parts if "text" in p and not p.get("thought", False)]
                if not text_parts:
                    text_parts = [p.get("text", "") for p in parts if "text" in p]
                    
                result_text = "".join(text_parts).strip()
                if result_text:
                    return result_text
        except urllib.error.HTTPError as e:
            last_error = e
            if e.code == 429:
                logger.warning(f"Google Gemini model {m} hit 429 quota. Trying alternative Gemini model...")
                continue
            raise e
        except Exception as e:
            last_error = e
            logger.warning(f"Google Gemini model {m} error: {e}. Trying alternative Gemini model...")
            continue

    if last_error:
        raise last_error
    raise ValueError("All Gemini models exhausted")

async def call_gemini_native(key: str, model: str, system_prompt: str, history: list, message: str) -> str:
    import asyncio
    return await asyncio.to_thread(call_gemini_native_sync, key, model, system_prompt, history, message)

@app.get("/health")
def health_check():
    active_llm = bool(gemini_api_key or openai_api_key or openai_client)
    return {
        "status": "healthy",
        "service": "ai-workout",
        "port": 8004,
        "llm_provider": "google_gemini" if gemini_api_key else ("openai" if openai_api_key else "deterministic_engine"),
        "llm_model": gemini_model if gemini_api_key else (os.getenv("OPENAI_MODEL", "gpt-4o-mini") if openai_api_key else None),
        "llm_configured": active_llm,
        "gemini_configured": bool(gemini_api_key),
        "openai_configured": bool(openai_api_key or openai_client)
    }

@app.post("/safety-check", response_model=SafetyCheckResult, dependencies=[Depends(verify_token)])
def test_safety_check(payload: dict):
    message = payload.get("message", "")
    return check_safety(message)

@app.post("/chat", response_model=ChatResponse, dependencies=[Depends(verify_token)])
async def chat_with_coach(req: ChatRequest):
    """
    Task #122: AI Workout Coach
    Quy trình 3 bước: Safety Guard -> Context Injection -> LLM / Hybrid Fallback
    """
    # Bước 1: Kiểm duyệt an toàn (Safety Guardrails)
    safety_result = check_safety(req.message)
    if not safety_result.is_safe:
        logger.warning(f"Safety violation intercepted: {safety_result.violation_type} | Flagged: {safety_result.flagged_terms}")
        return ChatResponse(
            reply=safety_result.warning_message or "Yêu cầu bị từ chối do vi phạm quy chuẩn an toàn y tế.",
            is_safe=False,
            safety_warning=f"Phát hiện thuật ngữ không an toàn: {', '.join(safety_result.flagged_terms)}",
            suggested_actions=["Hỏi về kỹ thuật bài tập tự nhiên", "Tham khảo ý kiến Bác sĩ chuyên khoa", "Xem thực đơn dinh dưỡng sạch"]
        )

    # Bước 2: Nạp ngữ cảnh thể lực chuyên sâu (Context Injection)
    profile = req.fitness_profile or FitnessProfile()
    system_prompt = FitnessContextLoader.build_system_prompt(profile)
    context_data = FitnessContextLoader.load_context(profile)

    # Bước 3: Gọi Google Gemini / OpenAI hoặc Hybrid Fallback
    reply_text = ""
    suggested_actions = []

    # Ưu tiên 1: Google Gemini (nếu đã cấu hình GEMINI_API_KEY)
    if gemini_api_key:
        try:
            reply_text = await call_gemini_native(
                gemini_api_key,
                gemini_model,
                system_prompt,
                req.conversation_history,
                req.message
            )
            suggested_actions = ["Xem bài tập gợi ý", "Ghi lại nhật ký buổi tập", "Đặt lịch tập cùng HLV"]
        except Exception as e:
            logger.error(f"Google Gemini call failed: {e}. Switching to Hybrid Deterministic Fallback.")
            reply_text = generate_hybrid_reply(req.message, profile, context_data)
            suggested_actions = ["Khởi động 10 phút", "Uống đủ 500ml nước", "Xem hướng dẫn động tác"]

    # Ưu tiên 2: OpenAI (nếu có openai_client)
    elif openai_client:
        try:
            messages = [{"role": "system", "content": system_prompt}]
            for msg in req.conversation_history[-6:]:  # Lấy 6 lượt hội thoại gần nhất
                messages.append({"role": msg.role, "content": msg.content})
            messages.append({"role": "user", "content": req.message})

            response = await openai_client.chat.completions.create(
                model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
                messages=messages,
                temperature=0.7,
                max_tokens=600
            )
            reply_text = response.choices[0].message.content or ""
            suggested_actions = ["Xem bài tập gợi ý", "Ghi lại nhật ký buổi tập", "Đặt lịch tập cùng HLV"]
        except Exception as e:
            logger.error(f"OpenAI call failed: {e}. Switching to Hybrid Deterministic Fallback.")
            reply_text = generate_hybrid_reply(req.message, profile, context_data)
            suggested_actions = ["Khởi động 10 phút", "Uống đủ 500ml nước", "Xem hướng dẫn động tác"]

    # Ưu tiên 3: Bộ máy Sinh phản hồi thông minh cục bộ
    else:
        reply_text = generate_hybrid_reply(req.message, profile, context_data)
        suggested_actions = ["Khởi động 10 phút", "Uống đủ 500ml nước", "Xem hướng dẫn động tác"]

    return ChatResponse(
        reply=reply_text,
        is_safe=True,
        suggested_actions=suggested_actions,
        context_used=context_data
    )

def generate_hybrid_reply(message: str, profile: FitnessProfile, context: dict) -> str:
    """Bộ tạo phản hồi thể hình thông minh cục bộ khi không có OpenAI key."""
    msg_lower = message.lower()
    name = context.get("full_name", "bạn")
    goal = context.get("fitness_goal", "thể lực")
    injuries = context.get("injuries", [])

    # Nếu có chấn thương lưng
    if any("lưng" in inj.lower() for inj in injuries):
        injury_note = "Do bạn đang có lưu ý về thắt lưng, tuyệt đối hãy gồng chặt core (bracing) và tránh các bài tạ đòn dồn lực dọc sống lưng nhé! "
    elif any("gối" in inj.lower() for inj in injuries):
        injury_note = "Lưu ý đầu gối của bạn: hãy tập trung mở khớp háng, đẩy mông ra sau và không để gối chụm vào trong (knee valgus) khi tập chân nhé! "
    else:
        injury_note = ""

    if any(k in msg_lower for k in ["toàn thân", "full body", "1 tuần", "một tuần", "lịch tập", "giáo án tuần"]):
        return (
            f"Chào {name}! Đây là ví dụ lịch tập toàn thân để tham khảo cho mục tiêu {goal or 'bạn đang hướng tới'}. "
            f"{injury_note}Hãy điều chỉnh sau khi xác nhận thể trạng và khả năng vận động với huấn luyện viên:\n\n"
            "📅 **BUỔI 1 (Thứ Hai) — Full Body A (Chân mông & Đẩy)**:\n"
            "1. **Goblet Squat (Dumbbell)**: 4 hiệp × 10-12 lần (nghỉ 90s). Giữ lưng thẳng, siết mông khi đứng lên.\n"
            "2. **Incline Dumbbell Press (Đẩy ngực trên)**: 3 hiệp × 10 lần.\n"
            "3. **Chest-Supported Dumbbell Row**: 3-4 hiệp × 10-12 lần. Tựa ngực vào ghế giải tỏa hoàn toàn áp lực cột sống.\n"
            "4. **Plank gồng Core**: 3 hiệp × 30-45 giây.\n\n"
            "📅 **BUỔI 2 (Thứ Tư) — Full Body B (Chuỗi cơ sau & Kéo)**:\n"
            "1. **Romanian Deadlift (RDL)**: 3 hiệp × 10 lần. Đẩy hông ra sau, cảm nhận căng cơ đùi sau.\n"
            "2. **Neutral-Grip Dumbbell Shoulder Press**: 3 hiệp × 10-12 lần (cầm song song bảo vệ khớp vai).\n"
            "3. **Lat Pulldown (Kéo xô máy)**: 3 hiệp × 12 lần.\n"
            "4. **Lying Leg Curl (Cuốn đùi sau)**: 3 hiệp × 15 lần.\n\n"
            "📅 **BUỔI 3 (Thứ Sáu) — Full Body C (Hypertrophy & Sức bền)**:\n"
            "1. **Bulgarian Split Squat**: 3 hiệp × 10 lần mỗi chân.\n"
            "2. **Dumbbell Flat Bench Press**: 3 hiệp × 10-12 lần.\n"
            "3. **Seated Cable Row**: 3 hiệp × 12 lần (kéo vào rốn, ép chặt bả vai).\n"
            "4. **Superset Bicep Curl & Tricep Pushdown**: 3 hiệp × 12 lần.\n\n"
            "💡 *Lời khuyên từ HLV FITOS*: Giữa các buổi tập hãy nghỉ 1 ngày (đi bộ nhẹ 20-30 phút), uống đủ 2-2.5L nước và ngủ 7-8 tiếng để cơ bắp phục hồi tối ưu nhé!"
        )

    if any(k in msg_lower for k in ["chân", "squat", "leg", "đùi"]):
        return (
            f"Chào {name}! Để phát triển cơ đùi và mông cho mục tiêu {goal}, "
            f"{injury_note}hôm nay bạn có thể thực hiện giáo án sau:\n\n"
            "1. **Goblet Squat**: 4 hiệp × 10-12 lần (nghỉ 90 giây). Tập trung siết mông ở đỉnh chuyển động.\n"
            "2. **Romanian Deadlift (RDL)**: 3 hiệp × 10 lần. Giữ lưng thẳng tự nhiên, cảm nhận cơ đùi sau căng giãn.\n"
            "3. **Walking Lunges**: 3 hiệp × 12 bước mỗi chân.\n"
            "4. **Leg Curl nằm**: 3 hiệp × 15 lần để cô lập nhóm cơ đùi sau.\n\n"
            "💡 *Mẹo từ FITOS AI*: Hãy hít sâu gồng bụng trước khi hạ người và thở ra dứt khoát khi đẩy tạ lên nhé!"
        )

    if any(k in msg_lower for k in ["ngực", "tay sau", "bench", "press"]):
        return (
            f"Tuyệt vời {name}! Buổi tập Ngực & Tay sau hướng tới {goal} của bạn nên tập trung vào góc ép ngực chuẩn:\n\n"
            "1. **Incline Dumbbell Press (Ngực trên)**: 4 hiệp × 8-10 lần (nghỉ 90s). Hạ tạ chậm trong 2-3 giây.\n"
            "2. **Dumbbell Flat Bench Press**: 3 hiệp × 10-12 lần.\n"
            "3. **Cable Chest Fly**: 3 hiệp × 15 lần. Giữ cùi chỏ hơi cong cố định, siết chặt ngực ở điểm chạm.\n"
            "4. **Tricep Rope Pushdown (Tay sau)**: 4 hiệp × 12-15 lần.\n\n"
            f"{injury_note}💡 *Mẹo từ FITOS AI*: Hạ bả vai xuống và ép nhẹ ra sau (retract scapula) để bảo vệ khớp vai tối đa!"
        )

    if any(k in msg_lower for k in ["mệt", "đau", "nghỉ", "recovery"]):
        fatigue = context.get("fatigue_level", 3)
        return (
            f"Chào {name}! Hiện tại mức độ mệt mỏi của bạn đang ở mức {fatigue}/10. "
            "Nếu bạn cảm thấy cơ bắp ê ẩm hoặc thiếu ngủ, một buổi **Active Recovery (Phục hồi tích cực)** "
            "là lựa chọn khoa học nhất:\n\n"
            "- 15 phút giãn cơ tĩnh (Static Stretching) toàn thân.\n"
            "- 20 phút đi bộ dốc nhẹ (incline walk) với nhịp tim vùng Zone 2.\n"
            "- Uống đủ nước kèm điện giải và bổ sung 25-30g protein sau buổi tập để cơ bắp tái tạo nhanh nhất!"
        )

    return (
        f"Chào {name}! FITOS AI Coach đã sẵn sàng đồng hành cùng bạn trên hành trình {goal}. "
        f"{injury_note}Bạn muốn lên giáo án chi tiết cho nhóm cơ nào hôm nay (Ngực, Lưng, Chân, Vai, hay Bụng & Cardio)? "
        "Hãy chia sẻ mức tạ hoặc cảm giác cơ bắp hiện tại để mình tối ưu chính xác nhất cho bạn nhé!"
    )

@app.post("/recommend", response_model=WorkoutRecommendationResponse, dependencies=[Depends(verify_token)])
async def recommend_workout_adjustments(req: WorkoutRecommendationRequest):
    """
    Task #123: AI Structured Workout Plan Adaptation
    Sinh JSON có cấu trúc (Structured Output) điều chỉnh bài tập thích ứng với chấn thương và thể trạng.
    """
    profile = req.fitness_profile
    context = FitnessContextLoader.load_context(profile)
    if gemini_api_key or openai_client:
        instruction = FitnessContextLoader.build_system_prompt(profile) + (
            "\nTrả về đúng JSON với analysis, suggested_changes (mảng action, exercise_name, "
            "replacement_or_modification, reason), recovery_advice, confidence_score. "
            "Chỉ sửa bài tập có trong current_exercises; nếu danh sách rỗng, suggested_changes phải rỗng. "
            "Không chẩn đoán hoặc điều trị bệnh. Khi thiếu dữ liệu, nói rõ chưa đủ dữ liệu."
        )
        question = json.dumps({
            "current_plan_name": req.current_plan_name,
            "target_muscle_group": req.target_muscle_group,
            "current_exercises": req.current_exercises,
            "feedback_or_pain_note": req.feedback_or_pain_note
        }, ensure_ascii=False)
        try:
            if gemini_api_key:
                raw = await call_gemini_native(gemini_api_key, gemini_model, instruction, [], question)
            else:
                answer = await openai_client.chat.completions.create(
                    model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
                    messages=[{"role": "system", "content": instruction}, {"role": "user", "content": question}],
                    response_format={"type": "json_object"}
                )
                raw = answer.choices[0].message.content or ""
            data = json.loads(raw.strip().removeprefix("```json").removesuffix("```").strip())
            result = WorkoutRecommendationResponse.model_validate(data)
            allowed = {str(ex.get("name", "")).casefold() for ex in req.current_exercises}
            result.suggested_changes = [
                change for change in result.suggested_changes
                if change.exercise_name.casefold() in allowed
            ]
            return result
        except Exception as exc:
            logger.warning("Structured recommendation unavailable: %s", exc)
    injuries = context.get("injuries", [])
    pain_note = req.feedback_or_pain_note or ""

    if not req.current_exercises:
        return WorkoutRecommendationResponse(
            analysis="Chưa có danh sách bài tập trong giáo án hiện tại. Hãy cập nhật giáo án để nhận đề xuất điều chỉnh cụ thể.",
            suggested_changes=[],
            recovery_advice="Nếu đang đau hoặc có chấn thương, hãy trao đổi với huấn luyện viên hoặc nhân viên y tế trước buổi tập.",
            confidence_score=0.0,
        )

    suggested_changes: list[ExerciseChange] = []
    analysis = ""
    recovery_advice = ""

    # Kiểm tra chấn thương lưng
    has_back_issue = any("lưng" in inj.lower() for inj in injuries) or "lưng" in pain_note.lower()
    has_knee_issue = any("gối" in inj.lower() for inj in injuries) or "gối" in pain_note.lower()
    has_shoulder_issue = any("vai" in inj.lower() for inj in injuries) or "vai" in pain_note.lower()

    if has_back_issue:
        analysis = (
            "Hội viên báo đau hoặc mỏi vùng lưng. Cần giảm tải và hỏi thêm về mức đau trước khi chọn bài tập."
        )
        suggested_changes.append(
            ExerciseChange(
                action="SUBSTITUTE",
                exercise_name="Barbell Back Squat",
                replacement_or_modification="Goblet Squat với tạ ấm hoặc Dumbbell",
                reason="Có thể dễ kiểm soát tư thế hơn với mức tạ nhẹ; ngừng nếu gây đau."
            )
        )
        suggested_changes.append(
            ExerciseChange(
                action="SUBSTITUTE",
                exercise_name="Barbell Bent-over Row",
                replacement_or_modification="Chest-Supported Incline Dumbbell Row",
                reason="Ghế hỗ trợ thân trên và giảm yêu cầu giữ tư thế gập người."
            )
        )
        recovery_advice = "Thực hiện tư thế Cat-Cow và Bird-Dog trong 5 phút cuối buổi để giải tỏa áp lực cột sống."

    elif has_knee_issue:
        analysis = (
            "Hội viên báo đau hoặc mỏi gối. Cần hỏi thêm về triệu chứng và tránh bài tập gây đau."
        )
        suggested_changes.append(
            ExerciseChange(
                action="SUBSTITUTE",
                exercise_name="Leg Extension",
                replacement_or_modification="Romanian Deadlift (RDL)",
                reason="Leg Extension tạo lực cắt lớn lên gân bánh chè; RDL giúp tăng cường chuỗi cơ sau mà không dồn lực gối."
            )
        )
        suggested_changes.append(
            ExerciseChange(
                action="REDUCE_LOAD",
                exercise_name="Barbell Squat",
                replacement_or_modification="Giảm 25% mức tạ, ngồi chạm ghế (Box Squat 90 độ)",
                reason="Kiểm soát góc gập khớp gối trong biên độ an toàn sinh học."
            )
        )
        recovery_advice = "Nếu đau hoặc sưng kéo dài, ngừng bài tập gây đau và trao đổi với nhân viên y tế."

    elif has_shoulder_issue:
        analysis = (
            "Hội viên báo khó chịu ở vai. Cần giảm tải và tránh chuyển động gây đau cho tới khi được đánh giá."
        )
        suggested_changes.append(
            ExerciseChange(
                action="SUBSTITUTE",
                exercise_name="Barbell Flat Bench Press",
                replacement_or_modification="Neutral-Grip Dumbbell Incline Press",
                reason="Vị trí lòng bàn tay hướng vào nhau (Neutral) giải phóng khoang khớp vai và dây gân chóp xoay."
            )
        )
        suggested_changes.append(
            ExerciseChange(
                action="INCREASE_WARMUP",
                exercise_name="Shoulder Press",
                replacement_or_modification="3 hiệp Face Pull với dây kháng lực trước khi đẩy tạ",
                reason="Kích hoạt cơ trám và cơ xoay ngoài để ổn định xương bả vai."
            )
        )
        recovery_advice = "Giãn cơ ngực bé (Pectoralis Minor) bằng bóng massage hoặc góc tường để mở rộng khoang vai."

    else:
        # Chưa ghi nhận triệu chứng trong dữ liệu được cung cấp.
        analysis = (
            "Chưa đủ dữ liệu về chấn thương và giáo án hiện tại để đánh giá mức tạ hoặc tăng cường độ. "
            "Hãy cập nhật mục tiêu, tình trạng đau và bài tập hiện tại trước khi điều chỉnh giáo án."
        )
        suggested_changes.append(
            ExerciseChange(
                action="REDUCE_LOAD" if (profile.fatigue_level or 0) >= 7 else "INCREASE_WARMUP",
                exercise_name=req.current_plan_name or "Bài tập chính",
                replacement_or_modification="Tăng 1 hiệp khởi động lũy tiến tạ (Warm-up sets) trước hiệp chính",
                reason="Chuẩn bị thần kinh cơ và tối đa hóa tuyển dụng sợi cơ nhanh (Type II)."
            )
        )
        recovery_advice = "Nghỉ ngơi đầy đủ và hỏi huấn luyện viên khi chưa chắc về mức tạ phù hợp."

    planned_names = {str(ex.get("name", "")).casefold() for ex in req.current_exercises}
    suggested_changes = [change for change in suggested_changes if change.exercise_name.casefold() in planned_names]
    return WorkoutRecommendationResponse(
        analysis=analysis,
        suggested_changes=suggested_changes,
        recovery_advice=recovery_advice,
        confidence_score=0.5
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8004, reload=True)
