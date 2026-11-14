# BÁO CÁO RÀ SOÁT TOÀN DIỆN — DỰ ÁN "GYM CỎ" (FITOS)

**Ngày rà soát:** 12/11/2026
**Phạm vi:** toàn bộ repo `D:\gym cỏ` — frontend (Next.js 14), backend (Spring Boot 3.2.5), 4 service AI (FastAPI), migrations, tests, CI/CD, Docker.
**Phương pháp:** đọc mã tĩnh toàn bộ file nguồn + đối chiếu chéo FE ↔ BE ↔ AI ↔ SQL. **Không sửa file nào.**

> **Giới hạn:** shell bị chặn bởi quyền file Windows nên **không chạy được** `mvn test`, `next build`, Playwright. Các kết luận về "không boot được" / "build fail" là suy luận từ mã nguồn (đã ghi rõ căn cứ); cần xác nhận lại bằng lệnh thật khi có quyền.
> Workspace **không có thư mục `.git`** nên không đánh giá được lịch sử commit / ai sửa gì.

---

## 0. TÓM TẮT ĐIỀU HÀNH

| Mức | Số lượng | Ý nghĩa |
|---|---|---|
| **P0 — Chặn chạy** | 4 | Hệ thống **không khởi động được** hoặc lỗ hổng cho phép chiếm quyền |
| **P1 — Nghiêm trọng** | 19 | Mất dữ liệu âm thầm, báo "thành công giả", sai tiền, sai quyền |
| **P2 — Trung bình** | 26 | Chức năng sai/thiếu, UX kém, a11y, hiệu năng |
| **P3 — Thấp / vệ sinh** | 14 | Code chết, copy lỗi, cấu hình lệch |

**3 kết luận lớn nhất:**

1. **Backend hiện tại KHÔNG khởi động được** — có 2 lỗi độc lập cùng chặn: (a) mapping trùng `GET /api/progress/me`, (b) toàn bộ entity của module workout được map vào cột **không tồn tại trong Flyway**. CI không phát hiện vì **không có test nào thật sự boot Spring context**.
2. **"Thành công giả" là bệnh hệ thống, không phải lỗi lẻ.** Có ít nhất **11 chỗ** `catch` rồi trả mock/`true`/redirect sang màn hình thành công, khiến người dùng tin là đã lưu/đã thu tiền/đã kích hoạt gói trong khi DB trống.
3. **Phân quyền vừa thiếu vừa sai.** Toàn bộ module workout **không có `@PreAuthorize`** (hội viên sửa được giáo án của người khác), trong khi role `'SALES'` trong `Roles.java` **không tồn tại trong DB** khiến lễ tân bị 403 ở đúng những trang nghiệp vụ chính của họ.

---

## 1. P0 — CHẶN KHỞI ĐỘNG / CHẶN BẢO MẬT

### P0-1. Entity workout map sai cột — Hibernate `validate` sẽ ném lỗi khi boot
`backend/src/main/resources/application.yml:15` đặt `ddl-auto: validate`. Flyway tạo schema, Hibernate đối chiếu entity với schema thật:

| Entity | Cột entity cần | Flyway thực tế tạo | Kết quả |
|---|---|---|---|
| `WorkoutSession` (`model/WorkoutSession.java:25-49`) | `workout_plan_id`, `started_at`, `completed_at`, `status`, `total_duration_secs`, `calories_burned` | `plan_id`, `start_time`, `end_time`, `duration_minutes`, `rating` (`V4_3__workout_logs.sql:4-20`) | **6 cột thiếu** |
| `WorkoutPlan` (`model/WorkoutPlan.java:33-34`) | `name` (NOT NULL) | `title` (`V4_2__workout_plans.sql:8`) | **sai tên cột** |
| `WorkoutPlanExercise` (`model/WorkoutPlanExercise.java:34-51`) | `sets`, `reps`, `duration_secs`, `rest_secs`, `weight_suggest`, `sort_order` | `target_sets`, `target_reps_min/max`, `rest_seconds`, `target_weight_kg`, `order_index` (`V4_2:40-59`) | **6 cột thiếu** |
| `WorkoutLog` (`model/WorkoutLog.java:24-49`) | `workout_session_id`, `reps_actual`, `duration_secs`, `logged_at` | `session_id`, `reps_completed`, `created_at` (`V4_3:35-51`) | **4 cột thiếu** |

Không có migration nào `ALTER TABLE ... RENAME`/`ADD COLUMN` các cột này (đã grep toàn bộ `db/migration`; chỉ `V5_2` thêm cột cho bảng AI, `V4_6` thêm `studio_id`).

**Hệ quả:** `SchemaManagementException: missing column` → context không dựng được → **ứng dụng không chạy**. Module workout/giáo án/AI coach coi như không tồn tại.

**Vì sao không ai phát hiện:** `backend/src/test/resources/application-test.yml:8` đặt `ddl-auto: update` → Hibernate **tự tạo cột** trong test, che mất lỗi. CI backend (`.github/workflows/ci-backend.yml:54`) chạy với profile `test`.

**Điều đáng chú ý:** đây là **hai hệ schema song song**. Các câu SQL thô (`ProgressService`, `WorkoutAiController`, `MemberActivityController`) viết đúng theo schema Flyway (`end_time`, `session_id`, `title`, `target_sets`…), còn JPA entity viết theo một schema khác. Không thể cả hai cùng đúng.

**Sửa:** chọn 1 nguồn sự thật. Cách nhanh & ít rủi ro: viết `V5_3__workout_schema_alignment.sql` đổi tên/bổ sung cột cho khớp entity (hoặc ngược lại, sửa entity cho khớp SQL), rồi **bật `ddl-auto: validate` cho cả profile test** để lỗi lộ ra ngay.

---

### P0-2. Mapping trùng `GET /api/progress/me` → `Ambiguous mapping` khi boot
- `backend/src/main/java/com/fitos/controller/ProgressController.java:46`
- `backend/src/main/java/com/fitos/controller/WorkoutExecutionController.java:64` — `@GetMapping({"/api/workout-logs/progress/me", "/api/progress/me"})`

Hai handler cùng method + cùng path. Spring ném `IllegalStateException: Ambiguous mapping` lúc `RequestMappingHandlerMapping` khởi tạo.

Ngoài ra 2 handler trả **2 DTO khác nhau** (`ProgressResponse` dùng `currentStreakDays/workoutsThisMonth`; `ProgressSummaryResponse` dùng `streak/totalSessionsCompleted`) → FE đọc field nào cũng có trang bị `undefined`.

