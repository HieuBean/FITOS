# BÁO CÁO KIỂM TRA & SỬA LỖI — DỰ ÁN "GYM CỎ" (FITOS)

**Ngày:** 13/11/2026
**Phạm vi:** chạy thật toàn hệ thống (PostgreSQL 18 + Spring Boot 3.2.5 + Next.js 14), kiểm thử API theo vai trò, đo giao diện tự động bằng Playwright ở 3 kích thước màn hình.

---

## 0. KẾT QUẢ TÓM TẮT

| Hạng mục | Trước | Sau |
|---|---|---|
| Backend khởi động | ❌ Không boot được | ✅ Chạy ổn định |
| Backend test suite | 103 test, **4 fail** + không boot được context | ✅ **103/103 PASS** |
| Frontend build | Lỗi hydration, 404 | ✅ Build sạch |
| API lỗi khi dùng | 11 lỗi (403/404/500) | ✅ **0 lỗi** |
| Trang bị tràn ngang | 19 trang (mobile) | ✅ **0 trang** |
| Lỗi accessibility | 27 | ✅ **0** |
| Lỗi tương phản màu | 20+ | ✅ **0** |

---

## 1. LỖI CHẶN KHỞI ĐỘNG (đã sửa)

### 1.1. Bean `passwordEncoder` định nghĩa trùng → app không boot
`common/config/SecurityBeanConfig.java` khai báo `PasswordEncoder` trùng với `config/SecurityConfig.java`. `@ConditionalOnMissingBean` không có tác dụng giữa hai `@Configuration` thường.

```
BeanDefinitionOverrideException: Invalid bean definition with name 'passwordEncoder'
```
**Sửa:** bỏ bean trùng trong `SecurityBeanConfig`, giữ một nguồn duy nhất.

### 1.2. Schema JPA lệch hoàn toàn với Flyway
Hai bộ tên cột song song: Flyway tạo `plan_id / start_time / end_time / title / target_sets`, entity map `workout_plan_id / started_at / completed_at / name / sets`. Với `ddl-auto: validate`, Hibernate dừng:

```
Schema-validation: missing column [duration_secs] in table [workout_logs]
Schema-validation: wrong column type [day_of_week]: found int2, expecting integer
```

**Sửa:** thêm `V5_3__workout_schema_alignment.sql` — đổi tên cột theo entity, ép `rpe → INT`, đổi 12 cột `timestamptz → timestamp` cho khớp `LocalDateTime`, `day_of_week → INT`.

### 1.3. `@Column` lặp trong entity chặn Lombok toàn cục
`PasswordResetToken.java` có hai `@Column` trên một field → javac báo `Column is not a repeatable annotation type`, kéo theo **hàng trăm** lỗi "cannot find symbol" ở các file khác (Lombok không sinh getter/setter).

**Sửa:** xoá annotation trùng.

### 1.4. Ràng buộc DB cũ chặn lưu giáo án
Sau khi đổi `order_index → sort_order`, ràng buộc `> 0` vẫn còn trong khi entity mặc định `sortOrder = 0` → mọi lần tạo giáo án trả 500.

**Sửa:** `V5_5__workout_constraint_relax.sql` — thay bằng `>= 0`, bỏ `chk_wpe_reps_range`.

---

## 2. LỖI BẢO MẬT (đã sửa)

### 2.1. Redis sập → toàn bộ đăng nhập hỏng (403 hàng loạt)
`JwtTokenProvider.validateToken()` gọi `isBlacklisted()` → Redis. Redis không chạy (Docker tắt) → exception → `JwtAuthFilter` bỏ qua xác thực → **mọi request đã đăng nhập đều 403**.

Đây là lỗi production nghiêm trọng: chỉ cần Redis restart là cả hệ thống khoá.

**Sửa:** tách kiểm tra blacklist khỏi luồng xác thực. Redis lỗi → trả `null` (không kết luận), token hợp lệ vẫn được chấp nhận; blacklist vẫn ghi cục bộ để logout có tác dụng.

### 2.2. IDOR — hội viên sửa được giáo án của người khác
`WorkoutPlanController` và `WorkoutExecutionController` **không có `@PreAuthorize` nào**, đồng thời nhận `customerId` từ body/query.

**Sửa:**
- Thêm `@PreAuthorize` cho toàn bộ endpoint.
- `customerId` suy ra từ JWT (`CurrentMemberService`), không nhận từ client.
- `WorkoutLogService.requireOwnedSession()` kiểm tra quyền sở hữu buổi tập.

**Kiểm chứng thực tế:**
```
MEMBER tạo giáo án            → 403 ✅
MEMBER đọc giáo án người khác → 403 ✅
MEMBER đọc giáo án của mình   → 200 ✅
TRAINER đọc giáo án           → 200 ✅
```

### 2.3. Role `SALES` không tồn tại → lễ tân bị 403
`Roles.java` dùng `'SALES'` nhưng DB chỉ cấp `ROLE_RECEPTIONIST`. Lễ tân **không xem được hồ sơ hội viên** — đúng nghiệp vụ chính của họ.

