# FITOS Cloud Deployment & Smoke Test Runbook (Railway / Render / Docker)

Tài liệu hướng dẫn triển khai Production và kịch bản Smoke Test xác thực tự động sau khi deploy Backend Spring Boot & AI CRM (Người A - Task 129).

---

## 1. Biến môi trường bắt buộc trên Railway / Render

| Biến môi trường | Giá trị mẫu | Mục đích |
| :--- | :--- | :--- |
| `PORT` | `8080` (Backend) / `8001` (AI-CRM) | Cổng dịch vụ web container |
| `SPRING_PROFILES_ACTIVE` | `prod` | Kích hoạt cấu hình production |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}` | URL kết nối cơ sở dữ liệu PostgreSQL |
| `SPRING_DATASOURCE_USERNAME`| `${PGUSER}` | Tài khoản DB |
| `SPRING_DATASOURCE_PASSWORD`| `${PGPASSWORD}` | Mật khẩu DB |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | Khóa bí mật ký JWT Token |
| `AI_CRM_URL` | `http://ai-crm.railway.internal:8001` hoặc URL public | Đường dẫn backend gọi sang microservice AI |
| `AI_CRM_API_KEY` | `crm-secret-ai-token-2026` | Bearer token xác thực giữa các service |

---

## 2. Kịch bản Smoke Test (Post-deployment Verification)

Sau khi deploy hoàn tất, chạy script smoke test sau để xác thực hệ thống:

```bash
#!/usr/bin/env bash
set -e

BACKEND_URL=${1:-"http://localhost:8080"}
AI_CRM_URL=${2:-"http://localhost:8001"}

echo "=== [SMOKE TEST 1/4] Checking AI-CRM Health Endpoint ==="
curl -f -s "${AI_CRM_URL}/health" | grep '"status":"UP"'
echo " -> AI CRM is healthy and running!"

echo "=== [SMOKE TEST 2/4] Testing AI-CRM Lead Scoring API ==="
curl -f -s -X POST "${AI_CRM_URL}/score-lead" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer crm-secret-ai-token-2026" \
  -d '{
    "lead": {
      "id": 1,
      "name": "Smoke Test User",
      "fitness_goal": "MUSCLE_GAIN",
      "budget_range": "HIGH",
      "activities": [
        {"activity_type": "TRIAL", "outcome": "ATTENDED", "notes": "Hài lòng và muốn mua"}
      ]
    }
  }' | grep '"tier"'
echo " -> Lead scoring API responding correctly!"

echo "=== [SMOKE TEST 3/4] Checking Backend Exercise List API ==="
curl -f -s "${BACKEND_URL}/api/exercises?size=5" > /dev/null
echo " -> Backend Database & Flyway Migrations connected and active!"

echo "=== [SMOKE TEST 4/4] Checking Workout Plans API ==="
curl -f -s "${BACKEND_URL}/api/workout-plans?customerId=1" > /dev/null
echo " -> Workout Plans endpoint operational!"

echo "=== ALL SMOKE TESTS PASSED SUCCESSFULLY! ==="
```

