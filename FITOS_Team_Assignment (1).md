# FITOS — PHÂN CÔNG NHÓM 4 NGƯỜI × 5 TUẦN
## Phiên bản 8.0 — TÁI CÂN BẰNG TOÀN DIỆN (Phương án A: Bình đẳng công nghệ cao & kiến thức 100%)
### Mỗi người = 37h chính xác | Cả 4 đều làm AI nâng cao, Dashboard xịn, Backend phức tạp | Cả 4 nắm trọn bộ kiến thức toàn dự án

---

## NGUYÊN TẮC CHIA & BÌNH ĐẲNG CÔNG NGHỆ (PHƯƠNG ÁN A)

> **Tổng: 148h → mỗi người CHÍNH XÁC 37h**
>
> ✅ **Mỗi layer chia đều 4 phần BẰNG NHAU**: DB=2h, BE=12h, FE=12h, AI=5h, OPS=3h, TEST=3h cho mỗi người.
> ✅ **Bình đẳng "Cool Factor" & Công nghệ cao**: 
>    • **Không ai "độc quyền" GenAI**: Cả 4 người đều tự viết Python Hybrid AI có **Context Injection từ DB**, Prompt Engineering và Safety/Validation.
>    • **Không ai làm proxy hộ**: Cả 4 người đều tự viết **Spring Boot WebClient/RestClient** trong Java để kết nối trực tiếp với microservice AI của chính mình.
>    • **Bình đẳng Data Visualization**: Người A sở hữu Manager Dashboard (biểu đồ doanh thu pure CSS + funnel), Người B sở hữu Sales Dashboard & Customer 360, Người C sở hữu 7-column Weekly Calendar & Booking Strip, Người D sở hữu Trainer Schedule & Member Progress Chart.
> ✅ **Cơ chế All-Team Mastery (Cả 4 người sở hữu 100% kiến thức toàn dự án)**:
>    • Task được chia để thi công song song, nhưng **cả 4 người bắt buộc phải hiểu sâu toàn bộ 8 chuyên đề kiến trúc của nhau**.
>    • Áp dụng Rotating Pair-Programming hàng tuần và Code Walkthrough sau mỗi Pull Request.
>    • Khi hội đồng bốc thăm vấn đáp bất kỳ ai về bất kỳ module nào (Auth, Payment, Concurrency, AI, Docker, CI/CD), người đó đều trả lời trơn tru như chính mình code.
> ✅ **Độ ưu tiên cân bằng tuyệt đối**: 🔴 Must-have ~34h–34.5h, 🟡 Nice-to-have ~2.5h–3.0h (lệch tối đa 0.5h).
> ✅ **100% Spec Coverage**: bao phủ trọn vẹn toàn bộ 21 khối nghiệp vụ của Functional Specification.
>
> 🔴 Must-have (tuần 1–4) | 🟡 Nice-to-have (làm tuần 5 nếu còn giờ)
> Có AI hỗ trợ (Cursor/Kiro/Copilot) → estimate đã tính sẵn

**Mỗi người sở hữu riêng 1 Python AI microservice nâng cao (Hybrid AI + Context Injection):**
```
ai-crm/         port 8001  ← A tự viết (Lead Context Loader + Hybrid Scoring 0-100 + Persona Followup)
ai-commerce/    port 8002  ← B tự viết (Spending Context Loader + Churn Risk Model + Dynamic Retention Offer)
ai-operations/  port 8003  ← C tự viết (Schedule Matrix Loader + No-Show Risk Engine + Smart Slot Optimization)
ai-workout/     port 8004  ← D tự viết (Fitness Context Loader + Safety Filter Guard + Conversational Workout Coach)
```

---

## BẢNG CHIA ĐỀU THEO LAYER & ĐỘ ƯU TIÊN (CHUẨN HÓA SAU AUDIT V8)

| Layer | Tổng giờ | A | B | C | D | Trạng thái |
|-------|----------|---|---|---|---|------------|
| DB | 8h | **2h** | **2h** | **2h** | **2h** | ✅ Đều tuyệt đối |
| BE | 48h | **12h** | **12h** | **12h** | **12h** | ✅ Đều tuyệt đối |
| FE | 48h | **12h** | **12h** | **12h** | **12h** | ✅ Đều tuyệt đối |
| AI | 20h | **5h** | **5h** | **5h** | **5h** | ✅ Đều tuyệt đối |
| OPS | 12h | **3h** | **3h** | **3h** | **3h** | ✅ Đều tuyệt đối |
| TEST | 12h | **3h** | **3h** | **3h** | **3h** | ✅ Đều tuyệt đối |
| **TỔNG GIỜ** | **148h** | **37h** | **37h** | **37h** | **37h** | ✅ **BẰNG NHAU** |
| 🔴 Must-Have | 136.5h | **34.5h** | **34.0h** | **34.0h** | **34.0h** | ✅ **Cân bằng tối đa** |
| 🟡 Nice-To-Have | 11.5h | **2.5h** | **3.0h** | **3.0h** | **3.0h** | ✅ **Chênh lệch ≤ 0.5h** |

---

## LAYER 1 — DATABASE (8h tổng → 2h/người)

> Mỗi người thiết kế và viết migration cho 1 domain riêng + 1 phần chung.

| # | Task | h | 🔴/🟡 | Người |
|---|------|---|-------|-------|
| 1 | `V1__users.sql` — bảng users, roles, index email UNIQUE | 0.5 | 🔴 | **A** |
| 2 | `V1__leads.sql` — leads, lead_activities, audit_logs | 1.0 | 🔴 | **A** |
| 3 | `V1__customers.sql` — customers, index user_id | 0.5 | 🔴 | **A** |
| 4 | `V2__plans.sql` — service_plans (MEMBERSHIP + PT_PACKAGE) | 0.5 | 🔴 | **B** |
| 5 | `V2__orders.sql` — orders, order_items, UNIQUE reference, index | 0.5 | 🔴 | **B** |
| 6 | `V2__payments.sql` — payments, UNIQUE transaction_ref | 0.5 | 🔴 | **B** |
| 7 | `V2__memberships.sql` — memberships, pt_packages, index expiry | 0.5 | 🔴 | **B** |
| 8 | `V3__trainers.sql` — trainers, trainer_assignments | 0.5 | 🔴 | **C** |
| 9 | `V3__classes.sql` — classes, class_sessions, index date | 0.5 | 🔴 | **C** |
| 10 | `V3__bookings.sql` — bookings UNIQUE(customer,session), attendance | 0.5 | 🔴 | **C** |
| 11 | `V3__pt_sessions.sql` — pt_sessions, completion_ref UNIQUE | 0.5 | 🔴 | **C** |
| 12 | `V4__exercises.sql` — exercises, index muscle_group | 0.5 | 🔴 | **D** |
| 13 | `V4__workout_plans.sql` — workout_plans, workout_plan_exercises | 0.5 | 🔴 | **D** |
| 14 | `V4__workout_logs.sql` — workout_sessions, workout_logs, body_metrics | 0.5 | 🔴 | **D** |
| 15 | `V4__ai_tables.sql` — ai_conversations, ai_messages, ai_recommendations JSONB | 0.5 | 🔴 | **D** |

**✅ DB: A = 2h | B = 2h | C = 2h | D = 2h — BẰNG NHAU**

---

## LAYER 2 — BACKEND (48h tổng → 12h/người)

> Chia theo domain, mỗi người 12h chính xác. **Cả 4 người đều tự viết Spring Boot WebClient/RestClient** để kết nối trực tiếp với microservice Python AI của mình (không ai làm proxy hộ ai).

### Backend — Người A (12h) — Auth + Lead CRM + Dashboard Stats + Lead AI Client

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 16 | `POST /api/auth/register` — BCrypt, tạo user, trả JWT | 1.0 | 🔴 |
| 17 | `POST /api/auth/login` — verify password, access(15m) + refresh(7d) | 1.0 | 🔴 |
| 18 | `POST /api/auth/refresh` — trả access token mới | 0.5 | 🔴 |
| 19 | `POST /api/auth/logout` — Redis blacklist | 0.5 | 🔴 |
| 20 | `JwtAuthFilter` — đọc Bearer token, set SecurityContext | 1.0 | 🔴 |
| 21 | `@PreAuthorize` RBAC annotations trên controllers | 0.5 | 🔴 |
| 22 | Global exception handler — JSON chuẩn, không leak stack trace | 0.5 | 🔴 |
| 23 | `POST /api/leads` — tạo lead | 0.5 | 🔴 |
| 24 | `GET /api/leads` — list + filter(status,source) + search + pagination | 1.0 | 🔴 |
| 25 | `GET /api/leads/:id` — detail + activities timeline | 0.5 | 🔴 |
| 26 | `PUT /api/leads/:id` + `PATCH /leads/:id/status` | 0.5 | 🔴 |
| 27 | `POST /api/leads/:id/activities` (CALL, MEETING, CONSULTATION, TRIAL, NOTE — Spec 4.4, 4.5) + `PATCH /leads/:id/assign` | 0.5 | 🔴 |
| 28 | `GET /api/dashboard/stats` + `revenue-chart` + `crm-funnel` + **`LeadAIClient` (WebClient gọi `ai-crm:8001`)** | 1.5 | 🔴 |
| 29 | `POST/GET /api/body-metrics/me` | 0.5 | 🔴 |
| 30 | `GET /api/progress/me` — workouts/month, streak (1.0h 🔴) + PRs analytics (0.5h 🟡) | 1.5 | 🔴 / 🟡* |
| 31 | Cron job: auto-complete sessions sau end_time | 0.5 | 🟡 |
| **A tổng BE** | | **12h** | |

