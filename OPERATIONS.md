# Chạy FITOS cục bộ

1. Đặt biến môi trường `FITOS_BOOTSTRAP_ADMIN_PASSWORD` với mật khẩu riêng tối thiểu 12 ký tự. Backend chỉ dùng giá trị này để kích hoạt tài khoản seed `admin@fitos.com` khi tài khoản đó chưa từng đổi mật khẩu. Mật khẩu `Admin@123` trong migration cũ không còn đăng nhập được.
2. Đặt `POSTGRES_PASSWORD`, `JWT_SECRET` và `AI_SERVICE_TOKEN` cho môi trường triển khai.
3. Chạy `docker compose up -d --build --wait`. Flyway chạy khi backend khởi động; `make migrate` cũng đợi backend và PostgreSQL khỏe. Dữ liệu bài tập mẫu nằm trong migration V4_1 và có thể kiểm tra bằng `make seed`.

Frontend gọi API backend qua `NEXT_PUBLIC_BACKEND_URL` tại thời điểm build. Với Docker Compose cục bộ, địa chỉ này là `http://localhost:8080/api`. Khi build image để triển khai, đặt biến GitHub `NEXT_PUBLIC_BACKEND_URL` thành URL API truy cập được từ trình duyệt.

## Triển khai Railway

Tạo bốn service Railway cho `backend`, `ai-workout`, `ai` (CRM scoring) và `frontend`, cùng PostgreSQL và Redis. Mỗi service phải dùng image GHCR tương ứng do workflow build (hoặc được cấu hình build trực tiếp từ đúng thư mục nguồn) trước khi dùng deploy webhook. Cấu hình backend với `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`, `AI_SERVICE_URL`, `AI_WORKOUT_URL`, `AI_SERVICE_TOKEN`, `FITOS_CORS_ALLOWED_ORIGINS` và `FITOS_BOOTSTRAP_ADMIN_PASSWORD`. Hai AI service dùng cùng `AI_SERVICE_TOKEN`; frontend chỉ cần URL backend công khai.

Trong GitHub repository, đặt biến `NEXT_PUBLIC_BACKEND_URL`, `BACKEND_HEALTH_URL` (URL kết thúc bằng `/api/studios`), `AI_WORKOUT_HEALTH_URL` (`/health`) và `FRONTEND_HEALTH_URL` (`/login`). Đặt bốn secret `RAILWAY_BACKEND_DEPLOY_WEBHOOK`, `RAILWAY_AI_WORKOUT_DEPLOY_WEBHOOK`, `RAILWAY_AI_CRM_DEPLOY_WEBHOOK`, `RAILWAY_FRONTEND_DEPLOY_WEBHOOK`. CD chỉ báo thành công khi build, kiểm thử, gọi webhook và kiểm tra URL triển khai đều thành công.

## Kiểm thử PostgreSQL

Bài `PostgresWorkflowIntegrationTest` chỉ chạy khi `FITOS_TEST_DATABASE_URL` trỏ tới một database PostgreSQL **riêng có tên kết thúc bằng `_test`**. Ví dụ trên PowerShell:

```powershell
$env:FITOS_TEST_DATABASE_URL = 'jdbc:postgresql://localhost:5432/fitos_test'
cd backend
mvn test
```

Có thể đặt thêm `FITOS_TEST_DATABASE_USER` và `FITOS_TEST_DATABASE_PASSWORD`. Bài kiểm thử tạo dữ liệu trong database này; không trỏ biến trên vào database đang dùng cho ứng dụng.

## Bốn AI service (layer C)

| Service | Port | Thư mục | Endpoint chính |
|---|---|---|---|
| ai-crm | 8001 | `ai/` | `/crm/score` (lead scoring, gọi từ `LeadAIClient`) |
| ai-commerce | 8002 | `ai-commerce/` | `/predict-renewal`, `/renewal-action` |
| **ai-operations** | **8003** | **`ai-operations/`** | `/health`, `/predict-noshow`, `/suggest-schedule`, `/trainer-tips` |
| ai-workout | 8004 | `ai-workout/` | `/chat`, `/recommend`, `/safety-check` |

Mọi service dùng chung header `Authorization: Bearer $AI_SERVICE_TOKEN`. Backend gọi ai-operations qua biến `AI_OPERATIONS_URL` (mặc định `http://localhost:8003`); nếu service chết, các endpoint `/api/ai/noshow-risk`, `/api/ai/schedule-suggestions`, `/api/ai/trainer-tips` vẫn trả kết quả rule-based với trường `source=fallback`.

### Chạy riêng ai-operations khi phát triển

```bash
cd ai-operations
python -m venv .venv && . .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8003
pytest -q
```

Không đặt `OPENAI_API_KEY` thì `/trainer-tips` dùng rule engine tất định; đặt key thì tự chuyển sang LLM (trường `engine = llm`).

## Kiểm thử tích hợp của layer C/A

`LeadConversionFlowIntegrationTest` chạy cùng điều kiện `FITOS_TEST_DATABASE_URL` (DB kết thúc bằng `_test`) và kiểm tra luồng Register → Login → tạo Lead → Convert → Customer. Chạy cả hai bài:

```bash
make test-integration
# hoặc
cd backend && mvn -B -Dtest='PostgresWorkflowIntegrationTest,LeadConversionFlowIntegrationTest' test
```

## CI

| Workflow | Phạm vi |
|---|---|
| `ci-ai-operations.yml` | pytest + build image + smoke test `/health` của ai-operations |
| `ci-integration.yml` | unit test backend, integration test với PostgreSQL + Redis thật, kiểm tra `docker compose config` |
| `ci-ai-workout.yml` | pytest cho ai-workout (của D) |