**Sửa:** bỏ `"/api/progress/me"` khỏi `WorkoutExecutionController`, giữ 1 endpoint duy nhất, và bật lại `FitosApplicationTests` với `@EnabledIfEnvironmentVariable` thay vì `@Disabled`.

---

### P0-3. Không có test nào boot Spring context → mọi lỗi P0-1/P0-2 lọt qua CI
`backend/src/test/java/com/fitos/FitosApplicationTests.java:8` — `@Disabled("Requires live PostgreSQL database container running")`.
Ba test `@SpringBootTest` còn lại đều bị gate bằng biến môi trường không được set trong CI:
- `RedisTokenBlacklistIntegrationTest.java:16` — `FITOS_TEST_REDIS`
- `LeadConversionFlowIntegrationTest.java:42` — `FITOS_TEST_DATABASE_URL`
- `PostgresWorkflowIntegrationTest.java` — tương tự

CI backend vì vậy chỉ chạy unit test Mockito → **xanh dù app không boot được**. Đây là lý do gốc khiến P0-1 và P0-2 tồn tại mà không ai biết.

**Sửa:** thêm 1 test `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers PostgreSQL chạy mặc định trong CI (không gate env), dùng `ddl-auto: validate`. Đây là việc quan trọng nhất của cả dự án.

---

### P0-4. Webhook thanh toán không xác thực chữ ký — ai cũng đánh dấu "đã trả tiền" được
`backend/src/main/java/com/fitos/config/SecurityConfig.java:66`
```java
.requestMatchers(HttpMethod.POST, "/api/payments/callback").permitAll()
```
`PaymentService.callback()` (`service/PaymentService.java:93-133`) **không có bất kỳ kiểm tra HMAC/signature/IP allowlist** nào. Chỉ cần biết `transactionRef` (dạng tuần tự, `nextReference()`), kẻ tấn công `POST /api/payments/callback {"transactionRef":"...","status":"SUCCESS"}` → `settle()` → order PAID + `activationService.activate(order)` → **kích hoạt gói hội viên miễn phí**.

**Sửa:** bắt buộc verify HMAC-SHA256 với secret của cổng thanh toán trên raw body, so sánh `MessageDigest.isEqual`, thêm replay-protection (timestamp + nonce), và rate-limit endpoint này.

---

## 2. P1 — BẢO MẬT & TOÀN VẸN DỮ LIỆU

### P1-1. Toàn bộ module workout không có phân quyền (IDOR)
- `controller/WorkoutPlanController.java` — **không có `@PreAuthorize` nào** ở cả 8 endpoint (tạo/sửa/activate/complete/cancel giáo án).
- `controller/WorkoutExecutionController.java` — không có `@PreAuthorize` ở cả 7 endpoint.
- `SecurityConfig.java:67` chỉ `.anyRequest().authenticated()` → **bất kỳ MEMBER nào** cũng tạo/sửa/xoá giáo án của bất kỳ ai.

Thêm nữa, `WorkoutLogService.startSession()` (`service/WorkoutLogService.java:32-34`) lấy `customerId` **từ request body**, không đối chiếu với principal; `createLog()` (`:99-104`) không kiểm tra session có thuộc về người gọi → **ghi set tập vào buổi tập của hội viên khác**.

**Sửa:** `@PreAuthorize` cấp class cho `WorkoutPlanController` (`ADMIN/MANAGER/TRAINER`), lấy `customerId` từ principal cho các endpoint của member, và thêm `session.getCustomer().getId().equals(principal.customerId)` check.

### P1-2. Role `'SALES'` không tồn tại → lễ tân bị 403 ở đúng nghiệp vụ chính
`common/security/Roles.java:15-18`
```java
STAFF             = "hasAnyRole('ADMIN','MANAGER','SALES')";
STAFF_OR_TRAINER  = "hasAnyRole('ADMIN','MANAGER','SALES','TRAINER')";
```
Nhưng hệ thống chỉ seed và cấp `ROLE_RECEPTIONIST` (`db/migration/V1_1__users.sql:35`, `dto/user/UserDtos.java:18`). Không tồn tại user nào có `ROLE_SALES` (grep toàn repo: chỉ 6 lần xuất hiện, đều là hằng chuỗi trong `Roles.java`/`CurrentUserService.java`).

**Ảnh hưởng trực tiếp:** `customer/CustomerController.java:27,36,42` → lễ tân **không xem/sửa được hồ sơ hội viên**; `trainer/TrainerScheduleController.java:20` → 403.

**Nguy hiểm kép:** FE lại mời lễ tân vào đúng trang đó (`components/Sidebar.tsx:26,37`) và trang `customers/page.tsx` **nuốt 403 rồi hiện 5 khách hàng demo hardcode** ("Sophie Turner"…) → lễ tân tưởng hệ thống chạy tốt và **gọi nhầm khách không tồn tại**.

**Sửa:** đổi `'SALES'` → `'RECEPTIONIST'` trong `Roles.java` + `CurrentUserService.java:65`. Thêm test `@WithMockUser(roles="RECEPTIONIST")` cho từng controller.

### P1-3. `frontend/src/lib/api.ts` dùng `fetch` trần — bỏ qua JWT, luôn 401
`api.ts:3-11` tạo axios instance có interceptor gắn token, nhưng **9 hàm** lại dùng `fetch()` không header: dòng 176, 199, 242, 253, 264, 286, 301, 321, 343.

Với `SecurityConfig.java:67` (`anyRequest().authenticated()`), các call này **luôn 401**. Kết hợp với `catch → return mock`, người dùng thấy:
- `workout-builder/page.tsx:95-96`: *"✅ Đã lưu giáo án thành công (Offline Draft)!"* — không có gì được lưu (và "Offline Draft" không hề tồn tại, không có localStorage).
- `workout-log/page.tsx:110-119`: *"Đã ghi nhận Set #…"* — set không vào DB.

**Sửa:** chuyển tất cả sang `api` (axios), xoá mọi `catch { return mock }`.

### P1-4. Gọi endpoint không tồn tại rồi hiển thị "thành tích" bịa
- `api.ts:264` → `GET /progress/summary` — **backend không có endpoint này** (chỉ `/api/progress/me`).
- `api.ts:286` → `GET /workout-sessions?customerId=&size=5` — backend chỉ có `/api/workout-sessions/me`.
- `dashboard/member/progress/page.tsx:27-35` fallback: `streak ?? 12`, PR `Bench 85kg / Squat 120kg / Deadlift 140kg`.
- `progress/page.tsx:7-14` biểu đồ 6 tháng là mảng tĩnh (T5→T10, trong khi hiện tại là T11).
- `progress/page.tsx:206` luôn hiện "+5% Tiến bộ", `:228` luôn "112% kỳ vọng".

→ Hội viên mới tinh thấy đầy đủ "kỷ lục cá nhân" không có thật. Đây là loại lỗi phá niềm tin nặng nhất với sản phẩm fitness.

### P1-5. Ba nguồn giá mâu thuẫn cho cùng một sản phẩm, hai thị trường trộn lẫn
| Nơi | Giá |
|---|---|
| `app/page.tsx:27,35,43` | `£29 / £49 / £89` (GBP) |
| `app/plans/page.tsx:20-51` | `290.000 / 490.000 / 1.200.000 ₫` |
| `lib/i18n.ts:64-79` | `đ890K / đ1.49M / đ3.2M` |

Trang chủ nói "City Centre Studio", coach "Sarah/Emma/Marcus" (`page.tsx:9-14`), `ForStudioTeams.tsx:19` ghi `£12k` — trong khi phần còn lại là tiếng Việt, "Quận 2". Khách Việt vào trang chủ thấy bảng giá bằng bảng Anh.

### P1-6. `payment-confirmed` xác nhận thanh toán giả + `create-sale` tự sinh mã đơn khi lỗi
- `dashboard/create-sale/page.tsx:163-172`: `catch` → tự sinh `ORD-${Date.now()}` → `router.push('/dashboard/payment-confirmed?...')`.
- `dashboard/payment-confirmed/page.tsx:11-16`: mặc định `'Jane Doe'`, `'ORD-20251025-9821'`, `490000`; `:80-83` status **hardcode "ACTIVE"**; không gọi API nào để verify.

→ Lễ tân thu tiền mặt, API sập, màn hình vẫn báo **"Payment Confirmed · Membership activated successfully"**. Không có order, không có membership, không đối soát được. Đây là lỗi nghiêm trọng nhất về nghiệp vụ.

**Sửa:** bỏ nhánh `catch` giả; `payment-confirmed` phải `GET /orders/{id}` và chỉ render khi `status === 'PAID'`; không nhận `amount`/`status` từ query string.

### P1-7. `N+1` + gọi AI đồng bộ trong transaction khi list lead
`service/LeadService.java:289` — `mapToResponse()` gọi `leadAIClient.scoreLead(...)` cho **từng lead**. Một trang 20 lead = 20 HTTP call đồng bộ, nằm trong `@Transactional` → giữ connection pool suốt thời gian gọi mạng. `client/LeadAIClient.java:21-23` lại **không set timeout** (`RestClient` mặc định vô hạn).

Cộng thêm: `LeadAIClient.java:29` gọi `"/crm/score"` nhưng `ai-crm/main.py:56` expose `"/score-lead"`, và token dùng `AI_SERVICE_TOKEN` trong khi ai-crm so `AI_CRM_API_KEY` → **404 rồi 401, luôn rơi vào fallback heuristic**. Nghĩa là "AI lead scoring" trong sản phẩm hiện chỉ là 4 dòng `if/else` (`LeadAIClient.java:41-57`) và **không ai biết** vì lỗi bị nuốt.

### P1-8. ai-crm bỏ qua xác thực khi thiếu header
`ai-crm/main.py:33-36`
```python
def verify_token(authorization: str = Header(None)):
    if not authorization:
        return True          # ← bypass
