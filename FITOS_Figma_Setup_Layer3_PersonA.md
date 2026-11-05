# FITOS — FIGMA SETUP & UI DESIGN SYSTEM
## LAYER 3: FRONTEND — PHẦN VIỆC NGƯỜI A (12H)

> **Mục tiêu:** Hướng dẫn chi tiết từng bước thiết lập file Figma, định nghĩa Design System Tokens, xây dựng thư viện Component chuẩn Auto-Layout, và dàn trang (Wireframe/Hi-fi Layout) cho toàn bộ 6 màn hình thuộc phạm vi Người A (Auth, Lead CRM, Manager Dashboard, Member Home).

---

## 1. CẤU TRÚC TRANG TRONG FIGMA (PAGES STRUCTURE)

Khi tạo file mới trên Figma (đặt tên: `FITOS — Web Platform UI`), hãy chia thành các trang (Pages) như sau:

```
FITOS — Web Platform UI
├── 🎨 01. Cover & Design System Tokens (Colors, Typography, Elevation)
├── 🧩 02. Component Library (Buttons, Inputs, Badges, Cards, Modals)
├── 🔐 03. Flow: Authentication (/login, /register, role-guard)
├── 📊 04. Screen: Manager Dashboard (/dashboard — KPIs, Revenue, Funnel)
├── 🎯 05. Screen: Lead CRM Pipeline (/dashboard/leads, /create, /[id] Timeline)
└── 🏋️ 06. Screen: Member Portal Home (/dashboard/member — Streak, Feed)
```

---

## 2. DESIGN SYSTEM TOKENS (FIGMA VARIABLES & STYLES)

### 2.1. Bảng màu (Color Palette & Semantic Tokens)

| Token Name | Hex Code | Figma Role | Ứng dụng thực tế |
| :--- | :--- | :--- | :--- |
| **`fitos-dark`** | `#171A16` | Surface / Dark Base | Sidebar nền tối, Dark mode header, text tiêu đề tương phản cao |
| **`fitos-near-black`** | `#111310` | Text / Primary | Màu chữ chính tiêu đề, số liệu KPI, button text trên nền lime |
| **`fitos-lime`** | `#D7FF3F` | Brand Accent / Primary | Nút CTA chính (`+ Add Lead`, `Login`), badge active, điểm nhấn biểu đồ |
| **`fitos-lime-hover`**| `#C4E82E` | Brand Accent / Hover | Trạng thái hover của nút bấm chính |
| **`fitos-bg`** | `#F5F5F0` | Background / Main Page | Màu nền canvas tổng thể của Dashboard và ứng dụng (Warm Cream) |
| **`fitos-off-white`** | `#ECEDE6` | Surface / Secondary | Nền bảng con, hover state của list item, thẻ container phụ |
| **`fitos-border`** | `#DADCD3` | Border / Stroke | Viền card, đường kẻ phân cách bảng, border input |
| **`fitos-muted`** | `#5E625A` | Text / Secondary | Chữ phụ, mô tả ngắn, placeholder input, nhãn trục biểu đồ |
| **`fitos-subtle`** | `#8A8E85` | Text / Tertiary | Chữ ghi chú nhỏ, nhãn uppercase tracking (11px) |
| **`fitos-green`** | `#526B00` | Feedback / Success | Text % tăng trưởng dương (`▲ +12%`), trạng thái `Converted` |
| **`fitos-info`** | `#E9EDFF` | Feedback / Info | Nền badge `Contacted`, thông báo hệ thống |
| **`fitos-danger`** | `#EF4444` | Feedback / Danger | Badge `Lost`, thông báo lỗi validation |

### 2.2. Kiểu chữ (Typography Scale — Font Family: `Inter`)

| Figma Text Style | Size / Line Height | Weight | Tracking | Ứng dụng |
| :--- | :--- | :--- | :--- | :--- |
| **`Display / Hero`** | 32px / 40px | Bold (700) | -0.02em | Tiêu đề lớn trên Dashboard, Landing |
| **`Heading / H1`** | 24px / 32px | Bold (700) | -0.01em | Tiêu đề trang (`Dashboard`, `Leads`) |
| **`Heading / H2`** | 20px / 28px | SemiBold (600) | -0.01em | Tiêu đề khối card, section header |
| **`KPI / Metric Large`**| 30px / 36px | Bold (700) | -0.02em | Chỉ số doanh thu `£18,450`, số lượng `2,412` |
| **`Body / Regular`** | 14px / 20px | Regular (400) | 0 | Văn bản nội dung, dữ liệu ô trong bảng |
| **`Body / Medium`** | 14px / 20px | Medium (500) | 0 | Tên cột trong bảng, nhãn dữ liệu |
| **`Body / Bold`** | 14px / 20px | SemiBold (600) | 0 | Tên khách hàng, nút bấm tiêu chuẩn |
| **`Caption / Subtle`** | 12px / 16px | Regular (400) | 0 | Chú thích thời gian, text phụ dưới KPI |
| **`Badge / Micro`** | 11px / 14px | SemiBold (600) | +0.10em | Nhãn IN HOA trên đầu card: `STUDIO MANAGER` |

