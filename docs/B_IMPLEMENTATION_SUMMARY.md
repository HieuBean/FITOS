# BÁO CÁO HOÀN THÀNH TOÀN BỘ CÁC LAYER CHO NGƯỜI B (FITOS)
> **Thành viên:** B  
> **Chủ đề chính:** Commerce, Customer 360, AI Commerce Service & Testing  
> **Tổng thời gian dự án phân bổ:** ~34.5h across 6 layers (Đã hoàn thiện đầy đủ cả 🔴 Must-have và 🟡 Nice-to-have)

---

## 1. TỔNG HỢP THEO 6 LAYER ĐÃ HOÀN THÀNH

### ✅ LAYER 1 — DATABASE (Đã làm trước đó & bổ sung token hash)
- `V2_1__plans.sql`: Bảng `service_plans` với enum CHECK (`MEMBERSHIP`, `PT_PACKAGE`), `price`, `features` JSONB, index active.
- `V2_2__orders.sql`: Bảng `orders` & `order_items`. Đảm bảo server-side pricing, CHECK `total_amount = subtotal - discount`, snapshot tên và giá tại thời điểm mua.
- `V2_3__payments.sql`: Bảng `payments` với UNIQUE(`transaction_ref`) và partial unique index 1 payment SUCCESS / order (chống double payment).
- `V2_4__memberships.sql`: Bảng `memberships` & `pt_packages` với `@Version` optimistic locking, ràng buộc dates và status.
- `V2_5__password_reset_tokens.sql`: Bảng `password_reset_tokens` lưu SHA-256 hash của reset token, bảo mật chuẩn OWASP.

---

### ✅ LAYER 2 — BACKEND (Java 21 / Spring Boot 3)
Tất cả các endpoint và business logic của Người B đã được triển khai đầy đủ:

1. **Service Plans CRUD (#34 — 🔴):**
   - DTOs: `PlanRequest`, `PlanStatusRequest`, `PlanResponse`.
   - `PlanService` & `PlanController` (`/api/plans`): CRUD đầy đủ, validation khớp các ràng buộc DB (PT_PACKAGE phải có sessions, MEMBERSHIP không có sessions). Public GET cho landing/pricing page.

2. **Server-Side Pricing & Orders (#35, #36, #37 — 🔴):**
   - `OrderPricing.java`: Pure business calculator tính tiền phía server, chống giá âm, chống discount > subtotal, giới hạn tối đa 1 membership + 1 PT pack trong 1 đơn hàng.
   - `OrderReferenceGenerator.java`: Sinh mã đơn nhân văn `ORD-YYYYMMDD-XXXXXX`.
   - `OrderService.java` & `OrderController.java` (`/api/orders`):
     - `POST /api/orders`: Tự tính tiền từ DB, nhân viên có thể áp discount, Member tự đặt theo tài khoản.
     - `GET /api/orders` (lọc status, date range, customer), `GET /api/orders/me`, `GET /api/orders/{id}`.
     - `PATCH /api/orders/{id}/cancel`: Chỉ cho phép huỷ đơn khi ở trạng thái `PENDING` và chưa có giao dịch SUCCESS.

3. **Customer 360 & Profiles (#30, #31, #32 — 🔴):**
   - `Customer.java` & `CustomerRepository.java`: Entity & repo hỗ trợ filter/search.
   - `CustomerActivityQueries.java`: Dùng JdbcTemplate an toàn kết nối sang bảng của các module khác (bookings, workouts, attendance) mà không làm sập ứng dụng nếu bảng chưa migrate.
   - `CustomerService.java` & `CustomerController.java` (`/api/customers`):
     - `GET /api/customers`: Phân trang, tìm kiếm họ tên/email/phone, lọc theo trạng thái thẻ (`ACTIVE`, `PAUSED`, `EXPIRED`, `NONE`).
     - `GET /api/customers/{id}`: Customer 360 tổng hợp từ 4 module (CRM, Commerce, Booking, Workout).
     - `PUT /api/customers/{id}`: Chỉnh sửa hồ sơ, mục tiêu tập luyện, ghi chú khách hàng.

4. **Auth Password Management (#20 — 🟡, #21 — 🔴):**
   - `PasswordService.java` & `PasswordController.java` (`/api/auth/`):
     - `POST /api/auth/forgot-password`: Rate-limited, sinh token ngẫu nhiên, lưu hash SHA-256 vào DB.
     - `POST /api/auth/reset-password`: Xác thực hash token còn hạn, cập nhật BCrypt password.
     - `PUT /api/auth/change-password`: Đổi mật khẩu cho user đang đăng nhập qua `CurrentUserService`.

5. **Admin User Management (#22 — 🟡):**
   - `User.java`, `Role.java`, `UserRepository.java`.
   - `UserService.java` & `UserController.java` (`/api/users`): `GET /api/users` (tìm kiếm, lọc role/status), `POST /api/users`, `PATCH /api/users/{id}/status`, `PATCH /api/users/{id}/role`.

6. **Trainer Schedule (#46 — 🔴):**
   - `TrainerScheduleService.java` & `TrainerScheduleController.java` (`/api/trainers/{id}/schedule`): Tổng hợp lịch dạy lớp nhóm và lịch dạy PT theo khoảng thời gian.

7. **AI Workout Chat Proxy (#64, #65 — 🔴):**
   - `AiConversation.java`, `AiMessage.java`, `AiConversationRepository.java`.
   - `AiChatProxyService.java` & `AiChatController.java` (`/api/ai/`):
     - `POST /api/ai/chat`: Lưu tin nhắn vào DB, proxy gọi sang service `ai-workout` (port 8004), lưu câu trả lời, có fallback thông minh nếu service AI chưa bật.
     - `GET /api/ai/conversations/me`: Tải lịch sử các cuộc trò chuyện của hội viên.

8. **Management Reports (#68 — 🟡):**
   - `ReportService.java` & `ReportController.java` (`/api/reports/`):
     - `GET /api/reports/sales`: Doanh thu, số lượng đơn, AOV, timeline doanh thu theo ngày, tỷ trọng từng gói.
     - `GET /api/reports/memberships`: Thống kê active, paused, expired, tỷ lệ retention rate.
     - `GET /api/reports/classes`: Thống kê buổi tập, tỷ lệ lấp đầy (occupancy rate).

---

### ✅ LAYER 3 — FRONTEND (Next.js 14 / React 18 / TailwindCSS)
- `src/lib/api.ts`: Axios client chuẩn hoá, tự động gắn JWT Bearer token và kết nối backend.
- `src/components/NoShowBadge.tsx` (#94 — 🟡): Component hiển thị badge nguy cơ vắng mặt AI (Low/Medium/High risk với % và pulse dot).
- `src/app/plans/page.tsx` (#73 — 🔴): Giao diện 3 gói dịch vụ động, kết nối `/api/plans` với fallback mượt mà.
- `src/app/dashboard/customers/page.tsx` (#78 — 🔴): Bảng danh sách hội viên, tìm kiếm debounce, tab lọc trạng thái thẻ (All, Active, Paused, Expired, No Membership), link trực tiếp vào hồ sơ 360.
- `src/app/dashboard/customers/[id]/page.tsx` (#79 — 🔴): Trang **Customer 360 gồm 6 Tab**:
  1. *Overview*: KPI thẻ, hồ sơ cá nhân, gói PT, mục tiêu tập luyện.
  2. *Membership*: Lịch sử thẻ, số ngày còn lại.
  3. *Bookings*: Lịch đặt lớp kèm `NoShowBadge`.
  4. *Workouts*: Buổi tập đã hoàn thành.
  5. *Payments*: Lịch sử hoá đơn, mã đơn hàng.
  6. *Activity Log*: Timeline check-in tại studio.
- `src/app/dashboard/sales/page.tsx` (#82 — 🔴): Trang bán hàng với 4 thẻ KPI (Doanh thu đã thu, Giao dịch thành công, Đơn chờ xử lý, Giá trị trung bình đơn) và bảng giao dịch kèm bộ lọc trạng thái.
- `src/app/dashboard/create-sale/page.tsx` (#83 — 🔴): **Wizard 3 bước hoàn chỉnh**:
  - Bước 1: Chọn khách hàng (có autocomplete) & Chọn gói dịch vụ.
  - Bước 2: Tuỳ chỉnh ngày bắt đầu, nhập giảm giá và ghi chú.
  - Bước 3: Chọn phương thức thanh toán (Thẻ, VNPay, Chuyển khoản, Tiền mặt) và xác nhận tạo đơn.
- `src/app/dashboard/payment-confirmed/page.tsx` (#84 — 🔴): Biên lai xác nhận thanh toán thành công, thông tin gói kích hoạt và đồng hồ đếm ngược (countdown) tự động chuyển về trang Sales.
- `src/app/dashboard/member/progress/page.tsx` (#98 — 🔴): Thống kê luyện tập, biểu đồ cột Pure CSS tần suất tập theo tuần, bảng kỷ lục cá nhân (PRs) và tóm tắt nhận xét từ AI Coach.

---

### ✅ LAYER 4 — AI SERVICE (`ai-commerce/` — Port 8002)
Toàn bộ service Python FastAPI độc lập của Người B:
- `requirements.txt`: FastAPI, Uvicorn, Pydantic, HTTPX, Pytest.
- `schemas.py`: Định nghĩa Pydantic models cho request/response.
- `main.py`:
  - `POST /predict-renewal` (#107 — 🔴): Rule-based tính xác suất gia hạn (0.05 – 0.98) dựa trên tần suất tập (30 ngày), số ngày vắng mặt, thâm niên và gói PT.
  - `POST /renewal-action` (#108 — 🔴): Gợi ý hành động giữ chân khách hàng (Offer giảm giá, HLV check-in 1-on-1, Mời tập lớp nhóm) qua kênh phù hợp.
  - `POST /suggest-upsell` (#109 — 🟡): Phân tích mức độ sử dụng để đề xuất nâng cấp gói (từ Move lên Perform, hoặc Perform lên PT Focus).
- `test_main.py` (#151 — 🔴): Bộ test Pytest kiểm tra toàn bộ logic, các khoảng xác suất, cận trên/dưới [0.05, 0.98], ưu đãi gia hạn và upsell sang PT Focus (**Đã chạy thực tế: 8/8 PASSED ✅**).

---

### ✅ LAYER 5 — OPS
- `frontend/Dockerfile` (#120 — 🔴): Multi-stage Dockerfile Next.js tối ưu với `standalone output` và non-root user `nextjs`.
- `ai-commerce/Dockerfile` (#122 — 🔴): `python:3.11-slim`, cấu hình chạy port 8002.
- `.github/workflows/ci-frontend.yml` (#130 — 🔴): GitHub Actions CI tự động kiểm tra `npm ci` và `npm run build`.
- `.github/workflows/ci-ai-commerce.yml` (#132 — 🔴): GitHub Actions CI tự động cài đặt dependencies và chạy `pytest -v`.
- `docker-compose.yml`: Đã bổ sung service `ai-commerce` chạy trên port 8002.
- `frontend/vercel.json` (#138 — 🔴): Cấu hình deployment Next.js lên Vercel.

---

### ✅ LAYER 6 — TESTING BE
Các unit test và integration test đã được viết hoàn chỉnh, biên dịch thành công 100% trên Java 21 và chạy vượt qua toàn bộ 21 tests:
- `OrderPricingTest.java` (#142 — 🔴): 10 unit tests tính tổng tiền server-side, kiểm tra chiết khấu hợp lệ, chặn chiết khấu âm, chặn chiết khấu vượt quá subtotal, chặn mua trùng loại gói, chặn rỗng, kiểm tra mặc định chiết khấu null, chặn số lượng != 1. (10/10 PASSED ✅)
- `PaymentTest.java` (#143 — 🔴): 3 unit tests trạng thái thanh toán, cập nhật `paid_at` khi SUCCESS, lưu lý do khi FAILED, và kiểm tra tính năng Idempotency (xác thực duplicate webhook callback không ghi đè và không kích hoạt lặp). (3/3 PASSED ✅)
- `MembershipLifecycleTest.java` (#144 — 🔴): 6 unit tests tính ngày kết thúc khi kích hoạt, kiểm tra logic tạm dừng và tiếp tục (bù đúng số ngày tạm dừng vào ngày kết thúc), chặn tạm dừng thẻ hết hạn, chặn tiếp tục trước ngày tạm dừng, tính số ngày còn lại. (6/6 PASSED ✅)
- `CommerceFlowIntegrationTest.java` (#156 — 🔴): 2 integration tests luồng thương mại đầy đủ (Order PENDING → Payment SUCCESS → Order PAID → Kích hoạt Membership ACTIVE, và luồng combo mua đồng thời Membership + PT Package, trừ buổi tập). (2/2 PASSED ✅)

---

## 2. CHUẨN BỊ TRẢ LỜI VẤN ĐÁP CHO NGƯỜI B

1. **Server-side pricing: Tại sao không nhận total từ client?**
   > *Trả lời:* Client là môi trường không đáng tin cậy (người dùng có thể sửa request qua DevTools/Postman). Nếu server nhận total trực tiếp, kẻ gian có thể đổi giá gói 5.000.000đ thành 1.000đ. Do đó, backend chỉ nhận `planId` và số lượng, sau đó tự truy vấn bảng `service_plans` để tính `subtotal` và kiểm tra quyền áp dụng mã giảm giá hợp lệ.

2. **Payment idempotency: UNIQUE constraint vs application check, cái nào làm gì?**
   > *Trả lời:* 
   > - *Application check:* Kiểm tra `findByTransactionRef` để trả ngay HTTP 200 SUCCESS nếu webhook của cổng thanh toán gửi lại (retry), tránh tốn tài nguyên chạy lại logic kích hoạt.
   > - *UNIQUE constraint ở Database:* Là chốt chặn cuối cùng ngăn chặn Race Condition (2 request callback tới cùng 1 mili-giây). Nếu cả 2 đều vượt qua application check cùng lúc, DB sẽ quăng lỗi duplicate key, đảm bảo thẻ hội viên không bao giờ bị kích hoạt 2 lần.

3. **ServiceActivation: Tại sao tách khỏi PaymentService?**
   > *Trả lời:* Áp dụng nguyên lý Single Responsibility Principle (SRP). `PaymentService` chỉ lo việc thanh toán (xác thực số tiền, mã giao dịch, chữ ký). Việc thanh toán thành công có thể kích hoạt thẻ `MEMBERSHIP`, gói `PT_PACKAGE`, hoặc bán phụ kiện thể thao. Tách `ServiceActivationService` giúp dễ mở rộng sản phẩm mới mà không làm sửa đổi luồng thanh toán cốt lõi.

4. **Membership pause/resume: end_date tính thế nào khi resume?**
   > *Trả lời:* Khi hội viên bấm `PAUSE`, hệ thống ghi nhận `paused_at = today`. Khi `RESUME`, hệ thống tính số ngày đã tạm dừng: `days = ChronoUnit.DAYS.between(paused_at, today)`. Ngày kết thúc mới sẽ được cộng bù: `end_date = end_date.plusDays(days)`, đồng thời cộng dồn vào `total_paused_days` và xoá cờ `paused_at`.

5. **renewal_probability: Input nào ảnh hưởng nhất, range output là gì?**
   > *Trả lời:* Input ảnh hưởng mạnh nhất là **tần suất tập luyện trong 30 ngày gần nhất** (`attendance_last_30d`) và **độ trễ kể từ lần tập cuối** (`days_since_last_visit`). Khách hàng nghỉ quá 14 ngày hoặc không đi tập trong tháng sẽ bị trừ điểm rất nặng. Range output được chuẩn hoá từ `0.05` (5%) đến `0.98` (98%).

6. **Multi-stage Dockerfile Next.js: Stage builder vs runner làm gì khác nhau?**
   > *Trả lời:* 
   > - *Stage builder:* Cần đầy đủ toàn bộ mã nguồn, TypeScript compiler, devDependencies (`@types/*`, `tailwindcss`, v.v.) để build ra bundle tối ưu (`.next/standalone`).
   > - *Stage runner:* Chỉ cần Node.js runtime tối thiểu, copy duy nhất thư mục `.next/standalone` và `.next/static`. Không chứa source code hay devDependencies nặng, giúp giảm dung lượng image từ ~1GB xuống dưới 100MB và tăng cường bảo mật (không lộ source code trong container).