### Backend — Người B (12h) — Customers + Orders + Payments + Membership + Commerce AI Client

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 32 | `POST /api/auth/forgot-password` + `reset-password` | 1.0 | 🟡 |
| 33 | `PUT /api/auth/change-password` | 0.5 | 🔴 |
| 34 | `GET /api/customers` — list + filter + search | 0.5 | 🔴 |
| 35 | `GET /api/customers/:id` — aggregate profile (join 4 module) | 1.0 | 🔴 |
| 36 | `PUT /api/customers/:id` — edit profile | 0.5 | 🔴 |
| 37 | `GET/POST/PUT/PATCH /api/plans` — CRUD plans | 1.0 | 🔴 |
| 38 | `POST /api/orders` — server-side tính total, validate | 1.5 | 🔴 |
| 39 | `GET /api/orders` + `GET /api/orders/me` + `GET /:id` | 0.5 | 🔴 |
| 40 | `PATCH /api/orders/:id/cancel` — chỉ PENDING | 0.5 | 🔴 |
| 41 | `POST /api/payments` — process + trigger ServiceActivation | 1.5 | 🔴 |
| 42 | `POST /api/payments/callback` — idempotent webhook | 1.0 | 🔴 |
| 43 | `ServiceActivationService` + **`CommerceAIClient` (WebClient gọi `ai-commerce:8002`)** | 1.0 | 🔴 |
| 44 | `MembershipValidator` bean — `requireActive(customerId)` | 0.5 | 🔴 |
| 45 | `GET /api/reports/sales` + `memberships` + `classes` | 1.0 | 🟡 |
| **B tổng BE** | | **12h** | |

### Backend — Người C (12h) — Lead Convert + Membership Lifecycle + Booking + Operations AI Client

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 46 | `POST /api/leads/:id/convert` — state machine, tạo Customer+User, preserve history (phụ thuộc V1 migration A, độc lập với Lead CRUD) | 2.0 | 🔴 |
| 47 | `PATCH /api/leads/:id/lost` | 0.5 | 🔴 |
| 48 | `AuditLogAspect` — @Around @Auditable | 1.0 | 🟡 |
| 49 | `GET/PATCH /api/memberships` — list, pause, resume, cancel | 1.5 | 🔴 |
| 50 | `POST /api/memberships/:id/renew` + `GET /memberships/expiring` | 0.5 | 🔴 |
| 51 | `POST /api/bookings` — SELECT FOR UPDATE + capacity + concurrency | 2.0 | 🔴 |
| 52 | `GET /api/bookings/me` + `GET /api/bookings` | 0.5 | 🔴 |
| 53 | `PATCH /api/bookings/:id/cancel` + `no-show` | 0.5 | 🔴 |
| 54 | `POST /api/checkin` — validate membership + tạo attendance + **`OperationsAIClient` (WebClient gọi `ai-operations:8003`)** | 1.0 | 🔴 |
| 55 | `GET /api/attendance` + `GET /api/attendance/me` | 0.5 | 🔴 |
| 56 | `GET/POST/PUT /api/exercises` + seed 30 exercises | 1.0 | 🔴 |
| 57 | `GET/POST/PUT /api/users` (ADMIN) | 1.0 | 🟡 |
| **C tổng BE** | | **12h** | |

### Backend — Người D (12h) — Trainers + PT + Classes + Workout + Workout AI Client

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 58 | `GET/POST/PUT /api/trainers` + assign customer | 1.0 | 🔴 |
| 59 | `GET /api/trainers/:id/schedule` | 0.5 | 🔴 |
| 60 | `GET/POST/PUT /api/classes` | 0.5 | 🔴 |
| 61 | `GET/POST/PUT /api/sessions` + PATCH cancel | 1.0 | 🔴 |
| 62 | `GET /api/pt-packages/me` + `PATCH /:id/use` idempotent @Version | 1.0 | 🔴 |
| 63 | `POST /api/pt-sessions` — schedule PT, validate trainer available | 1.0 | 🔴 |
| 64 | `GET /api/pt-sessions` + `PATCH reschedule/cancel` | 0.5 | 🔴 |
| 65 | `PATCH /api/pt-sessions/:id/complete` — idempotent via completion_ref | 1.0 | 🔴 |
| 66 | `POST/GET/PUT /api/workout-plans` + PATCH activate/complete/cancel | 2.0 | 🔴 |
| 67 | `POST /api/workout-sessions` + `PATCH complete` + `GET /me` | 1.0 | 🔴 |
| 68 | `POST /api/workout-logs` + `GET /:id/logs` | 0.5 | 🔴 |
| 69 | `POST /api/ai/chat` proxy + **`WorkoutAIClient` (WebClient gọi `ai-workout:8004`)** | 1.0 | 🔴 |
| 70 | `GET /api/ai/conversations/me` + `POST /api/ai/recommend` + accept/dismiss | 1.0 | 🟡 |
| **D tổng BE** | | **12h** | |

**✅ BE: A = 12h | B = 12h | C = 12h | D = 12h — BẰNG NHAU (Ai cũng viết Spring Boot AI Client)**

---

## LAYER 3 — FRONTEND (48h tổng → 12h/người)