### 2.3. Hiệu ứng & Bo góc (Elevation & Radii)
* **Corner Radius:**
  * Small (`8px`): Input field, Button, Tag, Status Pill.
  * Medium (`12px`): Stat Card, Table container, Dropdown menu.
  * Large (`16px`): Modal dialog, Slide-over detail drawer.
  * Full (`9999px`): Avatars, Notification dot, AI Score badge.
* **Drop Shadow (Elevation):**
  * `Elevation 1 (Card)`: `0px 1px 3px rgba(0, 0, 0, 0.05), 0px 1px 2px rgba(0, 0, 0, 0.03)`
  * `Elevation 2 (Floating/Dropdown)`: `0px 4px 12px rgba(0, 0, 0, 0.08)`
  * `Elevation 3 (Modal/Drawer)`: `0px 12px 32px rgba(0, 0, 0, 0.16)`

---

## 3. THƯ VIỆN COMPONENT TRONG FIGMA (COMPONENT LIBRARY)

Tất cả component phải bật **Auto Layout (`Shift + A`)** với thuộc tính co giãn linh hoạt:

### 3.1. Primary Button
* **Auto-layout:** Horizontal, Padding: `px: 16px, py: 10px`, Gap: `8px`, Corner radius: `8px`.
* **Variants:**
  * `Primary Lime`: Fill `#D7FF3F`, Text `#111310` (SemiBold 14px).
  * `Secondary Dark`: Fill `#171A16`, Text `#FFFFFF`.
  * `Outline`: Fill transparent, Stroke `1px #DADCD3`, Text `#111310`.
  * `Danger`: Fill `#FEE2E2`, Text `#DC2626`.
* **States:** `Default`, `Hover`, `Pressed`, `Disabled`, `Loading (với spinner)`.

### 3.2. Form Input Field
* **Auto-layout:** Vertical, Gap: `6px`.
* **Cấu trúc:**
  1. Label: 13px Medium `#111310` (kèm dấu `*` đỏ nếu bắt buộc).
  2. Input box: Auto-layout horizontal, height `42px`, padding `px: 12px`, border `1px #DADCD3`, radius `8px`, background `#FFFFFF`.
  3. Helper / Error Text: 12px Regular (`#5E625A` hoặc `#EF4444`).

### 3.3. KPI Stat Card Component
* **Kích thước:** Width `Fill Container` hoặc `Fixed 260px`, height `Auto`.
* **Auto-layout:** Vertical, Padding: `20px`, Gap: `8px`, Radius: `12px`, Fill: `#FFFFFF`, Stroke: `1px #DADCD3`.
* **Cấu trúc từ trên xuống:**
  1. Header row: Nhãn uppercase `11px SemiBold tracking +0.1em #8A8E85` (ví dụ: `MRR`, `ACTIVE MEMBERS`).
  2. Value row: Text `30px Bold #111310` (ví dụ: `£18,450`).
  3. Footer row: Badge tăng trưởng `12px Medium #526B00` (ví dụ: `▲ +12% vs last month`).

### 3.4. Status Pill Component (Lead Lifecycle)
* **Auto-layout:** Horizontal, Padding: `px: 10px, py: 4px`, Radius: `9999px`, Font: `12px SemiBold`.
* **Variants theo Status:**
  * `NEW`: Background `#FEF9C3`, Text `#854D0E` (Vàng).
  * `CONTACTED`: Background `#DBEAFE`, Text `#1E40AF` (Xanh dương).
  * `TRIAL`: Background `#D7FF3F`, Text `#526B00` (Xanh chanh FITOS).
  * `CONVERTED`: Background `#DCFCE7`, Text `#166534` (Xanh lá).
  * `LOST`: Background `#FEE2E2`, Text `#991B1B` (Đỏ).

### 3.5. AI Opportunity Score Badge Component
* **Auto-layout:** Horizontal, Padding: `px: 10px, py: 4px`, Radius: `9999px`, Font: `12px SemiBold`.
* **Variants:**
  * `Score ≥ 75`: Background `#D7FF3F`, Text `#111310` (Ví dụ: `★ 88 High Intent`).
  * `Score 40-74`: Background `#FEF3C7`, Text `#92400E` (Ví dụ: `• 54 Medium`).
  * `Score < 40`: Background `#F3F4F6`, Text `#6B7280` (Ví dụ: `• 28 Cold`).