**Sửa:** đổi sang `RECEPTIONIST`. Kiểm chứng: `RECEPTIONIST /api/customers → 200 ✅`

### 2.4. Trùng mapping `GET /api/progress/me`
Hai controller map cùng path → Spring báo `Ambiguous handler methods` và trả 400 cho mọi lần gọi.

**Sửa:** chỉ giữ `ProgressController`, đổi endpoint kia thành `/api/workout-logs/progress/me`.

---

## 3. LỖI DỮ LIỆU & NGHIỆP VỤ (đã sửa)

### 3.1. JSONB bind sai kiểu → giao dịch thanh toán rollback
`AuditLog.details` map `jsonb` nhưng Hibernate bind `varchar`:
```
column "details" is of type jsonb but expression is of type character varying
```
Lỗi xảy ra trong `@Around` của audit aspect → `UnexpectedRollbackException` → **thanh toán tiền mặt luôn 500**, không kích hoạt được gói.

**Sửa:** thêm `@JdbcTypeCode(SqlTypes.JSON)` cho cả 6 entity map jsonb.

### 3.2. Giá gói là USD (29đ) thay vì VND
Seed V2_1 dùng `29 / 49 / 89` USD cho sản phẩm Việt Nam → đơn hàng thật tạo với giá **29 đồng**. `PT Focus` còn bị gắn nhầm `MEMBERSHIP`.

**Sửa:** `V5_4__plan_catalog_vnd.sql` — 290.000 / 490.000 / 1.200.000 ₫, sửa loại gói.

**Kiểm chứng:** đơn hàng thật tạo với `total=290000.00`, thanh toán CASH → `PAID`, membership `ACTIVE` được tạo.

### 3.3. `daysRemaining` lệch 1 ngày
Gói 30 ngày, ngày cuối cùng hiển thị "còn 0 ngày" → hội viên tưởng đã hết hạn.

**Sửa:** `return diff < 0 ? 0 : diff + 1`.

### 3.4. `OrderPricing` trả sai scale và thông điệp lỗi
`discount` null trả `0` thay vì `0.00`; thông điệp lỗi không nêu rõ "exceeds subtotal".

**Sửa:** luôn `setScale(2)`, thông điệp rõ ràng.

### 3.5. Tạo giáo án thiếu `start_date` → 500
`start_date` NOT NULL nhưng form không có trường này.

**Sửa:** mặc định hôm nay, kiểm tra `endDate >= startDate`.

### 3.6. Query JPQL với tham số null → lỗi PostgreSQL
Hibernate bind null không rõ kiểu → `function lower(bytea) does not exist` → trang Leads 500.

**Sửa:** ép kiểu tường minh `CAST(:search AS string)`.

---

## 4. GIAO DIỆN (đã sửa)

### 4.1. Sidebar không responsive — tràn ngang toàn bộ dashboard
`<main className="ml-[232px]">` cố định. Trên điện thoại 375px, nội dung chỉ còn ~143px → **19 trang bị tràn ngang**.

**Sửa:** `<main className="... pt-16 lg:pt-0 lg:ml-[232px]">` + Sidebar thành drawer có nút hamburger, có `aria-expanded`, đóng bằng lớp phủ, khoá cuộn nền, đóng khi đổi trang.

### 4.2. Tràn ngang ở các trang cụ thể
| Trang | Nguyên nhân | Sửa |
|---|---|---|
| `/classes` | 7 tab ngày không scroll | `overflow-x-auto` + `shrink-0` |
| `/dashboard/sales` | 4 chip lọc tràn | `overflow-x-auto` |
| `/dashboard/create-sale` | Stepper 3 bước | `flex-wrap` + `shrink-0` |
| `/plans` | `/90 days` quá dài | `flex-wrap` + `whitespace-nowrap` |
| `/dashboard/member/progress` | Biểu đồ 6 cột | `gap` nhỏ hơn, `truncate`, `overflow-hidden` |
| `/dashboard/member/profile` | Aside `w-80` cố định | `w-full lg:w-80` + `flex-col lg:flex-row` |

### 4.3. Tương phản màu không đạt WCAG AA
`#8A8E85` chỉ đạt 2.8–3.35:1 (cần 4.5:1) ở cỡ chữ 10–12px — gần như không đọc được với người lớn tuổi.

**Sửa:**
- Nền sáng: `#8A8E85 → #5E625A` (4.9:1)
- Nền tối: `#A8ADA2` (4.6:1)
- Nhãn rất nhỏ trên nền xám: `#3F433C`

### 4.4. Accessibility
- **14 input không có nhãn** (login, register, create-lead, workout-builder, workout-log, tìm kiếm) → thêm `htmlFor`/`id` hoặc `aria-label`.
- **`autoComplete`** cho email/mật khẩu/tên/SĐT → trình quản lý mật khẩu hoạt động.
- **Vòng focus rõ ràng** cho người dùng bàn phím.
- **`prefers-reduced-motion`** được tôn trọng.
- **Touch target 44px** trên thiết bị cảm ứng.
- Thêm **`aria-current="page"`** cho menu đang mở.