```
Cộng CORS `allow_origins=["*"]` (`:27`) → 3 endpoint AI CRM mở công khai ra Internet. Test `ai-crm` còn **khẳng định hành vi sai này là đúng**.

### P1-9. Token lưu localStorage + cookie không HttpOnly; middleware chỉ kiểm tra cookie tồn tại
`context/AuthContext.tsx:73,94`:
```ts
document.cookie = `fitos_token=${accessToken}; path=/; max-age=604800; SameSite=Lax`;
```
- Không `HttpOnly` → XSS đọc được token.
- Không `Secure` → lộ qua HTTP.
- Cookie sống **7 ngày** trong khi access token chỉ **15 phút** (`security/JwtTokenProvider.java:34`).
- `middleware.ts:9-15` chỉ `if (!token) redirect` — **không verify chữ ký**, ai gõ tay `document.cookie='fitos_token=x'` là qua cổng.

**Sửa:** chuyển sang cookie `HttpOnly; Secure; SameSite=Strict` do backend set qua route handler; access token ngắn hạn + refresh trong cookie.

### P1-10. Sai tiền ở màn hình tạo đơn
`dashboard/create-sale/page.tsx:129-130`
```ts
const total = Math.max(0, subtotal - discountAmount);   // không có tax
```
Backend `service/OrderService.java:97` tính `subtotal - discount + tax`. Hiện `tax = 0` nên chưa lệch số, **nhưng** toàn bộ số tiền gửi lên `payment-confirmed` là số FE tự tính, không phải `order.totalAmount` server trả về → ngày nào bật thuế/VAT là lệch ngay.

Thêm: `startDate` (người dùng chọn ở `:41`) **không có trong payload** `POST /orders` (`:148-153`) → ngày kích hoạt bị bỏ qua; input discount chỉ clamp bằng `Math.max(0, …)`, không chặn `discount > subtotal`.

### P1-11. Trang Users: MANAGER cấp được ROLE_ADMIN; khoá tài khoản 1 click không xác nhận
- `dashboard/users/page.tsx:301-313` — dropdown role gồm cả `ROLE_ADMIN`.
- `service/UserAdminService.java:121-123` — chỉ chặn tự đổi role của chính mình, **không chặn** MANAGER cấp ADMIN.
- `:314-325` — nút "Khoá" gọi API ngay, không confirm.

→ Leo thang đặc quyền (manager tạo admin đồng phạm) và khoá nhầm tài khoản đang trực quầy.

### P1-12. `toISOString()` dùng giờ UTC cho ngày local → lệch 1 ngày ở Việt Nam (UTC+7), xuất hiện 2 nơi
- `app/classes/page.tsx:26-28` + `:64,70`
- `app/dashboard/schedule/page.tsx:25-27` + `:48,81,175-177`

`startOfWeek` set 00:00 **local**; `toISOString()` đổi sang UTC → thứ Hai 00:00 (VN) = Chủ nhật 17:00Z → ra **Chủ nhật**. Kết quả: query `from=` lùi 1 ngày và so khớp `sessionDate` sai → **toàn bộ lưới lịch lệch 1 ngày**, đúng ở nơi quan trọng nhất (lịch lớp, lịch tuần).

(Lưu ý: `member/book/page.tsx:18-21` đã làm **đúng** — trừ `getTimezoneOffset()`. Nên áp dụng pattern đó cho 2 file kia.)

### P1-13. Đặt được lớp đã huỷ / đã qua giờ
`dashboard/member/book/page.tsx:226-262` không đọc `session.status`, không so `startTime` với hiện tại. Backend cũng không chặn: `service/BookingService.java:28-43` chỉ check duplicate + capacity; `OpsService.java:137-138` trả cả session `CANCELLED`.

### P1-14. `Promise.all` làm mất toàn bộ dashboard khi 1 endpoint lỗi
`dashboard/member/page.tsx:44-65` — `Promise.all([/progress/me, /body-metrics/me, /member/activity])`. `/member/activity` yêu cầu `hasRole('MEMBER')` và cần customer profile; nếu 404 thì `setData(null)` → **xoá sạch cân nặng, PR, streak** dù 2 call kia thành công. Dùng `Promise.allSettled`.

### P1-15. `completePTSession` trả `true` khi lỗi → trừ buổi PT không xảy ra
`lib/api.ts:424-431`:
```ts
} catch (err) { console.warn('API completePTSession fallback:', err); return true; }
```
`trainer/page.tsx:58` tin kết quả → UI báo "đã trừ 1 buổi", DB vẫn `SCHEDULED` → hội viên tập thêm miễn phí, sai đối soát gói PT.

### P1-16. Đặt chỗ `upcoming` không lọc ngày → huỷ nhầm buổi đã tập
`member/book/page.tsx:114-116` chỉ lọc `status === CONFIRMED || PENDING`, không lọc `sessionDate >= today`, không loại `NO_SHOW`. Booking tháng trước vẫn nằm trong "Lượt đặt sắp tới", bộ đếm `:132` sai, nút Huỷ `:286` huỷ nhầm buổi đã dùng.

### P1-17. `Roles` + `@PreAuthorize` không đồng bộ với RoleGuard của FE, và `WorkoutAiController` chặn cả ADMIN
`controller/WorkoutAiController.java:32` — `@PreAuthorize("hasAuthority('ROLE_MEMBER')")` ở cấp class. Nhưng `member/ai-coach/page.tsx` được bọc `RoleGuard` cho `['MEMBER','ADMIN']` (và ADMIN luôn pass `RoleGuard.tsx:28`) → **ADMIN vào được trang nhưng mọi request 403**.

### P1-18. `RiskReason` nội bộ (chống churn) bị hiển thị cho hội viên
`service/MembershipService.java:215-221` sinh chuỗi *"nguy cơ rời bỏ cao"*, *"nên nhắc gia hạn"*; `member/payments/page.tsx:130-132` render thẳng `activeMembership.riskReason` cho hội viên đọc. Đây là nhãn nội bộ cho CRM, không phải thông điệp dành cho khách.

### P1-19. Không có idempotency khi tạo đơn/thanh toán; webhook thiếu replay-protection
`OrderController.java:31-36` không nhận `Idempotency-Key`. Retry/timeout hoặc 2 tab → 2 đơn trùng, 2 doanh thu. `payment_events` có `ON CONFLICT DO NOTHING` cho callback (tốt), nhưng `POST /api/orders` thì không.

---

## 3. P2 — CHỨC NĂNG SAI / THIẾU

### 3.1. Dữ liệu & logic nghiệp vụ

**P2-1. Leads: không phân trang, KPI tính trên 20 dòng đầu.**
`leads/page.tsx:56` không truyền `page/size` → backend `LeadController.java:45` mặc định `size=20`. Lead thứ 21 trở đi **không bao giờ hiển thị**. `mapLeadPage` (`lib/contracts.ts:41-44`) vứt bỏ `totalElements`. Cùng lỗi ở `sales/page.tsx:34-37` (KPI "Paid Revenue/AOV" reduce trên mảng đã cắt) và `customers/page.tsx:47`.

**P2-2. Lead "Mất dấu" luôn thất bại.**
`leads/[id]/page.tsx:100` gửi `{status:'LOST'}` nhưng backend `LeadService.java:138-139` **bắt buộc** `lostReason` → luôn 400. Lỗi hiển thị chung chung *"Không thể cập nhật trạng thái"* nên không ai biết tại sao. Backend đã có endpoint đúng (`PATCH /leads/{id}/lost`, `:107`) nhưng FE không dùng.

**P2-3. Không chặn transition trạng thái lead.**
`:222` cho chọn `CONVERTED`/`LOST` cả khi lead đã `LOST`/`CONVERTED` → 400. Nút "Chuyển đổi thành hội viên" luôn hiện kể cả đã convert.

**P2-4. Race condition khi đổi filter/tuần nhanh.**
`leads/page.tsx:67-69`, `customers/page.tsx:132-134`, `schedule/page.tsx:43-58` đều không có `AbortController` hay sequence guard → response cũ về sau ghi đè response mới. Bấm ‹ › nhanh trên lịch tuần sẽ thấy lịch nhảy về tuần cũ.

**P2-5. N+1: mỗi dòng booking gọi 1 `POST /ai/noshow-risk`.**
`bookings/page.tsx:213-219` render `NoShowBadge` cho từng booking; `NoShowBadge.tsx:73` gọi API mỗi mount. Với `size: 100` → **100 request** mỗi lần load.

**P2-6. `NoShowBadge` gọi endpoint mà hội viên không có quyền.**
`AiOpsController.java:33` chỉ cho `ADMIN/MANAGER/RECEPTIONIST/TRAINER`, nhưng `member/book/page.tsx:285,307` render badge cho MEMBER → **403 mỗi lần render**, badge kẹt ở "đang tính…".

**P2-7. Reports 100% hardcode, bộ lọc ngày vô tác dụng.**
`dashboard/reports/page.tsx:8-28` — `salesData`, `classesData`, `memberStats` là hằng số; `:138` "1,053,000,000 đ", `:167` "Thứ Ba, 15 Tháng 7, 2026". `dateRange` (`:32`) chỉ dùng để đặt tên file CSV (`:36`). Backend đã có `report/ReportController.java` (`/api/reports/sales|memberships|classes`) nhưng **FE không gọi**. Thêm nữa Sidebar cho TRAINER vào `/dashboard/reports` (`Sidebar.tsx:50`) trong khi backend chỉ cho `MANAGEMENT` → 403.

**P2-8. Trainer console bịa dữ liệu + ghi chú chỉ nằm trong RAM.**
`trainer/page.tsx:17-34` — 8 học viên + 5 ca hardcode, ngày "Thứ Ba, 15 Tháng 7, 2026". `:73-82` `handleSaveNote` chỉ `setClients(...)` → **F5 là mất**.

**P2-9. Workout log: session/timer/set chỉ là state cục bộ.**
`workout-log/page.tsx:22-34` — điều hướng/F5 mất trắng. `:72` `startWorkoutSession(1, 1)` hardcode `customerId=1`, `planId=1`. `:73` `setSessionId(session.id || 101)`, `:92` `sessionId || 101` → ghi vào **session ma 101**. `:321` nút "Ghi nhận Set" **không cần** session đang chạy.

**P2-10. Nhập số trong workout log bị kẹt.**
`:282,292,303,314` dùng `parseInt(x) || 1`, `parseFloat(x) || 0` — không xoá hết ký tự để nhập số mới được (về 0 rồi thành mặc định). Không chặn `RPE > 10`, tạ âm, reps thập phân. Không dedupe `setNumber` → sửa tay sinh 2 dòng "Set #3".

**P2-11. Toggle gói dịch vụ 1-click không xác nhận.**
`dashboard/plans/page.tsx:46-57` gọi `PATCH /plans/{id}/toggle` ngay khi bấm → misclick là **ngừng bán gói đang có người mua**.

**P2-12. Không có luồng gia hạn/mua gói cho hội viên.**
`membership/payments` chỉ đọc. `MembershipDto.autoRenew` có trong type + DB nhưng **không có UI toggle và không có endpoint**. Hội viên muốn gia hạn chỉ còn cách ra quầy.

**P2-13. `RenewalRiskWidget` gia hạn 1 click không confirm.**
`components/RenewalRiskWidget.tsx:56-67` — hành động tài chính, không confirm/undo.

### 3.2. Auth & phiên

**P2-14. 401 mà không có refresh token → không clear, không redirect.**
`lib/api.ts:32-55` — toàn bộ nhánh cleanup/redirect nằm **trong** `if (refreshToken)`. Mất refresh token (tab khác logout, user xoá storage) → chỉ `Promise.reject` → app kẹt ở trạng thái "đã đăng nhập" với dữ liệu rỗng.

**P2-15. Open redirect sau đăng nhập.**
`login/page.tsx:69` (và `:47,53,64`) `router.push(redirectUrl || '/dashboard')` — không whitelist. `/login?redirect=https://fitos-login.evil` đưa nhân viên sang trang phishing ngay sau khi đăng nhập thật.

