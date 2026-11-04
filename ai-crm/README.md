# FITOS Layer 4 - AI CRM Microservice

Microservice độc lập phục vụ trí tuệ nhân tạo cho module CRM (Port 8001), phụ trách chấm điểm lead (Lead Scoring), xây dựng chân dung khách hàng (Persona) và kịch bản chăm sóc cá nhân hóa (Follow-up scripts).

## Kiến trúc & Cổng dịch vụ
- **Port**: `8001`
- **Framework**: FastAPI (Pydantic v2, Uvicorn, Pytest)
- **Authentication**: Bearer Token (`AI_CRM_API_KEY` trong `.env` / Header `Authorization: Bearer <token>`)

## Danh sách API Endpoints

### 1. `GET /health`
Kiểm tra tình trạng hoạt động của service:
```json
{
  "status": "UP",
  "service": "ai-crm",
  "port": 8001,
  "version": "1.0.0"
}
```

### 2. `POST /score-lead`
Chấm điểm Lead kết hợp Hybrid Scoring (0 - 100 điểm):
- **Phân tích 4 trục**:
  - `Profile & Budget Alignment` (Max 25 điểm)
  - `Engagement & Touchpoints` (Max 25 điểm)
  - `Trial Experience` (Max 30 điểm - tín hiệu trải nghiệm thực tế)
  - `Intent & Sentiment Analysis` (Max 20 điểm - NLP intent detection từ ghi chú/lời thoại)
- **Output Tiers**: `HOT` (>=75), `WARM` (45-74), `COLD` (<45).

### 3. `POST /suggest-followup`
Phân tích Persona (High-Value Achiever, Value-Conscious, Busy Professional, Beginner) và sinh kịch bản gọi điện thoại + tin nhắn Zalo/SMS cá nhân hóa kèm khung giờ liên hệ tối ưu.

### 4. `POST /conversion-tips`
Xử lý các rào cản từ chối phổ biến (Về giá: Cost Reframe, Về thời gian: Lịch linh hoạt 45 phút, Đắn đo suy nghĩ: Scarcity & Ưu đãi có hạn) và câu chốt cam kết (Closing Hook).

## Hướng dẫn chạy & Kiểm thử

### Chạy Unit Test:
```bash
cd ai-crm
python -m pytest tests
```
*Kết quả: 7/7 tests passed (100%).*

### Chạy Service Local:
```bash
cd ai-crm
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