---

## 4. CHI TIẾT THIẾT KẾ CÁC MÀN HÌNH CỦA NGƯỜI A (SCREEN BLUEPRINTS)

Desktop Canvas chuẩn: **Width: 1440px | Height: 1024px** (Cố định Sidebar 240px, Main Content 1200px co giãn).

---

### Màn hình 1: `/dashboard` — Manager Overview Dashboard (Task #78)

* **Layout tổng quan:**
  * **Sidebar (Trái, 240px, Dark `#171A16`):**
    * Logo FITOS Studio với icon tia chớp xanh chanh.
    * Nhóm Menu: `Dashboard` (Active Lime pill), `Members`, `Classes`, `Schedule`, `Payments`, `CRM (Leads)`, `Reports`, `Settings`.
    * Bottom profile: Avatar tròn, tên `Alex Thompson`, nhãn `Studio Manager`.
  * **Header (Trên cùng):**
    * Sub-header: `STUDIO MANAGER · CITY CENTRE`.
    * Title: `Dashboard` — `Tuesday, 15 July 2025`.
    * Quick CTA Button: `+ New lead` (Primary Lime).
  * **Hàng 1 — 4 KPI Cards (Grid 4 cột, gap 20px):**
    * Card 1: **MRR** — `£18,450` (`▲ +12% vs last month`).
    * Card 2: **Active Members** — `2,412` (`▲ +3% vs last month`).
    * Card 3: **Class Fill Rate** — `79.6%` (`▲ +4% vs last week`).
    * Card 4: **New Leads** — `87` (`This month · 34% conversion`).
  * **Hàng 2 — Monthly Revenue Trend (Card lớn 8 cột, 800px):**
    * Header: `Monthly revenue` kèm dropdown lọc thời gian (`Revenue 6 Months`).
    * Biểu đồ cột Pure CSS: Trục tung £0 -> £20,000, 6 cặp cột (Doanh thu tháng trước & tháng này) dùng màu `#171A16` và `#D7FF3F`.
    * Tooltip hover: `Jul: £18,450 (+7.2%)`.
  * **Hàng 3 — CRM Sales Funnel Breakdown (Card 12 cột, 1150px):**
    * Header: `CRM sales funnel breakdown`.
    * Phễu 6 tầng hình thang (hoặc thanh bar phân khúc ngang):
      * `NEW`: 87 leads (100%).
      * `CONTACTED`: 54 leads (62%).
      * `TRIAL`: 28 leads (32%).
      * `CONVERTED`: 12 members (14%).

---

### Màn hình 2: `/dashboard/leads` — Lead CRM Pipeline Table (Task #74)

* **Header:**
  * Title: `Lead Pipeline` — `Track and convert prospective members`.
  * Nút `+ Add Lead` mở Modal hoặc trang `/dashboard/leads/create`.
* **Top Filter Bar:**
  * Status Filter Tabs: `All (77)`, `New (32)`, `Contacted (18)`, `Trial (14)`, `Converted (8)`, `Lost (5)`. Tab đang chọn có viền gạch chân `#D7FF3F`.
  * Search Box: Input có icon kính lúp, placeholder: `Search leads by name, phone, email...`.
  * Dropdown: Lọc theo Source (`All Sources`, `Walk-in`, `Facebook Ads`, `Website`, `Referral`).
* **Leads Data Table (Auto-layout Table):**
  * Columns:
    1. Checkbox chọn hàng.
    2. `Name & Avatar`: Avatar tròn + Tên in đậm + Nguồn nhỏ bên dưới.
    3. `Phone`: Số điện thoại có định dạng chuẩn (Ví dụ: `(803) 348-3789`).
    4. `Source Badge`: Pill màu xám nhạt (`Facebook Ads`, `Website Form`, `Referral`).
    5. `Status Pill`: Pill màu tương ứng (`New`, `Contacted`, `Trial`, `Converted`, `Lost`).
    6. `AI Opportunity Score`: Badge xanh chanh (`88 High Intent`, `92 High Intent`, `74 Medium`).
    7. `Actions`: Icon gọi điện (Call), Icon gửi tin (Email), Icon 3 chấm (Menu).
  * Click vào bất kỳ hàng nào sẽ trượt ngăn kéo chi tiết (Slide-over Drawer) từ bên phải sang.

---

### Màn hình 3: `/dashboard/leads/[id]` — Lead Detail & Timeline Drawer (Task #76)

* **Kích thước Drawer:** Width `420px`, Height `100%`, viền đổ bóng `Elevation 3`.
* **Header:**
  * Nút đóng `✕`.
  * Avatar lớn, Tên khách hàng: `Sarah Johnson`, Email: `sarah.johnson@gmail.com`.
  * Staff phụ trách: `Owner: Sarah J.`.
  * Nút hành động nhanh: `Call`, `Log Trial`, `Convert to Member`.
