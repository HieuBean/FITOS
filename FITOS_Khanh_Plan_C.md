# FITOS — KẾ HOẠCH PHẦN VIỆC "C-C-C-C-C-A"

> Người làm: **Khanh** · Nhánh: `Khanh-C-C-C-C-C-A` · Base: `origin/Dai-A_A_A_D_D_C`
> Phân công gốc: [`FITOS_Team_Assignment.md`](./FITOS_Team_Assignment.md) — 6 layer: **DB · BE · FE · AI · OPS · TEST**

## 1. Cách hiểu phân công

`C-C-C-C-C-A` = 5 layer đầu lấy **toàn bộ task của người C**, layer thứ 6 (TEST) lấy **task của người A**.

| Layer | Task gốc | Trạng thái trên base | Việc phải làm |
|-------|----------|----------------------|---------------|
| DB | #6 `V3__trainers`, #7 `V3__classes`, #8 `V3__bookings` | ✅ đã có (`V3_1..V3_4`) | Không sửa; chỉ thêm migration nếu entity mới cần cột |
| BE | #27 activities/assign, #28 convert, #29 lost, #33 AuditLogAspect, #38 payments, #39 callback, #40 ServiceActivation, #41 MembershipValidator, #42 memberships, #43 renew/expiring, #66 ai/recommend accept/dismiss, #58 exercises | 🟡 có 27/28; thiếu 29, 33, 38–43, 58, 66 (phần accept/dismiss) | Viết mới toàn bộ phần còn thiếu + REST layer vận hành (classes/sessions/bookings/checkin/attendance/PT) để FE gọi được |
| FE | #74 `/classes`, #80 `/dashboard/profile`, #85 `/dashboard/plans`, #86 `/dashboard/member/payments`, #87 MembershipBadge, #88 RenewalRisk, #89 `/dashboard/schedule`, #90 `/dashboard/bookings`, #95 AI suggestion banner | 🟡 trang tồn tại nhưng **toàn bộ là mock tĩnh**, chưa gọi API; thiếu trang plans, 2 component, banner | Nối API + viết trang/component mới |
| AI | #110 setup `ai-operations/`, #111 `/predict-noshow`, #112 `/suggest-schedule`, #113 `/trainer-tips`, #152 pytest | ❌ chưa có (chỉ có `ai/` dummy và `ai-workout/`) | Tạo service riêng port 8003 + test |
| OPS | #123 `ai-operations/Dockerfile`, #126 compose infra, #127 compose services, #133 CI ai-operations, #135 CI integration, #157 E2E Book→Checkin→Attendance | 🟡 compose 4 service, thiếu redis/healthcheck/4 AI service; CI thiếu | Viết lại compose full stack + 2 workflow + Makefile |
| TEST (A) | #140 AuthService, #141 LeadService, #155 E2E Register→Login→Lead→Convert + deploy backend | 🟡 có test controller; **thiếu unit test service** | Viết unit test Mockito + integration test E2E |

## 2. Nguyên tắc kỹ thuật bám theo base

- Package style của base: `com.fitos.{model,repository,service,controller,dto,exception,security,client}` — **không** dùng feature-package như nhánh B.
- Mọi truy vấn multi-tenant phải lọc `studio_id` lấy từ `StudioContext.currentStudioId()`.
- Enum trạng thái giữ đúng CHECK constraint trong migration (xem bảng dưới).
- Tiền dùng `BigDecimal` + `NUMERIC(12,2)`; không bao giờ nhận `total` từ client.
- Lỗi trả qua `BadRequestException` / `ResourceNotFoundException` để `GlobalExceptionHandler` render JSON chuẩn.

### Hợp đồng trạng thái (theo migration)

| Bảng | Trạng thái hợp lệ |
|---|---|
| `orders` | PENDING, PAID, FAILED, CANCELLED, REFUNDED |
| `payments` | PENDING, SUCCESS, FAILED, REFUNDED |
| `memberships` | PENDING, ACTIVE, PAUSED, EXPIRED, CANCELLED |
| `pt_packages` | ACTIVE, COMPLETED, EXPIRED, CANCELLED |
| `classes` / `class_sessions` | (classes) is_active; session: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED |
| `bookings` | PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW |
| `pt_sessions` | SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW |
| `ai_recommendations` | PENDING, ACCEPTED, DISMISSED · type WORKOUT_CHANGE/RECOVERY/PROGRESSION |

## 3. Checklist thực thi

### Layer DB (đã xong trên base)
- [x] `V3_1__trainers.sql` — trainers + trainer_assignments
- [x] `V3_2__classes.sql` — classes + class_sessions (+ seed 6 lớp)
- [x] `V3_3__bookings.sql` — bookings UNIQUE(customer,session) + attendance
- [x] `V3_4__pt_sessions.sql` — pt_sessions + completion_ref UNIQUE
- [x] `V4_6__studio_ownership.sql` — thêm `studio_id` cho toàn bộ bảng (multi-tenant)
- [x] **MỚI** `V5_1__payment_events.sql` — bảng `payment_events` (UNIQUE event_key cho idempotency) + index gia hạn/đơn hàng/điểm danh