> Chia theo domain, mỗi người 12h chính xác. **Cả 4 người đều sở hữu Dashboard/View đồ họa ấn tượng**:
> • **Người A**: Sở hữu Manager Dashboard (`/dashboard`) với 4 KPI cards + CSS Revenue Chart + CRM Funnel (khớp hoàn hảo với BE thống kê #28 của A).
> • **Người B**: Sở hữu Sales Dashboard (`/dashboard/sales`) + Customer 360 Aggregated Profile (6 tabs) + Create Sale Wizard.
> • **Người C**: Sở hữu 7-column Weekly Schedule Calendar (`/dashboard/schedule`) + Real-time Check-in strip.
> • **Người D**: Sở hữu Trainer Schedule Dashboard (`/dashboard/trainer`) + Workout Builder + Member Progress Chart + AI Coach Chat UI.

### Frontend — Người A (12h) — Auth + Lead CRM + Member Home + Manager Dashboard

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 71 | `/login` — form + JWT cookie + redirect theo role | 1.0 | 🔴 |
| 72 | `/register` — form + role selector | 1.0 | 🔴 |
| 73 | `AuthContext` + `ProtectedRoute` + `RoleGuard` + `middleware.ts` | 1.5 | 🔴 |
| 74 | `/dashboard/leads` — filter tabs + search debounce + table + score badge | 1.5 | 🔴 |
| 75 | `/dashboard/leads/create` — form tạo lead | 1.0 | 🔴 |
| 76 | `/dashboard/leads/[id]` — info + timeline + add activity modal (log Consultation & Trial) + actions | 2.0 | 🔴 |
| 77 | `/dashboard/member` — home: 3 cards + quick actions + activity feed | 2.0 | 🔴 |
| 78 | `/dashboard` Manager Overview — 4 KPI cards + pure CSS revenue chart + CRM funnel + occupancy | 2.0 | 🔴 / 🟡* |
| **A tổng FE** | *(Trong đó: 1.0h 🔴 Overview KPI & Revenue Chart, 1.0h 🟡 Funnel Analytics tab)* | **12h** | |

### Frontend — Người B (12h) — Plans + Customers + Sales + Payments

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 79 | `/` landing — Hero + features + CTA | 0.5 | 🔴 |
| 80 | `/plans` — 3 plan cards, price, features list, CTA | 1.0 | 🔴 |
| 81 | `/dashboard/customers` — table với membership badge, expiry | 1.0 | 🔴 |
| 82 | `/dashboard/customers/[id]` — 6 tabs: Overview/Membership/Bookings/Workouts/Payments/Activity | 2.0 | 🔴 |
| 83 | `/dashboard/sales` — 4 KPI cards + transaction table | 1.5 | 🔴 |
| 84 | `/dashboard/create-sale` — 3-step wizard: chọn customer → chọn plan → xác nhận | 2.0 | 🔴 |
| 85 | `/dashboard/payment-confirmed` — success screen + receipt + countdown | 1.0 | 🔴 |
| 86 | `/dashboard/member/payments` — membership card + payment history table | 1.5 | 🔴 |
| 87 | Membership status badge component (ACTIVE/PAUSED/EXPIRED) | 0.5 | 🔴 |
| 88 | Renewal risk widget — expiring list + AI risk badge | 1.0 | 🟡 |
| **B tổng FE** | | **12h** | |

### Frontend — Người C (12h) — Classes + Schedule + Bookings + Profile

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 89 | `/classes` — class grid + filter chips + type badges | 1.0 | 🔴 |
| 90 | `/dashboard/profile` — xem + edit profile + đổi password | 1.0 | 🔴 |
| 91 | `/dashboard/plans` — table + toggle status + create modal (ADMIN/MANAGER) | 1.0 | 🔴 |
| 92 | `/dashboard/schedule` — 7-col weekly calendar + session cards màu theo type | 2.5 | 🔴 |
| 93 | `/dashboard/bookings` — check-in strip + stats + bookings table | 2.0 | 🔴 |
| 94 | `/dashboard/member/book` — tabs Classes/PT + date strip + class cards | 2.0 | 🔴 |
| 95 | Book confirmation modal component | 0.5 | 🔴 |
| 96 | No-show prediction badge component | 0.5 | 🔴 |
| 97 | Schedule AI suggestion banner | 0.5 | 🔴 |
| 98 | `/dashboard/users` (ADMIN) — list + change role + disable | 1.0 | 🟡 |
| **C tổng FE** | | **12h** | |

### Frontend — Người D (12h) — Trainer + Workout + AI Coach + Reports

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 99 | `/dashboard/trainer` — today timeline + clients list + PT management | 2.0 | 🔴 |
| 100 | `/dashboard/workout-builder` — plan form + week tabs + day cards + exercise sidebar | 2.5 | 🔴 |
| 101 | `/dashboard/member/progress` — stats + pure CSS bar chart + PRs table + AI summary | 2.0 | 🔴 |
| 102 | `/dashboard/member/ai-coach` — chat bubbles + input + quick prompts + recommend card | 2.0 | 🔴 |
| 103 | `/dashboard/reports` (nhận từ A) — tabs Sales/Members/Classes báo cáo chuyên sâu | 1.0 | 🟡 |
| 104 | `/dashboard/member/workout-log` + `ExerciseDetailDrawer` component (log sets/reps + drawer video kỹ thuật) | 2.5 | 🔴 |
| **D tổng FE** | | **12h** | |

**✅ FE: A = 12h | B = 12h | C = 12h | D = 12h — BẰNG NHAU (11.0h 🔴, 1.0h 🟡 mỗi người)**

---

## LAYER 4 — AI SERVICE (20h tổng → 5h/người)

> **CẢ 4 NGƯỜI ĐỀU LÀM HYBRID AI TIÊN TIẾN (CONTEXT INJECTION TỪ DB + PROMPT ENGINEERING + SAFETY)**.
> Không ai chỉ làm prompt đơn sơ hay rule-based thô sơ:
> • Mỗi service đều có kiến trúc chuyên nghiệp: `main.py`, `schemas.py`, module `services/*_context_loader.py`, prompt templates và bộ test tự động.

### AI — Người A viết `ai-crm/` (port 8001) (5h) — Lead Intelligence & Persona Engine

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 105 | Setup FastAPI app, `main.py`, `schemas.py`, `requirements.txt`, bearer token | 0.5 | 🔴 |
| 106 | `services/lead_context_loader.py` — load lead profile, lịch sử cuộc gọi/tư vấn/tập thử vào rich context | 1.0 | 🔴 |
| 107 | `POST /score-lead` — Hybrid Scoring: rule-based (0–100) kết hợp NLP intent detection | 1.0 | 🔴 |
| 108 | `POST /suggest-followup` — LLM: Prompt Engineering xây dựng Lead Persona & kịch bản chăm sóc cá nhân hóa | 1.5 | 🔴 |
| 109 | `POST /conversion-tips` (🟡) + Python unit tests (pytest scoring ranges & context builder isolation) | 1.0 | 🔴 / 🟡* |
| **A tổng AI** | *(Trong đó: 0.5h 🟡 conversion-tips, 0.5h 🔴 tests)* | **5h** | |

### AI — Người B viết `ai-commerce/` (port 8002) (5h) — Churn Prediction & Dynamic Retention Engine

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 110 | Setup FastAPI app, `main.py`, `schemas.py`, `requirements.txt`, bearer token | 0.5 | 🔴 |
| 111 | `services/spending_context_loader.py` — load lịch sử chi tiêu, số ngày vắng mặt, tần suất check-in | 1.0 | 🔴 |
| 112 | `POST /predict-renewal` — Churn Risk Model: phân tích tần suất tập giảm dần → xác suất tái tục 0–1 | 1.0 | 🔴 |
| 113 | `POST /renewal-action` — LLM: Prompt Engineering sinh **Dynamic Retention Voucher** (tính % giảm giá & lý do giữ chân) | 1.5 | 🔴 |
| 114 | `POST /suggest-upsell` (gợi ý nâng cấp gói) + Python unit tests (pytest churn probability & discount bounds) | 1.0 | 🔴 |
| **B tổng AI** | | **5h** | |

### AI — Người C viết `ai-operations/` (port 8003) (5h) — No-Show Engine & Smart Scheduling Optimizer

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 115 | Setup FastAPI app, `main.py`, `schemas.py`, `requirements.txt`, bearer token | 0.5 | 🔴 |
| 116 | `services/schedule_context_loader.py` — load ma trận lịch tuần, occupancy rate, lịch sử vắng của hội viên | 1.0 | 🔴 |
| 117 | `POST /predict-noshow` — No-Show Risk Engine: phân tích lead-time đặt trước, khung giờ, lịch sử vắng | 1.0 | 🔴 |
| 118 | `POST /suggest-schedule` — LLM: Prompt Engineering giải bài toán tối ưu ca dạy Trainer & gợi ý mở thêm slot giờ vàng | 1.5 | 🔴 |
| 119 | `POST /trainer-tips` (coaching advice) + Python unit tests (pytest schedule matrix & thresholds) | 1.0 | 🔴 |
| **C tổng AI** | | **5h** | |

### AI — Người D viết `ai-workout/` (port 8004) (5h) — AI Workout Coach & Safety Guardrails

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 120 | Setup FastAPI app, `main.py`, `schemas.py` + `safety_filter.py` (BLOCKED_TERMS list chất cấm & bệnh lý) | 0.5 | 🔴 |
| 121 | `services/fitness_context_loader.py` — load thông số thể lực, chấn thương, lịch sử sets/reps gần nhất | 1.0 | 🔴 |
| 122 | `POST /chat` — AI Workout Coach: Safety Guard → Context Injection → OpenAI call → phản hồi cá nhân hóa | 1.5 | 🔴 |
| 123 | `POST /recommend` — LLM sinh JSON structured output `suggested_changes` cho giáo án tập luyện | 1.0 | 🟡 |
| 124 | Python tests: pytest safety filter blocks, context loader isolation, mockup OpenAI call | 1.0 | 🔴 |
| **D tổng AI** | | **5h** | |

**✅ AI: A = 5h | B = 5h | C = 5h | D = 5h — BẰNG NHAU (Cả 4 đều làm Context Injection + Prompt Engineering)**

---

## LAYER 5 — OPS / DEVOPS (12h tổng → 3h/người)

> Mỗi người đều viết Dockerfile + CI pipeline + 1 phần deploy. Không ai thiếu kinh nghiệm OPS.

### OPS — Người A (3h)

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 125 | `ai-crm/Dockerfile` — python:3.11-slim, expose 8001 | 0.5 | 🔴 |
| 126 | `.env.example` — tất cả variables: DB, Redis, JWT, OpenAI, ports | 0.5 | 🔴 |
| 127 | `.github/workflows/ci-backend.yml` — mvn test với postgres service | 1.0 | 🔴 |
| 128 | `.github/workflows/ci-ai-crm.yml` — pytest ai-crm | 0.5 | 🔴 |
| 129 | Deploy backend lên Railway/Render + smoke test | 0.5 | 🔴 |
| **A tổng OPS** | | **3h** | |

### OPS — Người B (3h)

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 130 | `frontend/Dockerfile` — multi-stage Next.js standalone output | 0.5 | 🔴 |
| 131 | `ai-commerce/Dockerfile` — python:3.11-slim, expose 8002 | 0.5 | 🔴 |
| 132 | `.github/workflows/ci-frontend.yml` — npm ci + npm run build | 0.5 | 🔴 |
| 133 | `.github/workflows/ci-ai-commerce.yml` — pytest ai-commerce | 0.5 | 🔴 |
| 134 | Deploy frontend lên Vercel/Railway + smoke test | 1.0 | 🔴 |
| **B tổng OPS** | | **3h** | |

### OPS — Người C (3h)

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 135 | `ai-operations/Dockerfile` — python:3.11-slim, expose 8003 | 0.5 | 🔴 |
| 136 | `docker-compose.yml` phần infra: postgres + redis + healthcheck + volumes | 0.5 | 🔴 |
| 137 | `docker-compose.yml` phần services: backend + frontend + 4 AI + env vars | 1.0 | 🔴 |
| 138 | `.github/workflows/ci-ai-operations.yml` — pytest ai-operations | 0.5 | 🔴 |
| 139 | `.github/workflows/ci-integration.yml` — integration test với real postgres | 0.5 | 🔴 |
| **C tổng OPS** | | **3h** | |

### OPS — Người D (3h)

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 140 | `backend/Dockerfile` — multi-stage Java 21 (builder + runner) | 0.5 | 🔴 |
| 141 | `ai-workout/Dockerfile` — python:3.11-slim, expose 8004 | 0.5 | 🔴 |
| 142 | `Makefile` — make up / down / migrate / seed / logs (lệnh dev & CI/CD core) | 0.5 | 🔴 |
| 143 | `.github/workflows/ci-ai-workout.yml` — pytest ai-workout | 0.5 | 🔴 |
| 144 | `.github/workflows/cd-deploy.yml` — build + push GHCR + deploy Railway | 1.0 | 🔴 |
| **D tổng OPS** | | **3h** | |

**✅ OPS: A = 3h | B = 3h | C = 3h | D = 3h — BẰNG NHAU**

---

## LAYER 6 — TESTING (12h tổng → 3h/người)

> Mỗi người viết unit tests cho domain mình + 1 integration test E2E. Đặc biệt áp dụng **Cross-Module Testing & Pair Programming** ở các module trọng yếu để đảm bảo tính khách quan và kiểm chứng chặt chẽ các ràng buộc toàn vẹn dữ liệu.

### Testing — Người A (3h)

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 145 | Unit test `AuthService`: register, login, refresh, logout, disabled user | 0.5 | 🔴 |
| 146 | Unit test `LeadService`: create, update status, convert validation (phối hợp C verify schema) | 0.5 | 🔴 |
| 147 | Unit test `ProgressService`: streak calculation, personal records, edge cases | 0.5 | 🔴 |
| 148 | Integration test E2E: Register → Login → Create Lead → Add Activity → Convert → Customer created | 1.5 | 🔴 |
| **A tổng TEST** | | **3h** | |

### Testing — Người B (3h)

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 149 | Unit test `OrderService`: total calculation, discount, negative total prevention | 0.5 | 🔴 |
| 150 | Unit test `PaymentService`: idempotency same reference, callback duplicate | 0.5 | 🔴 |
| 151 | Unit test `MembershipService`: activate (B code) + pause/resume end_date calculation, cancel (pair-testing với C — cross-testing) | 0.5 | 🔴 |
| 152 | Integration test E2E: Create Order → Process Payment → Membership ACTIVE → Verify access | 1.5 | 🔴 |
| **B tổng TEST** | | **3h** | |

### Testing — Người C (3h)

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 153 | Unit test `BookingService`: capacity check, duplicate booking prevention | 0.5 | 🔴 |
| 154 | **Concurrency test**: 2 CompletableFuture cùng book last slot → đúng 1 thành công | 1.0 | 🔴 |
| 155 | Unit test PT complete idempotent: pair-testing với D (cross-testing: gọi 2 lần → decrement 1 lần, verify completion_ref UNIQUE) | 0.5 | 🔴 |
| 156 | Integration test E2E: Book Class → Check-in → Attendance recorded → Verify history | 1.0 | 🔴 |
| **C tổng TEST** | | **3h** | |

### Testing — Người D (3h)

| # | Task | h | 🔴/🟡 |
|---|------|---|-------|
| 157 | Unit test `WorkoutPlanService`: activate deactivates previous, complete, cancel | 0.5 | 🔴 |
| 158 | Unit test `WorkoutLogService`: log creation, progress computation | 0.5 | 🔴 |
| 159 | Unit test AI safety_filter + context_loader isolation (không leak data) | 0.5 | 🔴 |
| 160 | Integration test E2E: Create Workout Plan → Start Session → Log Exercise → Progress computed | 1.5 | 🔴 |
| **D tổng TEST** | | **3h** | |

**✅ TEST: A = 3h | B = 3h | C = 3h | D = 3h — BẰNG NHAU**

---

## TỔNG KẾT — BẢNG ĐỀU TUYỆT ĐỐI (CHUẨN HÓA SAU AUDIT V8)

### 1. Phân bổ theo Layer (Bằng nhau 100%)

| Layer | Người A | Người B | Người C | Người D | Đánh giá |
|-------|---------|---------|---------|---------|----------|
| **DB** | 2h | 2h | 2h | 2h | ✅ Đều tuyệt đối |
| **BE** | 12h | 12h | 12h | 12h | ✅ Đều tuyệt đối |
| **FE** | 12h | 12h | 12h | 12h | ✅ Đều tuyệt đối |
| **AI** | 5h | 5h | 5h | 5h | ✅ Đều tuyệt đối |
| **OPS** | 3h | 3h | 3h | 3h | ✅ Đều tuyệt đối |
| **TEST** | 3h | 3h | 3h | 3h | ✅ Đều tuyệt đối |
| **TỔNG GIỜ** | **37h** | **37h** | **37h** | **37h** | ✅ **KHÔNG CHÊNH LỆCH (0h)** |

### 2. Phân bổ theo Độ ưu tiên (🔴 Must-Have vs 🟡 Nice-To-Have)

| Độ ưu tiên | Người A | Người B | Người C | Người D | Ghi chú & Đánh giá |
|------------|---------|---------|---------|---------|---------------------|
| 🔴 **Must-Have** (Core MVP) | **34.5h** | **34.0h** | **34.0h** | **34.0h** | Khối lượng lõi ngang ngửa (chênh lệch tối đa ≤ 0.5h) |
| 🟡 **Nice-To-Have** (Enhancements) | **2.5h** | **3.0h** | **3.0h** | **3.0h** | Phân bổ phụ đều đặn, tuần 5 làm nếu thừa thời gian |
| **TỔNG CỘNG** | **37.0h** | **37.0h** | **37.0h** | **37.0h** | **Cân bằng hoàn hảo cả về lượng và chất** |

> ✅ **KHÔNG CHÊNH LỆCH**: Mọi người bằng nhau ở mọi layer và đồng đều mức độ chịu trách nhiệm cho MVP.
> ✅ **FULLSTACK TOÀN DIỆN**: Mỗi thành viên tự triển khai đầy đủ chu trình: Thiết kế DB Schema → Code API Spring Boot → Lập trình Giao diện Next.js → Xây dựng Microservice AI Python → Cấu hình Docker & CI/CD → Viết Unit Test & E2E Integration Test.
> ✅ **CV-READY CHUẨN MỰC**: Mỗi cá nhân đều tự tin khẳng định trên CV: *"Fullstack Software Engineer (Java, Spring Boot, Next.js, Python, PostgreSQL, Docker, CI/CD, E2E Testing)"*.

---

## LAYER CHECK — MỖI NGƯỜI ĐỦ 6 LAYER + SỞ HỮU CÔNG NGHỆ CAO ĐỒNG ĐỀU

| Layer | A | B | C | D |
|-------|---|---|---|---|
| **DB** | users, leads, customers | plans, orders, payments, memberships | trainers, classes, bookings, pt_sessions | exercises, workout_plans, workout_logs, ai_tables |
| **BE** | Auth JWT, Lead CRM, Dashboard Stats, Progress, `LeadAIClient` (WebClient 8001) | Customers, Orders, Payments, ServiceActivation, Membership, `CommerceAIClient` (WebClient 8002) | Lead convert, Membership lifecycle, Booking concurrency, Check-in, Exercises, Users CRUD, `OperationsAIClient` (WebClient 8003) | Trainers, PT sessions, Classes, Workout CRUD, AI chat proxy, `WorkoutAIClient` (WebClient 8004) |
| **FE** | /login, /register, /leads, /member-home, **/dashboard (Manager Overview: 4 KPI + pure CSS revenue chart + CRM funnel)** | /landing, /plans, /customers, /dashboard/customers/[id] (6 tabs 360 profile), **/dashboard/sales (4 KPI + transactions), /dashboard/create-sale (3-step wizard)** | /classes, /profile, /dashboard/plans, **/dashboard/schedule (7-col weekly calendar), /dashboard/bookings (real-time checkin strip)**, /member/book | **/dashboard/trainer (timeline + schedule), /dashboard/workout-builder (interactive week tabs + exercise sidebar)**, /member/progress (CSS chart), /member/ai-coach, /reports, ExerciseDetailDrawer |
| **AI** | ai-crm (8001): Lead Context Loader + Hybrid Scoring (0–100) + Persona Followup Prompt | ai-commerce (8002): Spending Context Loader + Churn Risk Model + Dynamic Retention Offer Prompt | ai-operations (8003): Schedule Matrix Loader + No-Show Risk Engine + Smart Slot Optimizer Prompt | ai-workout (8004): Fitness Context Loader + Safety Filter Guard + Conversational Workout Coach |
| **OPS** | ai-crm Dockerfile + CI backend Postgres + .env.example + deploy BE | frontend Dockerfile + ai-commerce Dockerfile + CI FE + deploy FE | ai-ops Dockerfile + docker-compose full (8 containers) + CI integration | backend Dockerfile + ai-workout Dockerfile + Makefile + CD pipeline deploy Cloud |
| **TEST** | Auth + Lead + Progress unit + E2E CRM flow | Order + Payment + Membership unit + E2E commerce flow | Booking + Concurrency + PT idempotent + E2E booking flow | Workout + Log + AI safety unit + E2E workout flow |

---

## TEAMWORK & PHỐI HỢP TOÀN DIỆN

### 1. Phối hợp dòng dữ liệu xuyên module (Cross-Module Flow):

| Tuần | Bên cung cấp | Bên tiếp nhận | Nội dung phối hợp kỹ thuật cụ thể |
|------|-------------|--------------|-----------------------------------|
| **Tuần 1** | A → Nhóm | B, C, D | A hoàn thành Migration V1 (ngày 1–2) cho leads & customers → C bắt đầu triển khai Lead Convert logic ngay. Cuối ngày 3, A xong Auth JWT Filter → cung cấp dev token cho B, C, D test API có xác thực. |
| **Tuần 2** | B → C | C | B hoàn thành Payment & ServiceActivation → C sử dụng để kiểm thử trạng thái Membership kích hoạt thực tế trong chu trình Check-in và Lifecycle. |
| **Tuần 2** | A → C | C | A hoàn thành Lead CRUD API → C dùng để test toàn diện flow Convert Lead từ giao diện người dùng. |
| **Tuần 2** | C → D | D | C hoàn tất cấu trúc Booking & Sessions → D dùng để kiểm thử đặt lịch PT Session và đồng bộ lịch huấn luyện viên. |
| **Tuần 3** | D → A | A | D hoàn thành Workout Sessions & Workout Logs → A sử dụng bảng workout_logs để tính toán streak và personal records cho API Progress. |
| **Tuần 3** | Cả 4 thành viên | Cả 4 | Mỗi người tự viết Spring Boot WebClient (`LeadAIClient`, `CommerceAIClient`, `OperationsAIClient`, `WorkoutAIClient`) kết nối sang microservice Python của mình (port 8001–8004). |
| **Tuần 3** | B → D | D | B hoàn tất MembershipValidator bean → D nhúng vào workout builder & PT booking để chặn hội viên không có gói tập hợp lệ. |
| **Tuần 4** | Cả 4 thành viên | Cả nhóm | Thực hiện chuỗi E2E Cross-Testing: mỗi thành viên chạy luồng kiểm thử đi qua module của 3 thành viên khác. |

---

### 2. Ma trận Cross-Module Testing & Pair-Testing (Lý do & Giá trị kỹ thuật):

| Test Task | Người viết Test | Người code Service | Lý do kỹ thuật & Giá trị hợp tác | Cách phối hợp (Pair Programming) |
|-----------|-----------------|--------------------|-----------------------------------|-----------------------------------|
| **#155: PT Complete Idempotent** (0.5h) | **C** | **D** | • C thiết kế migration `V3__pt_sessions.sql` (#11) với constraint `UNIQUE (completion_ref)`.<br>• D code service method `PATCH /api/pt-sessions/:id/complete` (#65).<br>• C viết unit test đảm bảo khi gọi 2 lần với cùng `completion_ref`, DB constraint và exception handling hoạt động đúng, session chỉ bị trừ đúng 1 lần. | C và D pair-programming: C cung cấp mock scenario và unique violation verification, D đối soát transactional rollback logic. |
| **#151: Membership Lifecycle** (0.5h) | **B** | **C** | • B thiết kế migration `V2__memberships.sql` (#7) và code `ServiceActivationService` (#43).<br>• C code các endpoint lifecycle `GET/PATCH /api/memberships` (#49 - pause, resume, cancel).<br>• B viết unit test cho lifecycle để đảm bảo công thức tính toán `end_date` sau khi resume giữ nguyên tổng số ngày hội viên đã thanh toán. | B và C pair-programming: B định nghĩa state machine mẫu (PENDING → ACTIVE → PAUSED → ACTIVE → EXPIRED), C kiểm chứng logic Hibernate dirty checking. |
| **#148: CRM to Customer E2E Flow** (1.5h) | **A** | **C & B** | • A quản lý Lead CRM, C quản lý Convert logic (#46), B quản lý Customer profile (#35).<br>• A viết E2E test đi qua toàn bộ hành trình chuyển đổi để đảm bảo dữ liệu lịch sử Lead activities không bị thất thoát khi thành Customer. | A đóng vai trò QA chính cho luồng CRM, C và B hỗ trợ setup MockMvc / WebTestClient integration context. |

---

### 3. Quy chuẩn giải quyết triệt để Dependency Tuần 1:

- **Vấn đề**: C có task `POST /api/leads/:id/convert` (#46) ở Tuần 1, trong khi A hoàn thành trọn bộ API Lead CRUD ở Tuần 2.
- **Giải pháp kỹ thuật chuẩn xác**:
  1. A cam kết ưu tiên đẩy `V1__leads.sql` và `V1__customers.sql` lên nhánh develop ngay trong **Ngày 1 hoặc nửa đầu Ngày 2 Tuần 1**.
  2. Logic Convert Lead của C là nghiệp vụ chuyển đổi nội bộ: đọc dữ liệu từ `LeadRepository`, xác thực trạng thái `QUALIFIED`, tạo bản ghi trong `CustomerRepository` và tài khoản trong `UserRepository`, sau đó cập nhật cờ `is_converted = true`.
  3. C **KHÔNG phụ thuộc** vào các endpoint CRUD Lead khác của A (như filter, search, pagination). C chỉ cần Database Entities đã sinh từ Migration V1.
  4. C có thể viết và hoàn thành 100% logic Convert + Unit Test độc lập ngay trong Tuần 1. Khi A lên xong CRUD API ở Tuần 2, hai người ghép lại trong 5 phút.

---

### 4. Bảng đối chiếu 100% Spec Coverage (Không bỏ sót tính năng):

| Nghiệp vụ trong Functional Specification | Mức độ đáp ứng | Task phụ trách trong V8 | Ghi chú kỹ thuật |
|------------------------------------------|----------------|--------------------------|-------------------|
| **1. Authentication & Authorization** (JWT, Refresh, Blacklist, RBAC) | 100% | #16–21, #32–33, #71–73 | A & B chia sẻ; bảo mật 2 lớp Access/Refresh token. |
| **2. Lead CRM Pipeline & Scoring** | 100% | #23–27, #74–76, #106–108 | A phụ trách trọn vẹn FE, BE và Python AI CRM. |
| **3. Consultation & Trial Management** (Spec 4.4, 4.5) | 100% | #27 (BE), #76 (FE) | Tích hợp tinh gọn vào Lead Activities với enum `activity_type` (`CONSULTATION`, `TRIAL`), hỗ trợ ghi chú kết quả, ngày hẹn và tình trạng tập thử. |
| **4. Customer 360 Aggregated Profile** | 100% | #34–36, #81–82 | B phụ trách; tổng hợp 6 tab: thông tin, gói tập, lịch đặt, bài tập, thanh toán, tương tác. |
| **5. Service Plans & Pricing** | 100% | #37, #91 | B phụ trách API CRUD; C phụ trách giao diện quản trị Plans (#91 🔴). |
| **6. Orders, Payments & Idempotent Webhook** | 100% | #38–42, #83–86 | B phụ trách; Server-side pricing chống gian lận; webhook xử lý idempotent. |
| **7. Service Activation Engine** | 100% | #43, #44 | B phụ trách; phân tách rành mạch logic kích hoạt Membership vs PT Package. |
| **8. Membership Lifecycle Management** | 100% | #49–50, #87 | C phụ trách API (Pause, Resume, Renew, Expiring); B phụ trách Badge hiển thị. |
| **9. Class & Session Scheduling** | 100% | #60–61, #89, #92 | D phụ trách BE CRUD lớp học; C phụ trách Calendar UI 7 cột tuần (#92). |
| **10. Booking Engine & Concurrency** | 100% | #51–53, #93–95, #154 | C phụ trách; bảo vệ chống overbooking bằng `SELECT FOR UPDATE` + Pessimistic Lock + Unique Index. |
| **11. Check-in & Attendance Validation** | 100% | #54–55, #93 | C phụ trách; kiểm tra hiệu lực membership thời gian thực trước khi ghi nhận check-in. |
| **12. PT Package Usage & Scheduling** | 100% | #62–65, #99 | D phụ trách; trừ số buổi PT bằng Optimistic Locking (`@Version`), hoàn tất buổi tập idempotent qua UUID `completion_ref`. |
| **13. Exercise Library & Seeding** | 100% | #56, #104b | C phụ trách seed 30 bài; D phụ trách Exercise Detail Drawer UI component. |
| **14. Workout Plans & Execution Logs** | 100% | #66–68, #100, #104 | D phụ trách; xây dựng Builder trực quan, ghi chép log sets, reps, weight chi tiết. |
| **15. Metrics, Streak & Personal Records** | 100% | #29–30, #101 | A phụ trách backend tính streak & PRs; D phụ trách biểu đồ tiến độ pure CSS trên frontend. |
| **16. AI CRM Microservice (8001)** | 100% | #28, #105–109 | A viết FastAPI Hybrid AI + Spring Boot `LeadAIClient`: chấm điểm tiềm năng lead (0–100), gợi ý kịch bản chăm sóc cá nhân hóa. |
| **17. AI Commerce Microservice (8002)** | 100% | #43, #110–114, #88 | B viết FastAPI Hybrid AI + Spring Boot `CommerceAIClient`: dự báo nguy cơ không gia hạn, sinh Dynamic Retention Voucher. |
| **18. AI Operations Microservice (8003)** | 100% | #54, #115–119, #96–97 | C viết FastAPI Hybrid AI + Spring Boot `OperationsAIClient`: dự báo no-show, tối ưu hóa xếp lịch ca dạy Trainer. |
| **19. AI Workout Coach Microservice (8004)** | 100% | #69–70, #120–124, #102 | D viết FastAPI LLM + Spring Boot `WorkoutAIClient`: tư vấn kế hoạch tập có context cá nhân, bộ lọc an toàn BLOCKED_TERMS. |
| **20. DevOps, Docker, CI/CD Pipelines** | 100% | #125–144 | A, B, C, D mỗi người viết Dockerfile service mình, CI riêng biệt và tích hợp CD hoàn chỉnh. |
| **21. Unit Test & E2E Verification** | 100% | #145–160 | Cả 4 thành viên hoàn thành 3h test mỗi người với 4 luồng E2E cốt lõi. |

---

### 5. Quy trình Code Review chéo:

| Reviewer | Review code của | Lý do kỹ thuật & Trọng tâm review |
|----------|----------------|-----------------------------------|
| **A** | **B** (Payment & Orders) | A chuyên gia về Auth/JWT → rà soát tính an toàn của token và phân quyền RBAC trên các giao dịch tài chính. |
| **B** | **C** (Booking & Check-in) | B nắm rõ quy tắc bán hàng & hợp đồng → đối soát điều kiện trạng thái gói tập khi hội viên check-in và đặt chỗ. |
| **C** | **D** (Workout & PT Sessions) | C nắm rõ schema lịch tập và phòng tập → rà soát chống xung đột lịch giữa PT session cá nhân và lớp tập nhóm. |
| **D** | **A** (Lead Scoring & AI CRM) | D nắm sâu về kiến trúc prompt & tích hợp LLM → rà soát hiệu năng kết nối proxy từ Spring Boot sang Python AI CRM. |

---

## TIMELINE 5 TUẦN CHI TIẾT

```
TUẦN 1 — Foundation & Schema Setup (Tất cả làm cùng lúc, phối hợp thông suốt)
  A: Migration V1 (users, leads, customers) [Ngày 1-2] → Bàn giao schema cho C
     Code Auth (register, login, refresh, logout) + JwtFilter + RBAC [Ngày 3] → Cung cấp JWT cho cả nhóm test
     Khởi tạo dự án ai-crm FastAPI (port 8001)
  B: Migration V2 (plans, orders, payments, memberships) + Plans CRUD API
     Code đổi mật khẩu (change-password) + Khởi tạo ai-commerce FastAPI (port 8002)
  C: Migration V3 (trainers, classes, bookings, pt_sessions)
     Code logic Lead Convert (#46) dựa trên Entity V1 của A + AuditLogAspect
     Khởi tạo ai-operations FastAPI (port 8003)
  D: Migration V4 (exercises, workout_plans, workout_logs, ai_tables)
     Code Trainers CRUD + Classes CRUD API + Khởi tạo ai-workout FastAPI (port 8004)
  ⚡ Cột mốc Tuần 1: Toàn bộ 4 migration Flyway khớp nối sạch sẽ, Docker Postgres chạy thông suốt, JWT hoạt động.

TUẦN 2 — Core Business Logic & Primary UI (Phối hợp chặt chẽ)
  A: Hoàn thiện trọn bộ Lead CRUD API + Giao diện /login, /register, /dashboard/leads, /dashboard/leads/[id]
  B: Hoàn thiện Customer Profile API + Orders + Payments + ServiceActivation + Giao diện /landing, /plans, /customers
  C: Hoàn thiện Membership Lifecycle + Booking concurrency (SELECT FOR UPDATE) + Check-in API + Giao diện /classes, /schedule, /bookings
  D: Hoàn thiện PT packages (@Version) + PT sessions scheduling + Workout Plans CRUD + Giao diện /dashboard/trainer, /workout-builder
  ⚡ Cột mốc Tuần 2: Luồng nghiệp vụ cơ bản từ đăng ký → nạp khách hàng → đặt lịch lớp học chạy được trên Postman và UI.

TUẦN 3 — Complete Advanced Features & 4 AI Services (Tích hợp AI Microservices & Data Visuals)
  A: Progress API (Streak, PRs) + Body metrics + Giao diện /dashboard/member home + Manager Dashboard Overview (/dashboard)
     Hoàn tất ai-crm (Lead Context Loader + Hybrid Scoring + Followup) + Spring Boot LeadAIClient + Cron job (🟡)
  B: Reports API (🟡) + Giao diện /sales, /create-sale, /payment-confirmed, /member/payments
     Hoàn tất ai-commerce (Spending Context Loader + Churn Model + Dynamic Offer) + Spring Boot CommerceAIClient
  C: Exercises CRUD + Seed 30 bài tập + Admin Users CRUD (🟡) + Giao diện /profile, /member/book, /dashboard/plans
     Hoàn tất ai-operations (Schedule Matrix Loader + No-show Engine + Smart Slots) + Spring Boot OperationsAIClient
  D: Workout sessions + Workout logs + Giao diện /member/progress, /member/ai-coach, /member/workout-log, ExerciseDetailDrawer
     Hoàn tất ai-workout (Fitness Context Loader + Safety Filter + Conversational Coach) + Spring Boot WorkoutAIClient
  ⚡ Cột mốc Tuần 3: Cả 4 Python microservice hoạt động độc lập trên port 8001–8004 và kết nối trơn tru với Spring Boot qua WebClient.

TUẦN 4 — DevOps CI/CD & Cross-Module Testing (Kiểm thử chéo & tự động hóa)
  A: ai-crm Dockerfile + .github/workflows/ci-backend.yml + .env.example + 3 Unit Tests (Auth, Lead, Progress)
     Viết Integration Test E2E #148 (Lead CRM → Convert → Customer Flow)
  B: frontend Dockerfile + ai-commerce Dockerfile + CI Frontend + CI ai-commerce + 3 Unit Tests (Order, Payment, Membership)
     Viết Integration Test E2E #152 (Order → Payment → Membership ACTIVE Flow)
     ⚡ Pair-testing với C trên Membership lifecycle test (#151)
  C: ai-operations Dockerfile + docker-compose.yml (full stack 8 container) + CI ai-operations + CI Integration
     3 Unit Tests (Booking, Concurrency Test #154, PT Idempotent Test #155)
     Viết Integration Test E2E #156 (Book Class → Check-in → Attendance Flow)
     ⚡ Pair-testing với D trên PT Idempotent test (#155)
  D: backend Dockerfile + ai-workout Dockerfile + Makefile (🟡) + CI ai-workout + CD deploy workflow (.github)
     3 Unit Tests (WorkoutPlan, WorkoutLog, AI Safety Filter)
     Viết Integration Test E2E #160 (Workout Plan → Session Execution → Progress Computed Flow)
  ⚡ Cột mốc Tuần 4: Toàn bộ CI test xanh 100%, 4 kịch bản E2E kiểm thử chéo thành công, docker-compose up chạy trọn vẹn.

TUẦN 5 — Cloud Deployment, Polish & Balanced Nice-To-Have (Hoàn thiện xuất sắc)
  A: Deploy backend Spring Boot lên Render/Railway + Smoke test hệ thống
     Hoàn thiện Manager Dashboard Analytics tab (🟡 - 1.0h) + Hỗ trợ fix bugs
  B: Deploy frontend Next.js lên Vercel + Renewal risk widget (🟡 - 1.0h)
     Hoàn thiện Forgot/Reset Password API (🟡 - 1.0h) + Fix bugs
  C: Triển khai docker-compose lên VPS / Server staging + Giao diện /dashboard/users ADMIN (🟡 - 1.0h)
     Giao diện Schedule AI suggestion banner (#97 - 0.5h) + Fix bugs
  D: Cấu hình CD pipeline tự động build GHCR & push production + Makefile commands (🟡 - 0.5h)
     Hoàn thiện /dashboard/reports UI (🟡 - 1.0h) + Gợi ý giáo án AI (#70 🟡, #123 🟡) + Code polish & fix bugs
  ⚡ Cột mốc Tuần 5: Sản phẩm live trên môi trường thực tế, demo mượt mà từ public landing page đến dashboard 4 vai trò.
```

---

## 🌐 CHƯƠNG TRÌNH ĐỒNG BỘ TOÀN DIỆN KIẾN THỨC (ALL-TEAM MASTERY FRAMEWORK)

> **Mục tiêu tối thượng**: Đảm bảo **cả 4 thành viên đều có 100% kiến thức của toàn bộ dự án**. Khi hội đồng vấn đáp bốc thăm bất kỳ ai để hỏi về bất kỳ layer nào, người đó đều trả lời trơn tru, chuẩn xác và sâu sắc như chính mình là tác giả.

### 1. Cơ chế vận hành chia sẻ kiến thức nội bộ:
1. **Rotating Pair-Programming (Lập trình cặp luân phiên)**: Mỗi tuần nhóm dành 2 buổi (mỗi buổi 1h) để code cặp xoay vòng: Tuần 1 (A-B, C-D), Tuần 2 (A-C, B-D), Tuần 3 (A-D, B-C).
2. **Pull Request Walkthrough**: Trước khi merge code vào nhánh `main`, người viết bắt buộc quay video Loom/OBS 3–5 phút hoặc giải thích trực tiếp luồng code cho 3 thành viên còn lại.
3. **Internal Mock Defense (Vấn đáp thử nghiệm)**: Cuối Tuần 4 và Tuần 5, nhóm tổ chức buổi bảo vệ thử, trong đó các thành viên tráo đổi vai trò để phản biện lẫn nhau.

---

### 2. MA TRẬN 8 CHUYÊN ĐỀ KIẾN TRÚC TOÀN DỰ ÁN (BẮT BUỘC CẢ 4 NGƯỜI PHẢI THUỘC LÒNG):

#### 📘 Chuyên đề 1: Bảo mật, Xác thực & Phân quyền (Security & Authentication)
* **Người làm chính**: **Người A** | **Cả 4 người đều phải hiểu sâu**.
* **File mã nguồn trọng tâm**: `backend/src/main/java/com/fitos/security/JwtAuthFilter.java`, `SecurityConfig.java`, `AuthService.java`.
* **Bản chất kỹ thuật**:
  * **Access Token (15 phút)**: Mang thông tin `userId`, `role`, ký bằng HMAC-SHA256 với secret key. Lưu ở HTTP-only cookie hoặc client memory.
  * **Refresh Token (7 ngày)**: Lưu trong database, dùng để cấp access token mới qua endpoint `/api/auth/refresh` khi token cũ hết hạn mà không bắt user login lại.
  * **Redis Blacklist**: Khi logout (`POST /api/auth/logout`), token signature được lưu vào Redis với thời gian sống (TTL) bằng thời hạn còn lại của access token. Mọi request đi qua `JwtAuthFilter` đều kiểm tra `redisTemplate.hasKey(token)`.
  * **RBAC**: Phân quyền 4 vai trò (`ADMIN`, `MANAGER`, `TRAINER`, `MEMBER`) bằng `@PreAuthorize("hasRole('...')")`.
* **Câu hỏi vấn đáp**: *"Tại sao không lưu token vào database để kiểm tra mà phải dùng JWT kết hợp Redis Blacklist?"*
  * **Trả lời chuẩn**: JWT giữ cho hệ thống stateless ở 99% các request (không phải query DB kiểm tra session). Chỉ khi logout ta mới cần lưu vào Redis (in-memory key-value cực nhanh với O(1)) để invalidate token ngay lập tức, vừa đảm bảo hiệu năng cao vừa duy trì an ninh tuyệt đối.

---

#### 📘 Chuyên đề 2: FinTech, Xử lý Đơn hàng & Giao dịch Toàn vẹn (E-Commerce & Payment)
* **Người làm chính**: **Người B** | **Cả 4 người đều phải hiểu sâu**.
* **File mã nguồn trọng tâm**: `OrderService.java`, `PaymentService.java`, `ServiceActivationService.java`, `V2__payments.sql`.
* **Bản chất kỹ thuật**:
  * **Server-side Pricing**: Client chỉ gửi `planId`. Backend truy vấn giá niêm yết từ DB, tự tính tổng tiền, chiết khấu và thuế trong database transaction. Tuyệt đối không tin tưởng giá từ Frontend.
  * **Idempotent Webhook Callback**: Bảng `payments` có constraint `UNIQUE (transaction_ref)`. Khi cổng thanh toán gửi webhook trùng lặp, backend bắt được lỗi duplicate key hoặc kiểm tra trạng thái đã là `COMPLETED` thì trả ngay HTTP 200 mà không kích hoạt dịch vụ lần thứ hai.
  * **Service Activation Engine**: Tách rời khỏi `PaymentService`. Dựa trên `plan_type` (`MEMBERSHIP` thì tạo bản ghi `memberships` tính `end_date`; `PT_PACKAGE` thì tạo bản ghi `pt_packages` cấp `total_sessions`).
* **Câu hỏi vấn đáp**: *"Nếu webhook thanh toán gửi tới 2 lần cùng một phần nghìn giây thì hệ thống chống trừ tiền/kích hoạt trùng thế nào?"*
  * **Trả lời chuẩn**: Dựa vào 2 lớp phòng thủ: Lớp 1 là kiểm tra trạng thái nghiệp vụ trong code Java (`if (payment.getStatus() == SUCCESS) return;`), Lớp 2 là Database Unique Constraint trên cột `transaction_ref`. Giao dịch thứ hai cố gắng insert/update sẽ bị database chặn đứng với `DataIntegrityViolationException` và rollback ngay lập tức.

---

#### 📘 Chuyên đề 3: Kiểm soát Đồng thời & Chống Race Condition (Concurrency & Booking)
* **Người làm chính**: **Người C** | **Cả 4 người đều phải hiểu sâu**.
* **File mã nguồn trọng tâm**: `BookingService.java`, `V3__bookings.sql`, `BookingConcurrencyTest.java`.
* **Bản chất kỹ thuật**:
  * **Pessimistic Locking**: Khi khách hàng book slot, câu lệnh SQL thực thi là:
    ```sql
    SELECT * FROM class_sessions WHERE id = ? FOR UPDATE;
    ```
    Khóa này khóa dòng của session lại cho đến khi transaction kết thúc, serialize toàn bộ các request đặt chỗ đồng thời, đảm bảo điều kiện `current_bookings < max_capacity` luôn chính xác 100%.
  * **Database Unique Constraint**: `UNIQUE (customer_id, session_id)` trên bảng `bookings` ngăn chặn một khách hàng bấm đúp hoặc mở 2 tab đặt trùng một lớp học.
  * **Concurrency Testing**: Dùng `CompletableFuture.allOf()` mô phỏng 10 thread cùng đặt 1 slot cuối cùng, assert rằng chỉ đúng 1 thread thành công và 9 thread còn lại nhận lỗi `CLASS_FULL`.
* **Câu hỏi vấn đáp**: *"Tại sao không dùng Optimistic Locking (`@Version`) cho đặt chỗ lớp học mà lại dùng Pessimistic Locking (`SELECT FOR UPDATE`)?"*
  * **Trả lời chuẩn**: Vì đặt chỗ lớp học là bài toán có tỷ lệ tranh chấp cực cao (High Contention) khi lớp sắp hết chỗ. Nếu dùng Optimistic Locking, 9 trên 10 người sẽ bị văng ra exception và phải retry liên tục gây lãng phí tài nguyên CPU. Pessimistic Lock xếp hàng các giao dịch một cách tuần tự và từ chối dứt khoát ngay khi đủ chỗ.

---

#### 📘 Chuyên đề 4: Quản lý Huấn luyện viên & Khóa Lạc quan (PT Sessions & Optimistic Locking)
* **Người làm chính**: **Người D** | **Cả 4 người đều phải hiểu sâu**.
* **File mã nguồn trọng tâm**: `PTPackageService.java`, `PTSessionService.java`, `V3__pt_sessions.sql`.
* **Bản chất kỹ thuật**:
  * **Optimistic Locking (`@Version`)**: Entity `PTPackage` dùng khóa lạc quan vì gói PT của 1 khách hàng chỉ có khách hàng đó và PT sử dụng (Low Contention). Khi trừ buổi tập, Hibernate kiểm tra cột version:
    ```sql
    UPDATE pt_packages SET remaining_sessions = remaining_sessions - 1, version = version + 1 WHERE id = ? AND version = ?;
    ```
  * **Idempotent Session Completion**: Bảng `pt_sessions` lưu trường `completion_ref UNIQUE`. Khi HLV hoàn thành buổi tập, một mã UUID được sinh ra. Nếu bấm hoàn thành 2 lần, database chặn trùng lặp, đảm bảo không bị trừ âm buổi tập.
* **Câu hỏi vấn đáp**: *"Sự khác biệt căn bản giữa cơ chế khóa ở Module Booking của C và Module PT của D là gì?"*
  * **Trả lời chuẩn**: Module Booking của C dùng **Pessimistic Lock** vì có hàng trăm người cùng tranh chấp một tài nguyên chung (lớp học). Module PT của D dùng **Optimistic Lock** vì gói tập là tài nguyên cá nhân, xác suất đụng độ đồng thời thấp, dùng khóa lạc quan cho hiệu năng đọc/ghi tối ưu hơn.

---

#### 📘 Chuyên đề 5: Kiến trúc Microservices & AI Client Gateway
* **Người làm chính**: **Cả 4 người đều tự viết WebClient của mình** (A: 8001, B: 8002, C: 8003, D: 8004).
* **File mã nguồn trọng tâm**: `LeadAIClient.java`, `CommerceAIClient.java`, `OperationsAIClient.java`, `WorkoutAIClient.java`, `application.yml`.
* **Bản chất kỹ thuật**:
  * Spring Boot đóng vai trò **API Gateway / Backend-For-Frontend (BFF)**. Frontend chỉ giao tiếp với Spring Boot qua cổng 8080.
  * Spring Boot sử dụng Spring 6 `RestClient` / `WebClient` có cấu hình:
    - Connection Timeout: 3 giây.
    - Read Timeout: 15 giây (cho các tác vụ LLM).
    - Header bảo mật nội bộ: `Authorization: Bearer fitos-ai-token`.
  * Fallback cơ chế: Nếu service AI Python bị sự cố, Spring Boot fallback về thuật toán mặc định an toàn (rule-based score hoặc thông báo thân thiện) mà không làm sập ứng dụng web.
* **Câu hỏi vấn đáp**: *"Tại sao không cho Frontend gọi trực tiếp 4 microservice AI Python mà phải đi qua Spring Boot?"*
  * **Trả lời chuẩn**: (1) Bảo mật: Tránh lộ API Key OpenAI và token nội bộ ra ngoài trình duyệt; (2) Tập trung kiểm soát quyền: Spring Boot xác thực JWT trước khi cho phép gọi AI; (3) Toàn vẹn dữ liệu: Spring Boot tự động nạp dữ liệu từ PostgreSQL vào request gửi sang Python mà client không cần gửi lên.

---

#### 📘 Chuyên đề 6: Generative AI, Context Injection & Prompt Engineering
* **Người làm chính**: **Cả 4 người đều viết microservice Python** | **Cả 4 đều hiểu cấu trúc context**.
* **File mã nguồn trọng tâm**: `ai-crm/services/lead_context_loader.py`, `ai-commerce/services/spending_context_loader.py`, `ai-operations/services/schedule_context_loader.py`, `ai-workout/services/fitness_context_loader.py`.
* **Bản chất kỹ thuật**:
  * **Context Injection (Nạp ngữ cảnh dữ liệu)**: Thay vì prompt chay, module context loader truy vấn dữ liệu thực tế từ Database (thông số thể lực, lịch sử đi tập, lịch sử cuộc gọi) đưa vào System Prompt dưới dạng JSON/Markdown.
  * **Few-Shot & Structured Output**: Ép LLM trả về đúng định dạng JSON Schema với Pydantic để backend parse an toàn không bao giờ bị lỗi format.
  * **Safety Guardrails**: Bộ lọc `safety_filter.py` đối soát danh sách `BLOCKED_TERMS` chặn đứng các tư vấn thuốc cấm hoặc rò rỉ dữ liệu nhạy cảm.
* **Câu hỏi vấn đáp**: *"Làm sao để đảm bảo AI không bị ảo giác (hallucination) và không trả lời linh tinh?"*
  * **Trả lời chuẩn**: Chúng em áp dụng 3 kỹ thuật: (1) System Prompt giới hạn phạm vi chặt chẽ ("Chỉ được trả lời dựa trên context được cung cấp"); (2) Context Injection nạp dữ liệu chính xác từ PostgreSQL; (3) Ép kiểu đầu ra bằng Pydantic Structured Output và chạy qua Safety Filter kiểm duyệt trước khi trả về.

---

#### 📘 Chuyên đề 7: DevOps, Containerization & CI/CD Pipelines
* **Người làm chính**: **A, B, C, D chia sẻ** (A: CI Backend, B: CI Frontend, C: Docker Compose Full Stack, D: CD Deploy GHCR).
* **File mã nguồn trọng tâm**: `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `.github/workflows/ci-backend.yml`, `.github/workflows/cd-deploy.yml`.
* **Bản chất kỹ thuật**:
  * **Multi-stage Build**:
    - Backend: Stage 1 dùng `maven:3.9-eclipse-temurin-21` biên dịch jar; Stage 2 dùng `eclipse-temurin:21-jre-alpine` chỉ chạy runtime, giảm kích thước image từ 800MB xuống còn 180MB.
    - Frontend: Stage builder build Next.js standalone output; Stage runner chạy `node server.js` siêu nhẹ.
  * **Docker Compose Networking**: Khởi chạy 8 service (`postgres`, `redis`, `backend`, `frontend`, `ai-crm`, `ai-commerce`, `ai-operations`, `ai-workout`) trên cùng bridge network với healthcheck trên PostgreSQL.
  * **GitHub Actions CI/CD**: Khi push code lên `main`, workflow chạy test tự động với service PostgreSQL container, build Docker image, push lên GitHub Container Registry (GHCR) và trigger deploy lên Cloud.
* **Câu hỏi vấn đáp**: *"Tại sao cần healthcheck trên PostgreSQL trong docker-compose.yml?"*
  * **Trả lời chuẩn**: Vì database khởi động cần vài giây để nạp dữ liệu và mở cổng. Nếu không có `depends_on: postgres: condition: service_healthy`, Spring Boot backend sẽ khởi động ngay lập tức và bị crash vì lỗi `Connection Refused`.

---

#### 📘 Chuyên đề 8: Chiến lược Kiểm thử Tự động Toàn diện (Testing Strategy)
* **Người làm chính**: **Cả 4 người đều viết Unit Test + E2E Test**.
* **File mã nguồn trọng tâm**: `backend/src/test/java/...`, `frontend/tests/home.spec.ts`, các file `test_*.py` trong microservices.
* **Bản chất kỹ thuật**:
  * **Unit Testing**: Dùng Mockito và JUnit 5 kiểm thử logic nghiệp vụ độc lập mà không cần bật cả Spring context (tốc độ chạy dưới 1 giây).
  * **Integration Testing**: Dùng `@SpringBootTest` và `@AutoConfigureMockMvc` kiểm thử toàn bộ luồng request HTTP xuyên qua Filter, Controller, Service, Repository.
  * **Concurrency Testing**: Dùng đa luồng kiểm tra tranh chấp dữ liệu (Race Conditions).
  * **E2E Playwright/MockMvc**: Kiểm thử hành trình người dùng hoàn chỉnh xuyên qua nhiều module.
* **Câu hỏi vấn đáp**: *"Khi nào nhóm dùng Unit Test với Mockito và khi nào dùng Integration Test với DB thật?"*
  * **Trả lời chuẩn**: Unit Test dùng để kiểm tra các nhánh logic rẽ nhánh (if/else), thuật toán tính tiền, thuật toán streak trong Service một cách nhanh chóng. Integration Test dùng để kiểm tra tính toàn vẹn của câu lệnh SQL, migration Flyway, transactional rollback và ràng buộc khóa ngoại trên cơ sở dữ liệu thật.

---

## BỘ CÂU HỎI VẤN ĐÁP BẢO VỆ ĐỒ ÁN (ĐÃ MỞ RỘNG KIỂM TRA CHÉO)

### 1. Câu hỏi vấn đáp kiểm tra chéo (Bất kỳ ai cũng có thể bị hỏi):

| Người bị hỏi | Câu hỏi kiểm tra chéo module của đồng đội | Hướng dẫn trả lời chuẩn |
|---|---|---|
| **Người A** | *"Bạn làm CRM, hãy giải thích cách bạn C xử lý tranh chấp đặt chỗ lớp học bằng `SELECT FOR UPDATE`?"* | *"Dạ, bạn C sử dụng Pessimistic Locking với `SELECT ... FOR UPDATE` trên bảng sessions để serialize các request cùng lúc. Chỉ khi kiểm tra còn chỗ thì mới tăng số lượng và lưu booking, kết hợp với constraint UNIQUE(customerId, sessionId) để chống overbooking tuyệt đối ạ."* |
| **Người B** | *"Bạn làm Thanh toán, hãy giải thích cơ chế xác thực JWT và Redis Blacklist của bạn A?"* | *"Dạ, bạn A dùng Access Token sống 15 phút để giảm thiểu rủi ro, và Refresh Token 7 ngày để cấp lại token mới. Khi người dùng logout, bạn A lưu token signature vào Redis Blacklist với TTL bằng thời hạn còn lại của token, JwtAuthFilter sẽ chặn ngay mọi request dùng token này ạ."* |
| **Người C** | *"Bạn làm Booking, hãy giải thích tại sao bạn B bắt buộc phải tính giá ở Server-side thay vì nhận tổng tiền từ Frontend?"* | *"Dạ, vì nếu nhận giá từ Client gửi lên thì kẻ xấu có thể dùng DevTools hoặc Postman để sửa tổng tiền thành 0 đồng. Bạn B bắt buộc chỉ nhận danh sách planId, sau đó Backend tự truy vấn giá niêm yết trong Database và tính toán toàn bộ trong transaction an toàn ạ."* |
| **Người D** | *"Bạn làm Workout, hãy giải thích cơ chế chuyển đổi Lead thành Customer của bạn C và bạn A?"* | *"Dạ, bạn A quản lý Lead và lưu lịch sử tương tác. Khi Lead đạt trạng thái tiềm năng, bạn C kích hoạt State Machine Convert Lead: kiểm tra điều kiện, tạo bản ghi Customer cho bạn B quản lý, sinh tài khoản User cho bạn A, và đánh dấu is_converted=true nhưng vẫn giữ nguyên toàn bộ lịch sử chăm sóc cũ ạ."* |

---

## BẢNG SO SÁNH TIẾN HÓA: V7 vs V8 (PHƯƠNG ÁN A — TÁI CÂN BẰNG TOÀN DIỆN)

| Tiêu chí so sánh | Phiên bản V7 (Cũ) | Bản V8 (Phương án A — Tái cân bằng toàn diện) | Đánh giá nâng cấp |
|-------------------|-------------------|----------------------------------------------|-------------------|
| **Tổng số giờ làm việc** | A=35h, B=34h, C=36.5h, D=41h | **A=37h, B=37h, C=37h, D=37h** | ✅ **Chênh lệch 0h (Bằng nhau 100%)** |
| **Phân bổ Backend (BE)** | D=15h (lệch nhiều), A=11.5h | **A=12h, B=12h, C=12h, D=12h** | ✅ **Cả 4 đều tự viết Spring Boot AI Client** |
| **Phân bổ AI Microservice**| D độc quyền LLM chat & context loader | **A=5h, B=5h, C=5h, D=5h** | ✅ **Cả 4 đều làm Hybrid AI + Context Loader từ DB** |
| **Phân bổ Frontend (FE)** | D ôm 4 Dashboard trực quan | **A=12h, B=12h, C=12h, D=12h** | ✅ **A có Manager Dashboard, B có Sales & 360, C có 7-col Calendar, D có Trainer & Workout** |
| **Phân bổ DevOps (OPS)** | Chưa đều | **A=3h, B=3h, C=3h, D=3h** | ✅ **Ai cũng có Docker + CI/CD** |
| **Phân bổ Testing** | Thiếu cross-testing | **A=3h, B=3h, C=3h, D=3h** | ✅ **Đủ Unit Test, Concurrency, E2E** |
| **Cân bằng 🔴 Must vs 🟡 Nice** | C có quá nhiều việc phụ (5h 🟡) | **A=34.5/2.5h, B=34/3h, C=34/3h, D=34/3h** | ✅ **Khối lượng việc lõi đồng đều (lệch ≤ 0.5h)** |
| **"Độ ngầu" & Lợi ích học tập**| D trông vượt trội hơn hẳn | **Cả 4 người đều có Highlight công nghệ cao tương đương** | ✅ **Bình đẳng tri thức và giá trị CV** |
| **Sở hữu Kiến thức toàn dự án** | Ai biết phần người nấy | **Cơ chế All-Team Mastery + 8 Chuyên đề kiến trúc cốt lõi** | ✅ **Cả 4 người đều nắm trọn 100% dự án** |

---

*Tài liệu phân công FITOS Version 8.0 (Phương án A — Tái cân bằng toàn diện) — Tổng cộng 160 tasks | A = 37h | B = 37h | C = 37h | D = 37h | Bình đẳng công nghệ cao | Cả 4 nắm trọn 100% kiến thức toàn dự án.*