**P2-16. "Quên mật khẩu?" là link chết.**
`login/page.tsx:166` `<a href="#">`. **Backend đã có đủ** `POST /api/auth/forgot-password` + `/reset-password` (`auth/password/PasswordController.java:20,29`) nhưng **không có trang FE nào** cho `/forgot-password` hay `/reset-password`. Toàn bộ chức năng reset mật khẩu coi như vô hình.

**P2-17. `useSearchParams` không bọc Suspense ở 2 trang.**
`create-sale/page.tsx:5,27-28` và `payment-confirmed/page.tsx:9-16`. `login/page.tsx:258-266` đã bọc đúng → cho thấy team biết vấn đề nhưng bỏ sót. Next 14 sẽ lỗi prerender khi `next build` (cần xác nhận bằng lệnh thật).

**P2-18. `RoleGuard` mặc định `MEMBER` khi chưa có user → nháy sai nội dung.**
`RoleGuard.tsx:24` — `user?.role || ''`; `Sidebar.tsx:80` — `user?.role || 'MEMBER'`. Trong lúc hydrate, sidebar hiện nhóm HỘI VIÊN rồi mới đổi.

**P2-19. Trang `/dashboard/member` không có `RoleGuard`.**
Chỉ `ProtectedRoute` chung → STAFF gõ URL vẫn vào được trang hội viên (dữ liệu sẽ 404 nhưng UI vẫn render).

