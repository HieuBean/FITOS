"""Lớp gọi LLM tuỳ chọn — nếu không có API key thì service vẫn chạy ở chế độ rule-based."""

from __future__ import annotations

import logging
import os
from typing import List, Optional

logger = logging.getLogger("ai-operations.llm")

try:  # pragma: no cover - import phụ thuộc môi trường
    from openai import AsyncOpenAI
except Exception:  # pragma: no cover
    AsyncOpenAI = None  # type: ignore

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "").strip()
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")

_client = None
if OPENAI_API_KEY and AsyncOpenAI is not None:
    _client = AsyncOpenAI(api_key=OPENAI_API_KEY)
    logger.info("OpenAI đã cấu hình — trainer-tips có thể dùng LLM")


def is_configured() -> bool:
    return _client is not None


SYSTEM_PROMPT = (
    "Bạn là huấn luyện viên trưởng của một phòng gym tại Việt Nam. "
    "Đưa ra 3 lời khuyên ngắn, cụ thể, có thể hành động ngay cho huấn luyện viên phụ trách hội viên. "
    "Trả về JSON dạng {\"tips\": [\"...\", \"...\", \"...\"]}. Không chẩn đoán bệnh, không kê thuốc."
)


async def enrich_tips(context: str) -> Optional[List[str]]:
    """Trả về danh sách tips do LLM sinh, hoặc None nếu chưa cấu hình/lỗi."""
    if _client is None:
        return None
    try:
        response = await _client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": context},
            ],
            temperature=0.4,
            max_tokens=400,
            response_format={"type": "json_object"},
        )
        import json

        data = json.loads(response.choices[0].message.content or "{}")
        tips = data.get("tips")
        if isinstance(tips, list) and tips:
            return [str(tip).strip() for tip in tips if str(tip).strip()][:3]
    except Exception as exception:  # pragma: no cover - lỗi mạng/quota
        logger.warning("Gọi LLM thất bại, dùng rule-based: %s", exception)
    return None