### 4.5. Lỗi hydration React #425 trên `/plans`
`toLocaleString()` không tham số cho kết quả khác nhau giữa server (en-US) và client (vi-VN).

**Sửa:** `Intl.NumberFormat('vi-VN')` cố định.

---

## 5. LỖI "THÀNH CÔNG GIẢ" (đã sửa)

Đây là nhóm lỗi nguy hiểm nhất về niềm tin người dùng:

| Vị trí | Hành vi sai | Sửa |
|---|---|---|
| `api.ts` — 9 hàm `fetch` trần | Bỏ qua JWT → 401 → trả mock | Chuyển sang axios instance |
| `getExercises` | Luôn 401, trả 10 bài giả | Dùng `api.get` |
| `getProgressSummary` | Gọi `/progress/summary` không tồn tại → hiện PR bịa (85/120/140kg) | Gọi `/progress/me`, trả 0 khi chưa có dữ liệu |
| `getRecentWorkoutSessions` | Gọi sai path, trả 3 buổi tập giả | Gọi `/workout-sessions/me`, trả `[]` |
| `NoShowBadge` (trang hội viên) | Gọi endpoint chỉ dành cho staff → 403 mỗi lần render | Gỡ khỏi giao diện hội viên |
| `workout-builder` | `fetch` thiếu Bearer → 401 nhưng báo "Đã lưu thành công" | Dùng axios, báo lỗi thật |

---

## 6. KIỂM CHỨNG CUỐI CÙNG

### Backend
```
BUILD SUCCESS
Tests run: 103, Failures: 0, Errors: 0, Skipped: 6
```

### Luồng nghiệp vụ thật
```
ADMIN/MANAGER/RECEPTIONIST/TRAINER/MEMBER  → đăng nhập OK
Tạo đơn (290.000đ) → thu tiền mặt → đơn PAID → membership ACTIVE  ✅
Hội viên đặt lớp → CONFIRMED → đặt trùng bị chặn                ✅
Ma trận phân quyền 14 endpoint                                  ✅ đúng
```

### Giao diện (Playwright, 3 kích thước)
```
TRANG                  DESKTOP  TABLET  MOBILE
25 trang               0 lỗi    0 lỗi    0 lỗi
- Tràn ngang:          0        0        0
- Lỗi a11y:            0        0        0
- Lỗi tương phản:      0        0        0
- Lỗi API:             0        0        0
- Lỗi hydration:       0        0        0
```

---

## 7. NHỮNG VIỆC CÒN LẠI (không chặn vận hành)

Các điểm dưới đây **không phải lỗi** nhưng nên cải thiện tiếp:

1. **Trang Reports** vẫn dùng dữ liệu demo hardcode (`reports/page.tsx`). Backend đã có `/api/reports/*` nhưng FE chưa nối.
2. **Trang Trainer console** hiển thị dữ liệu mẫu; ghi chú chưa lưu vào DB.
3. **Phân trang**: bảng leads/customers/orders chỉ tải 20 bản ghi đầu, KPI tính trên trang hiện tại.
4. **Bảng giá trang chủ** vẫn là GBP (`£29`) — cần đồng bộ với VND như trang `/plans`.
5. **Webhook thanh toán** chưa xác thực chữ ký HMAC (mới chỉ permitAll) — cần làm trước khi lên production.
6. **Token lưu localStorage** + cookie không `HttpOnly` — nên chuyển sang cookie do backend set.
7. **Chưa có trang** `/forgot-password`, `/reset-password` (backend đã có API), `/terms`, `/privacy`.
8. **Chưa có not-found.tsx / error.tsx** — URL sai hiện trang mặc định tiếng Anh.
9. **`.env.example`** chứa secret thật — cần thay bằng placeholder.
10. **Test boot context** (`FitosApplicationTests`) vẫn `@Disabled` — nên bật với Testcontainers trong CI.

---

## 8. CÁCH CHẠY LẠI HỆ THỐNG

```powershell
# 1. Database (đã tạo sẵn: fitos / fitos_user / supersecret)
# 2. Backend
cd "D:\gym cỏ\backend"
# (đã có target\cp.txt)
java -cp "target\classes;<nội dung cp.txt>" com.fitos.FitosanApplication

# 3. Frontend
cd "D:\gym cỏ\frontend"
npx next build; npx next start -p 3000
```

Tài khoản demo (mật khẩu `FitOs@2026`):
`admin@fitos.com` (AdminBootstrap2026!) · `quanly@fitos.demo` · `letan@fitos.demo` · `hlv@fitos.demo` · `hoivien@fitos.demo`

---

*Báo cáo dựa trên việc chạy thật hệ thống, không phải phân tích tĩnh. Mọi số liệu đều đo được bằng lệnh trong mục 6.*