### 3.3. Hiệu năng & lỗi thời gian

**P2-20. `axios` timeout 10s < backend AI 25s.**
`lib/api.ts:10` `timeout: 10000` vs `WorkoutAiController.java:140` `readTimeout(25000)` vs Gemini 20s (`ai-workout/main.py:105`). Chat AI dài > 10s → FE báo lỗi trong khi backend vẫn đang xử lý; user retry → gọi trùng, tốn quota.

**P2-21. `LeadAIClient` không có timeout.**
`LeadAIClient.java:21-23` — `RestClient` mặc định **không timeout**; treo vô hạn nếu ai-crm không phản hồi (kết hợp P1-7 = treo cả trang leads).

**P2-22. Không có circuit breaker/retry cho 4 AI client.**
`AiWorkoutClient.java:42-45`, `AiOperationsClient.java:59-62`, `WorkoutAIClient.java:61-63` — nuốt toàn bộ exception, trả `null`/fallback bịa.

### 3.4. Hợp đồng FE ↔ BE ↔ AI sai

**P2-23. Accept/dismiss recommendation sai path + sai method.**
`api.ts:415` `POST /ai/recommend/${id}/accept` vs backend `PATCH /api/ai/recommendations/{id}/accept` (`AiRecommendationController.java:56,64`).

**P2-24. `requestAiRecommendation` sai cả request lẫn response schema.**
`api.ts:389-394` gửi `{customerId, goal, targetMuscleGroup, currentFitnessLevel}`; backend `WorkoutAiController.java:160-162` nhận `current_plan_name`, `target_muscle_group`, `feedback_or_pain_note` (`@JsonProperty` snake_case) → Jackson bỏ qua hết. Response kỳ vọng `{recommendationId, title, rawJson, status}` nhưng ai-workout trả `{analysis, suggested_changes, recovery_advice, confidence_score}`.

**P2-25. `saveWorkoutLog` sai shape.**
`api.ts:363-366` gửi `{customerId, workoutPlanId, sessionDate, durationMinutes, items[]}` trong khi `CreateLogRequest` cần `workoutSessionId, exerciseId, setNumber…` → luôn 400.

**P2-26. `dashboard/member/page.tsx:54-59` đọc `currentStreakDays`** trong khi vài chỗ khác khai báo `streak` (`api.ts:111-122`) → một trong hai luôn `undefined`.

---

## 4. P3 — GIAO DIỆN, A11Y, UX

### 4.1. Nghiêm trọng nhất trong nhóm UI

**P3-1. Không có navigation mobile.** `Header.tsx:15` `<nav className="hidden md:flex">` và **không có hamburger**. Dưới 768px, nav biến mất; footer (`Footer.tsx:17-21`) không bù link. Grep toàn `src`: link tới `/classes` **chỉ tồn tại ở Header:16** → trên mobile, trang lịch lớp là **trang mồ côi**, không thể vào.