### Layer BE — phần C viết mới
- [x] `PATCH /api/leads/{id}/lost` — `LeadService.markLost` + `LeadLostRequest`
- [x] `@Auditable` + `AuditLogAspect` — AOP ghi `audit_logs` (best-effort, không rollback nghiệp vụ)
- [x] `POST /api/payments` — `PaymentService.create`, validate số tiền = total của order, CASH thu ngay
- [x] `POST /api/payments/callback` — idempotent: `payment_events(event_key)` + `ON CONFLICT DO NOTHING`
- [x] `ServiceActivationService` — order PAID → `memberships` (MEMBERSHIP) hoặc `pt_packages` (PT_PACKAGE), idempotent theo order
- [x] `MembershipValidator.requireActive(customerId, studioId)` — chặn check-in khi hết hạn
- [x] `/api/memberships` — list, pause/resume/cancel, renew, `/expiring?days=`, `/me`; tự chuyển EXPIRED
- [x] `/api/plans` — CRUD + toggle bật/tắt bán (dùng `service_plans` seed sẵn)
- [x] `/api/orders` — tạo đơn giá server-side, list, detail, cancel PENDING, `/me`
- [x] `/api/exercises` — list/create/update (30 bài seed từ V4_1)
- [x] `/api/ai/recommendations` — `/me`, ticket theo customer, `/generate`, `/accept`, `/dismiss` (JSONB)
- [x] REST vận hành: `/api/classes`, `/api/sessions`, `/api/bookings`, `/api/checkin`, `/api/attendance`, `/api/pt-sessions`, `/api/trainers`
- [x] Proxy AI: `/api/ai/noshow-risk`, `/api/ai/schedule-suggestions`, `/api/ai/trainer-tips`, `/api/ai/operations-health` → `ai-operations`, fallback rule-based
- [x] `/api/profile/me` + `/api/profile/password` — phục vụ FE task #80
- [x] `SecurityConfig`: mở `GET /api/plans|/api/classes`, `POST /api/payments/callback`

### Layer FE
- [x] `/dashboard/plans` — bảng plan + toggle active + modal tạo (ADMIN/MANAGER)
- [x] `MembershipBadge` — ACTIVE/PAUSED/EXPIRED/CANCELLED/PENDING
- [x] `RenewalRiskWidget` — `/memberships/expiring` + chọn 7/14/30 ngày + nút gia hạn
- [x] `/dashboard/schedule` — lịch tuần từ `/api/sessions` + banner gợi ý AI + điều hướng tuần
- [x] `/dashboard/bookings` — bảng booking thật + điểm danh / no-show / huỷ
- [x] `/dashboard/member/payments` — membership card + lịch sử payment thật
- [x] `/classes` — grid lớp thật từ `/api/sessions` + `/api/classes`
- [x] `/dashboard/member/profile` — xem/sửa hồ sơ + đổi mật khẩu + mục tiêu + badge gói
- [x] Sidebar: thêm mục "Gói dịch vụ" → `/dashboard/plans`
- [x] Dashboard manager: nhúng `RenewalRiskWidget`

### Layer AI (`ai-operations/`, port 8003)
- [x] `main.py` + `schemas.py` + bearer-token auth
- [x] `POST /predict-noshow` — rule-based có yếu tố giải thích (`factors`)
- [x] `POST /suggest-schedule` — heuristic lấp slot + mở thêm suất
- [x] `POST /trainer-tips` — LLM nếu có key, fallback rule-based
- [x] `pytest`: 15 test (biên 0..1, monotonic, auth 401/403, fallback, ngày trống)
- [x] `Dockerfile` python:3.11-slim, non-root, healthcheck, expose 8003

### Layer OPS
- [x] `docker-compose.yml`: postgres + redis (+volume/healthcheck) + backend + frontend + 4 AI service, network chung
- [x] `Makefile`: thêm `test-ai-ops`, `test-integration`, cập nhật `test-all`, `seed`
- [x] `.github/workflows/ci-ai-operations.yml` — pytest + docker build + smoke test health
- [x] `.github/workflows/ci-integration.yml` — unit job + integration job (postgres + redis service) + `docker compose config`
- [x] `OPERATIONS.md` — bảng 4 service, cách chạy ai-operations, lệnh test

### Layer TEST (A)
- [x] `AuthServiceTest` — 10 test: register (+customer), không tự cấp ADMIN, email trùng, login, sai mật khẩu, refresh sai loại, user disable, refresh hợp lệ, logout
- [x] `LeadServiceTest` — 9 test: convert NEW, convert trùng, lead không tồn tại, tái dùng customer, markLost thiếu lý do, markLost ghi activity, CONVERTED không lost, chuẩn hoá source/goal, chặn studio khác
- [x] `LeadConversionFlowIntegrationTest` — E2E Register → Login → Lead → Convert → Customer (chạy khi có `FITOS_TEST_DATABASE_URL`)

## 4. Thứ tự thực hiện & phụ thuộc

```
DB (đã có)
 └─ BE commerce: plans → orders/payments → activation → memberships ─┐
 └─ BE operations: classes → sessions → bookings → checkin ──────────┤
 └─ BE misc: audit aspect, lead lost, recommendations, AI proxy ─────┤
                                                                     ├─ FE wiring ─┐
 AI service ai-operations ───────────────────────────────────────────┘             │
                                                                                   ├─ TEST
 OPS compose/CI/Makefile ──────────────────────────────────────────────────────────┘
```

## 5. Tiêu chí hoàn thành (Definition of Done)

1. `mvn -q -DskipTests compile` xanh; `mvn test` xanh (unit test không cần DB).
2. `npm run build` xanh, không lỗi TypeScript.
3. `pytest ai-operations` xanh ≥ 90% coverage phần rule-based.
4. `docker compose config` hợp lệ (không cần bật container khi máy không có Docker daemon).
5. Mỗi endpoint có `@PreAuthorize` đúng role và lọc `studio_id`.
6. Không có secret hard-code; mọi cấu hình qua biến môi trường.

## 6. Lệnh kiểm chứng

```bash
# backend
./.tools/apache-maven-3.9.9/bin/mvn.cmd -f backend/pom.xml test

# frontend
cd frontend && npm ci && npm run build

# ai-operations
cd ai-operations && python -m pytest -q

# compose
docker compose config
```