* **Khối Body — Activity Timeline:**
  * Nút bấm: `+ Add Activity` (Mở modal ghi nhận cuộc gọi, lịch tập thử, tư vấn).
  * Danh sách hoạt động dạng trục thời gian dọc (Vertical line nối các node icon tròn):
    * **Node 1 (Hôm nay 10:45 AM):** Icon lịch `Trial Consultation Scheduled` — Ghi chú: "Tư vấn gói PT giảm mỡ 10 buổi. Hẹn tập thử 18:00 ngày mai." — Staff: `Marko V.`
    * **Node 2 (Hôm qua 1:15 PM):** Icon điện thoại `Logged Call` — Ghi chú: "Đã liên hệ qua Zalo, khách có nhu cầu tập giảm cân sau sinh."
    * **Node 3 (23 Oct 9:15 AM):** Icon ghi chú `Lead Created` — Nguồn: `Website Form`.

---

### Màn hình 4: `/dashboard/leads/create` — Form tạo Lead mới (Task #75)

* **Layout:** Card căn giữa (Width: 640px) hoặc Modal Popup.
* **Fields:**
  * Full Name (`*` Bắt buộc)
  * Phone Number (`*` Bắt buộc)
  * Email Address
  * Date of Birth (Date picker)
  * Fitness Goal (Select: `WEIGHT_LOSS`, `MUSCLE_GAIN`, `GENERAL_FITNESS`, `STRENGTH`)
  * Source (Select: `WALK_IN`, `WEBSITE`, `FACEBOOK`, `REFERRAL`, `OTHER`)
  * Assign to Staff (Select danh sách huấn luyện viên / nhân viên tư vấn)
  * Initial Notes (Textarea)
* **Buttons:** `Cancel` (Outline) và `Create Lead` (Primary Lime).

---

### Màn hình 5: `/login` & `/register` — Authentication Flow (Tasks #71 & #72)

* **Layout Split-Screen thời thượng:**
  * **Cột trái (50%):** Form đăng nhập / đăng ký trên nền kem `#F5F5F0`.
    * Logo FITOS Studio.
    * Tiêu đề: `Welcome back` hoặc `Create an account`.
    * Input Email & Password (có icon mắt ẩn/hiện mật khẩu).
    * Với trang `/register`: thêm trường `Full Name`, `Phone`, và `Role Selector` (`Member`, `Trainer`, `Receptionist`).
    * Checkbox: `Remember me` & Link `Forgot password?`.
    * Nút Submit: `Sign in` / `Create account` (Lime background, full width).
  * **Cột phải (50%):** Banner hình ảnh phòng tập thể hình cao cấp với filter tối và slogan thương hiệu: *"Intelligent Coaching & Studio Management Powered by AI"*.

---

### Màn hình 6: `/dashboard/member` — Member Home Portal (Task #77)

* **Dành riêng cho Hội viên sau khi đăng nhập:**
  * **Hero Welcome:** *"Welcome back, Alex! You're on a 5-day workout streak 🔥"*.
  * **Hàng 3 Cards nổi bật:**
    1. `Next Scheduled Session`: Ca tập tiếp theo lúc 17:30 (Box & Burn với Coach Leo). Nút `Check-in QR`.
    2. `Membership Status`: Gói `Perform` (Còn 22 ngày). Nút `Renew / Upgrade`.
    3. `Fitness Progress`: Cân nặng hiện tại `74.5 kg` (Giảm -5.5 kg từ khi bắt đầu), Streak `5 ngày liên tục`.
  * **Activity Feed:** Lịch sử các buổi tập vừa hoàn thành trong tuần.

---

## 5. BẢNG CHECKLIST BÀN GIAO CHO LAYER 3 (DEV HANDOFF)

Sau khi dàn trang xong trên Figma, bạn có thể đối chiếu checklist này để bắt tay vào viết code Frontend bằng Next.js + Tailwind CSS:

* [ ] Đã tạo đủ bảng màu chuẩn vào `tailwind.config.ts`.
* [ ] Component `Sidebar` đã có các liên kết định tuyến chính xác.
* [ ] Trang `/dashboard` hiển thị đủ 4 KPI cards, biểu đồ doanh thu và phễu CRM.
* [ ] Bảng `/dashboard/leads` có ô tìm kiếm debounce, tab lọc status và badge AI Score.
* [ ] Drawer `/dashboard/leads/[id]` hiển thị timeline hoạt động và modal ghi nhận lịch tương tác.
* [ ] Trang `/login` và `/register` đã sẵn sàng kết nối API JWT Backend đã hoàn thiện ở Layer 2.