**P3-2. Sidebar dashboard không responsive.** `Sidebar.tsx:121` `w-[232px] fixed` + `dashboard/layout.tsx:9` `ml-[232px]`. Trên điện thoại nội dung bị bóp còn ~140px. Không có collapse/drawer.

**P3-3. `<html lang="en">` trên sản phẩm tiếng Việt.** `app/layout.tsx:11`. Screen reader dùng giọng tiếng Anh đọc "Chào mừng trở lại"; Google hiểu sai ngôn ngữ.

**P3-4. Không có favicon, OG image, robots, sitemap.** `frontend/public` chỉ có 3 file `.jpg`. `/favicon.ico` → 404. Chia sẻ link qua Zalo/Facebook không có preview.

**P3-5. Label không gắn input.** `login/page.tsx:147-158,163-178,138-143` và `register/page.tsx:148-202` — `<label>` không `htmlFor`, `<input>` không `id`. Click label không focus được; screen reader chỉ đọc placeholder (mất khi gõ).

**P3-6. Không có error/404/loading boundary.** `frontend/src/app` không có `not-found.tsx`, `error.tsx`, `global-error.tsx`, `loading.tsx` → URL sai hiện trang 404 mặc định tiếng Anh, không logo, không nav.

**P3-7. Hàng tab ngày `/classes` tràn ngang trên mobile.** `classes/page.tsx:96-111` — 7 nút `px-4 py-1.5` ≈ 454px trong container 295px, không wrap, không `overflow-x-auto` → cả trang scroll ngang.

**P3-8. Touch target 24–28px.** `classes/page.tsx:102` (28px), `:116` (24px), `Footer.tsx:19` (16px) — dưới ngưỡng 44px khuyến nghị.

**P3-9. Modal không có dialog semantics ở 6 nơi.**
`ExerciseDetailDrawer.tsx:15-37`, `leads/[id]/page.tsx:463`, `users/page.tsx:335`, `plans/page.tsx:208`, `trainer/page.tsx:294`, `member/page.tsx:172`, `ai-coach/page.tsx:248` — thiếu `role="dialog"`, `aria-modal`, Esc, focus trap, trả focus, khoá scroll body. Backdrop là `<div onClick>` (không dùng được bằng bàn phím).

**P3-10. `#8A8E85` không đạt contrast AA.** Tính theo WCAG: 3.35:1 trên trắng, 2.8:1 trên `#ECEDE6` (trust strip `page.tsx:68-80`) — dưới 4.5:1, lại dùng ở cỡ **10–12px**. Đối tượng khách gym 40+ gần như không đọc được.

**P3-11. Lỗi form không thông báo cho screen reader.** `login/page.tsx:94-99`, `register/page.tsx:128-133` — không `role="alert"`/`aria-live` (trong khi `classes/page.tsx:145` có). Không `aria-invalid`, không `aria-describedby`, không `autoComplete` (`email`, `current-password`, `new-password`, `name`, `tel`) → password manager không autofill được.

**P3-12. Toggle "Cổng Hội Viên / Quản Lý Studio" là UI giả.** `login/page.tsx:102-132` — chỉ đổi màu, không đổi endpoint/payload. Có hành vi ma thuật `if (email === 'member@fitos.io') setEmail('')`.

### 4.2. Nhất quán thị giác & design system

**P3-13. Token bị bỏ qua hoàn toàn.** `tailwind.config.ts:7-27` khai báo `fitos-lime`, `fitos-dark`… nhưng grep `bg-fitos-|text-fitos-|border-fitos-` = **0 match**; `#D7FF3F` xuất hiện **207 lần** hardcode. Biến `--fitos-*` trong `globals.css:7-18` không nơi nào dùng `var(--fitos-…)`. 8 utility class (`.btn-lime`, `.btn-outline`, `.card-dark`, `.sidebar-link`, `.stat-card`…) **được định nghĩa nhưng không component nào dùng**.
→ Đổi brand color phải sửa 200+ chỗ; và đã có drift (nút cùng loại lúc `hover:bg-[#C4E82E]`, lúc `hover:opacity-90`).

**P3-14. Class Tailwind không tồn tại → spinner vô hình.** `login/page.tsx:261` `border-3` (Tailwind chỉ có 0/2/4/8) → spinner fallback là **div trắng 32px**. `animate-fadeIn` (`login:95`, `leads:229`, `leads/[id]:465`, `RoleGuard:19`) trong khi config chỉ định nghĩa `fade-in` → không animation.

**P3-15. Google Fonts nạp bằng `@import`.** `globals.css:1` — render-blocking, 2 round-trip tới `fonts.googleapis.com` trước khi có chữ. `next/font` sẽ self-host + preload.

**P3-16. Ảnh hero tải trên mobile dù bị ẩn.** `Hero.tsx:47-55` — `hidden lg:block` nhưng `priority` vẫn phát `<link rel=preload>`, thiếu `sizes`.

**P3-17. `i18n.ts` 372 dòng chết.** Không nơi nào import `useTranslations` (grep 0 match). Nội dung còn **mâu thuẫn với UI thật**: `hero_title: 'Run the studio.'` vs Hero "Train smarter."; `plan1_price: 'đ890K'` vs 290.000.

### 4.3. Copy & nội dung

**P3-18. Hai lời hứa khác nhau về dùng thử.** `page.tsx:54` "first class completely free" vs `Header.tsx:23` "Start free trial" vs `i18n.ts` "14-day trial". Tương tự "freeze up to 3 months" (`page.tsx:52`) vs "paused anytime" (`plans/page.tsx:62`).

**P3-19. Nửa Việt nửa Anh.** `plans/page.tsx:160` `/{p.durationDays} days` → "/30 days"; `:65` typo **"cancelation"** + sai hoà hợp số ít.

**P3-20. Trang chủ lẫn copy UK.** Xem P1-5.

**P3-21. Nút "Đặt chỗ" vẫn bấm được khi hết chỗ.** `classes/page.tsx:175-189` — "Hết chỗ" chỉ là text, CTA vẫn active y hệt lớp còn chỗ, không có waitlist dù `i18n.ts` có key `join_waitlist`.

**P3-22. `/login` và `/register` không có đường về trang chủ trên mobile.** Panel thương hiệu là `hidden lg:flex` (`login/page.tsx:213`, `register:78`) → mobile không có logo, không link, không Header.

---

## 5. P3 — DEVOPS, TEST, VỆ SINH REPO

