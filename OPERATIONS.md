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