**P3-23. Bí mật thật nằm trong repo.** `.env.example:33` `JWT_SECRET=404E63…5970` (64 hex, đủ mạnh — nhưng là **secret công khai**), `:38,41,44,47` các `*-secret-ai-token-2026`, `:50` `OPENAI_API_KEY=sk-proj-…`. Trùng với `application.yml:27` (`${JWT_SECRET:superjwtsecret_default_key_for_dev_mode}`), `:34,52` (`:fitos-ai-token`), và default `fitos-ai-token` trong **cả 4** service Python. Deploy quên set env = chạy bằng secret ai cũng đọc được trong repo.

**P3-24. CORS `*` + `allow_credentials=True` ở cả 3 service AI** (`ai-workout/main.py:30-36`, `ai-crm:25-31`, `ai-operations:45-51`). Tổ hợp này bị spec cấm và không có ý nghĩa bảo vệ.

**P3-25. API key Google nằm trong query string.** `ai-workout/main.py:98` `...?key={key}` → lọt vào access log/proxy.

**P3-26. `/health` lộ cấu hình.** `ai-workout/main.py:136-148` trả `llm_provider`, `llm_model`, `gemini_configured`, `openai_configured` công khai.

**P3-27. So sánh token không constant-time.** `ai-workout/main.py:42`, `ai-operations/main.py:58`, `ai/app/main.py:11` dùng `!=` thay `secrets.compare_digest`.

**P3-28. Không giới hạn độ dài prompt / rate limit.** `ai/app/main.py:14-15` `prompt: str` không `max_length`; không service nào có rate limit.

**P3-29. Service `ai/` là xác chết gây nhiễu.** `ai/app/main.py:19-20` còn `# Dummy response for now... AI integration pending`. Không có trong `docker-compose.yml`, không có workflow CI — nhưng `OPERATIONS.md:11` lại hướng dẫn deploy nó, **trùng port 8001 với ai-crm**.

**P3-30. `docker-compose` thiếu healthcheck `ai-commerce`** và `frontend depends_on: backend: condition: service_started` thay vì `service_healthy`.

**P3-31. Playwright & unit test: 3 spec đỏ 100%.**
- `tests/home.spec.ts` chờ `#lang-vi` và text "Smart Gym & Studio Management" — **không tồn tại trong DOM** (`Hero.tsx:15-18` là "Train smarter.").
- `tests/trainer_workout.spec.ts` 6/6 fail: không login → `ProtectedRoute` đẩy về `/login`, nên `h1` không có; thêm nữa copy AI Coach trong spec ("AI Workout Coach", "Đề Xuất Giáo Án Cá Nhân Hóa") khác DOM thật ("Huấn luyện viên Thể hình AI").
- `tests/api.unit.test.ts:7` import `getWorkoutSessions` — **không được export** (chỉ có `getRecentWorkoutSessions`) → ESM link error, cả file không chạy. Assert `:32-34` đòi "Bench Press"/"Pull Up" nhưng fallback là "Barbell Bench Press", không có Pull Up.
- `playwright.config.ts:4` `testDir: './tests'` → Playwright **collect cả** `api.unit.test.ts`/`ops.unit.test.ts` (dùng `node:test`, sai runner).
- `package.json:11` `node --experimental-strip-types` cần Node ≥ 22.6, CI dùng Node 20 (`.github/workflows/ci-frontend.yml:28`).

**P3-32. CI frontend chỉ `npm ci && npm run build`.** Không lint, không `tsc --noEmit`, không Playwright, không unit test (`ci-frontend.yml:32-39`). `package.json` cũng **không có eslint** trong devDeps dù script `lint` tồn tại.

**P3-33. Config drift.**
`.env.example:53` `NEXT_PUBLIC_API_URL` nhưng FE đọc `NEXT_PUBLIC_BACKEND_URL` (`next.config.mjs:9`). `.env.example:20` `DB_NAME=fitos_db` nhưng `application.yml:10` hardcode `.../fitos`. `docker-compose.yml:165` set `NEXT_PUBLIC_BACKEND_URL` lúc **runtime**, nhưng `next.config.mjs:8-10` inline biến lúc **build** → env runtime vô tác dụng.

**P3-34. `vercel.json` mâu thuẫn `output: 'standalone'`.** `frontend/vercel.json:6` `outputDirectory: ".next"` trong khi `next.config.mjs:4` standalone — tài liệu deploy gây nhầm.

**P3-35. Không có README gốc, không LICENSE, không `.git`.** Chỉ có `OPERATIONS.md` (67 dòng, và **sai endpoint** `/crm/score`), `docs/B_IMPLEMENTATION_SUMMARY.md`, `deploy/DEPLOY_RUNBOOK.md`.

**P3-36. Build artifact & venv nằm trong workspace.** `frontend/.next/**` (kể cả `standalone/`), `.venv/`, `ai-crm/.venv/Lib/site-packages/**` (~3.400 file), `backend/target/**`. Đều đã có trong `.gitignore` nhưng vẫn chiếm chỗ và gây nhiễu tìm kiếm.

**P3-37. Code chết trong backend.** `service/WorkoutAIService.java` + `client/WorkoutAIClient.java` + `dto/AIDto.java` — **không controller nào dùng** (grep: chỉ tự tham chiếu). Đây là bản trùng lặp thứ hai của tính năng AI chat, dùng config khác (`ai.workout.base-url` vs `fitos.ai.workout-url`).

**P3-38. Version Playwright lệch.** `package.json:20` `@playwright/test ^1.63.0` vs `:24` `playwright 1.44.1`.

**P3-39. `axios 1.7.2`, `next 14.2.4`, `react 18.3.1` đều đã cũ** so với thời điểm 11/2026 — nên rà CVE.

**P3-40. Fresh dev không thể đăng nhập.** `V4_9__disable_unconfigured_seed_admin.sql` tắt tài khoản `admin@fitos.com` trừ khi set `FITOS_BOOTSTRAP_ADMIN_PASSWORD` (≥ 12 ký tự, `config/AdminBootstrap.java:25`). Đây là thiết kế **đúng về bảo mật** nhưng không có tài liệu nào giải thích → dev mới sẽ tưởng app hỏng.

---

## 6. NHỮNG CHỖ ĐANG LÀM TỐT (giữ nguyên khi refactor)

Để cân bằng, đây là các điểm chất lượng thật sự:

- `BookingService` chống double-booking/overbooking bằng `PESSIMISTIC_WRITE`; `BookingController:45-55` lấy customer từ JWT chứ không từ body (đúng).
- `PaymentService.recordEvent()` dùng `INSERT ... ON CONFLICT (event_key) DO NOTHING` — idempotency webhook làm đúng chuẩn.
- `PasswordService` — token reset 32-byte `SecureRandom`, lưu **hash SHA-256** chứ không lưu raw, có invalidate token cũ, rate-limit 1 giờ.
- `AdminBootstrap` + `V4_8`/`V4_9` — xử lý rất đúng vấn đề "seed password công khai".
- `StudioContext` + `findBy...AndStudioId` — cách ly đa studio (multi-tenant) nhất quán ở **hầu hết** service (27 file dùng).
- `ai-workout/safety_filter.py` — bộ lọc chất cấm/chấn thương có chuẩn hoá dấu tiếng Việt, là ý tưởng tốt và hiếm.
- `ai-workout` lọc `suggested_changes` theo `current_exercises` (`main.py:338-342`) — chống AI "bịa bài tập" rất đúng hướng.
- `Dockerfile` của `ai-workout` — non-root, healthcheck, không `--reload`. `docker-compose.yml` có healthcheck cho 3/4 AI service.
- `GlobalExceptionHandler` — map lỗi nhất quán, không lộ stack trace ra client.
- `dashboard/page.tsx` — một trong số ít trang có error state trung thực (`:36-45`).

---

## 7. LỘ TRÌNH SỬA ĐỀ XUẤT

### Tuần 1 — "Làm cho nó chạy được" (P0)
1. Viết migration align schema workout (P0-1) hoặc sửa entity.
2. Xoá mapping trùng `/api/progress/me` (P0-2).
3. Bật 1 test boot context + Testcontainers, `ddl-auto: validate`, chạy trong CI (P0-3).
4. Verify HMAC cho webhook thanh toán (P0-4).
   → *Tiêu chí xong: `mvn test` xanh với context thật; `docker compose up` lên được; `POST /payments/callback` không chữ ký trả 401.*

### Tuần 2 — "Dừng nói dối người dùng" (P1 fake success)
5. Xoá **toàn bộ** `catch → mock/true/redirect-success`: `lib/api.ts` (9 hàm), `create-sale`, `payment-confirmed`, `customers`, `sales`, `trainer`, `workout-builder`, `completePTSession`, `plans`.
6. Thay bằng: error state + retry + thông báo lỗi thật từ `err.response.data.message`.
7. `payment-confirmed` chỉ render từ `GET /orders/{id}`.
8. Sửa contract: `/progress/summary` → `/progress/me`; accept/dismiss → `PATCH /api/ai/recommendations/{id}`; `saveWorkoutLog` shape.
   → *Tiêu chí xong: tắt backend, mọi trang hiện lỗi rõ ràng, không có con số/thành công nào là bịa.*

### Tuần 3 — Bảo mật & phân quyền (P1)
9. `Roles.java`: `SALES` → `RECEPTIONIST`; thêm test theo role.
10. `@PreAuthorize` cho `WorkoutPlanController` + `WorkoutExecutionController`; lấy `customerId` từ principal; check ownership session.
11. ai-crm: bắt buộc auth (`HTTPBearer(auto_error=True)`), `compare_digest`, sửa test.
12. `LeadAIClient`: path `/score-lead`, đúng token, set timeout, bỏ gọi AI trong `mapToResponse` (chuyển async/cache).
13. Chặn MANAGER cấp ROLE_ADMIN; confirm dialog cho khoá tài khoản.
14. Token → cookie `HttpOnly` + `Secure`; whitelist `redirect`.
   → *Tiêu chí xong: có bộ test ma trận role × endpoint; pentest 5 kịch bản IDOR đều 403.*

### Tuần 4 — Lịch & thời gian (lỗi lặp lại nhiều nơi)
15. Chuẩn hoá 1 helper `toLocalIso()` dùng chung; áp cho `classes`, `schedule`, `book`, `leads`.
16. Chặn đặt lớp `CANCELLED`/đã qua giờ (FE + BE).
17. Lọc `upcoming` theo `sessionDate >= today`.
18. Phân trang cho leads/sales/customers/bookings; KPI lấy từ endpoint thống kê.
   → *Tiêu chí xong: test chạy ở TZ `Asia/Ho_Chi_Minh` cho lịch tuần/ngày.*

### Tuần 5 — Giao diện & a11y
19. Mobile nav (Header hamburger + Sidebar drawer responsive).
20. `lang="vi"`, favicon, OG image, `not-found.tsx`, `error.tsx`.
21. `htmlFor`/`id` cho mọi label; `role="alert"` cho mọi lỗi form; `autoComplete`.
22. Modal → `<dialog>` hoặc `role="dialog"` + Esc + focus trap (6 nơi).
23. Thay `#8A8E85` bằng token đạt 4.5:1 cho chữ nhỏ.
24. Thống nhất bảng giá + xoá copy UK; xoá `i18n.ts` hoặc dùng thật.

### Tuần 6 — Hạ tầng & test
25. Tách `tests/unit` khỏi Playwright `testDir`; viết lại 3 spec đỏ; Node 22 + `engines`.
26. CI frontend: `tsc --noEmit` + lint + Playwright + unit.
27. Xoá `.next/`, `target/`, `.venv/` khỏi workspace; `.env.example` chỉ placeholder.
28. README gốc: cách chạy, cách set `FITOS_BOOTSTRAP_ADMIN_PASSWORD`, sơ đồ service/port.
29. Xoá code chết (`WorkoutAIService`, `WorkoutAIClient`, `AIDto`, `i18n.ts`, `ai/` hoặc gộp vào ai-crm).

---

## 8. PHỤ LỤC — CÁCH TỰ XÁC MINH NHANH

Khi có quyền shell, chạy theo thứ tự để thấy đúng các lỗi P0:

```powershell
# P0-1 + P0-2: context không dựng được
cd backend
mvn -q -DskipTests spring-boot:run   # kỳ vọng: SchemaManagementException / Ambiguous mapping

# P0-3: CI xanh giả
mvn -q test                          # kỳ vọng: BUILD SUCCESS dù app không boot

# P1-1/P1-2: ma trận quyền
#   login bằng RECEPTIONIST → GET /api/customers      → 403 (sai)
#   login bằng MEMBER       → POST /api/workout-plans → 201 (sai)

# P0-4: webhook không chữ ký
#   POST /api/payments/callback {"transactionRef":"<ref>","status":"SUCCESS"} → 200 (sai)

# P2-17: build FE
cd ../frontend
npx next build                       # kỳ vọng: useSearchParams suspense error

# P3-31: test đỏ
npx playwright test                  # kỳ vọng: home + trainer_workout fail
node --experimental-strip-types --test tests/api.unit.test.ts   # kỳ vọng: import error
```

---

*Báo cáo được tạo bằng rà soát tĩnh toàn bộ mã nguồn. Không có file nào trong dự án bị sửa đổi.*
