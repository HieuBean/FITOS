# FITOS — FITNESS STUDIO MANAGEMENT & AI COACHING PLATFORM

> **Functional Specification — Full Function Tree**
>
> Tài liệu này mô tả toàn bộ chức năng nghiệp vụ của FITOS theo cấu trúc: **Chức năng chính → chức năng phụ → mô tả → actor → business rules**.
>
> FITOS là nền tảng B2B2C cho Fitness Studio/Gym: mỗi Studio là một đơn vị sử dụng FITOS; Manager/Owner, Receptionist và Trainer vận hành Studio; Customer là khách hàng của Studio. Quy trình trung tâm là **Visitor → Lead → Customer → Order → Payment → Membership/PT Package → Member sử dụng dịch vụ → Booking/PT → Workout → Progress → AI → Renewal/Upsell**. Customer có thể tạo tài khoản trước khi mua dịch vụ.

---

## 1. PRODUCT SCOPE

### 1.1. Mục tiêu

FITOS giúp Fitness Studio quản lý:

- Khách vãng lai và Lead.
- CRM và customer lifecycle.
- Membership và PT package.
- Sales, Order và Payment.
- Class, Schedule và Booking.
- Check-in và Attendance.
- Trainer và Personal Training.
- Workout Plan, Workout Log và Progress.
- AI Coach.
- Dashboard, KPI và Reports.
- User, Role, Security và Audit.

### 1.2. FITOS không phải

- Fitness app cá nhân đơn thuần.
- Website e-commerce thông thường.
- Chatbot AI độc lập.
- Full ERP.
- Full HRM.
- Healthcare/medical system.
- IoT/wearable platform.

### 1.3. Phạm vi nhiều Studio

- FITOS phục vụ nhiều Studio độc lập. Mỗi Lead, Customer, Plan, Order, Payment, Membership, PT Package, lịch, Booking, Attendance và dữ liệu tập luyện phải thuộc đúng một Studio. User là tài khoản đăng nhập chung, có thể được cấp vai trò ở nhiều Studio; Customer Profile và quyền của User được liên kết riêng theo từng Studio.
- User có quyền ở nhiều Studio phải chọn Studio đang làm việc; backend kiểm tra quyền của User tại Studio đó trên mỗi request. Mọi thao tác đọc/ghi dữ liệu nghiệp vụ xác định Studio từ ngữ cảnh đã xác thực, không tin `studio_id` do client tự gửi. Dữ liệu của Studio này không được hiển thị hoặc sửa bởi Studio khác chỉ vì User có cùng email hay biết ID bản ghi.
- Visitor chỉ xem thông tin công khai của Studio đã chọn. Việc tạo Studio và cấp quyền nhân sự chỉ do vai trò quản trị nền tảng thực hiện.
- **Multi-studio** là nhiều Studio độc lập trên FITOS; **multi-branch** là nhiều chi nhánh trong cùng một Studio và thuộc phạm vi sau MVP.

---

# 2. ACTORS / ROLES

## 2.1. Visitor

Người chưa có account hoặc chưa là Member.

Quyền:
- Xem Landing Page.
- Xem Membership Plans.
- Xem Class Schedule.
- Xem thông tin Fitness Studio.
- Gửi inquiry/register interest.

Không được:
- Xem dữ liệu cá nhân.
- Xem workout cá nhân.
- Dùng AI Coach cá nhân.
- Booking dịch vụ yêu cầu account.

## 2.2. Lead

Người có nhu cầu nhưng chưa trở thành Customer/Member.

Thông tin:
- Full Name.
- Phone.
- Email.
- Goal.
- Source.
- Status.
- Owner.
- Notes.
- Activity History.

Lead Status:

```text
NEW
CONTACTED
INTERESTED
TRIAL
NEGOTIATING
CONVERTED
LOST
```

## 2.3. Member / Customer

**Customer** là hồ sơ khách hàng của một Studio, có thể được tạo từ Lead hoặc đăng ký trực tiếp. **User** là tài khoản đăng nhập có thể liên kết với Customer. **Member** là Customer đang có ít nhất một quyền sử dụng dịch vụ còn hiệu lực từ Membership hoặc PT Package; từng quyền cụ thể vẫn phụ thuộc gói đã mua. Việc có tài khoản hoặc trạng thái hồ sơ `ACTIVE` tự nó không cấp quyền vào lớp/PT. Customer chưa mua gói vẫn được xem hồ sơ, gói dịch vụ và lịch công khai khi có tài khoản.

Quyền:
- Quản lý Profile.
- Xem/mua/gia hạn Membership.
- Booking Class/PT.
- Check-in.
- Xem Workout.
- Ghi Workout.
- Xem Progress.
- Dùng AI Coach.
- Xem Payment History.
- Xem lịch sử hoạt động.

## 2.4. Trainer

Quyền:
- Xem Schedule.
- Xem Assigned Clients.
- Xem Workout History.
- Tạo Workout Plan.
- Chỉnh Workout Plan.
- Xem Progress.
- Xem AI Insights.
- Ghi Training Notes.
- Quản lý PT Session.

## 2.5. Receptionist

Quyền:
- Quản lý Lead.
- Quản lý Customer cơ bản.
- Tạo Order.
- Xử lý Payment.
- Booking cho khách.
- Check-in.
- Hỗ trợ Walk-in/Trial.

## 2.6. Manager / Owner

Quyền:
- Dashboard.
- CRM.
- Customer.
- Plans.
- Memberships.
- Sales.
- Payments.
- Classes.
- Booking.
- Trainer.
- Reports.
- Renewal/Retention.

## 2.7. Admin

Admin của Studio có quyền quản trị trong Studio được cấp. Quản trị viên nền tảng là vai trò riêng, chỉ dùng để tạo Studio và quản lý cấu hình xuyên Studio; không mặc nhiên xem hồ sơ sức khỏe của Customer.

Quyền:
- Users.
- Roles.
- System Settings.
- Audit Logs.
- Security/System Administration.

---

# 3. CUSTOMER LIFECYCLE

```text
VISITOR / WALK-IN
        ↓
      LEAD
        ↓
CONTACT / CONSULTATION
        ↓
      TRIAL
        ↓
   INTERESTED
        ↓
 CUSTOMER PROFILE / ACCOUNT
        ↓
     PURCHASE / ORDER
        ↓
     PAYMENT
        ↓
MEMBERSHIP / PT PACKAGE ACTIVE
        ↓
  MEMBER ĐỦ ĐIỀU KIỆN SỬ DỤNG
        ↓
┌───────┼─────────┐
│       │         │
BOOKING PT      WORKOUT
│       │         │
└───────┼─────────┘
        ↓
     PROGRESS
        ↓
RENEWAL / UPSELL
```

Mục tiêu là hệ thống phải lưu được lịch sử của customer xuyên suốt vòng đời, không làm mất lịch sử Lead sau khi convert.

Các nhánh hợp lệ: Customer có thể đăng ký trực tiếp mà không đi qua Lead; Lead có thể chuyển thành Customer trước khi có tài khoản và trước khi mua gói; PT Package có thể được mua độc lập nếu chính sách Studio cho phép. Quyền sử dụng từng dịch vụ dựa trên gói đã thanh toán, hiệu lực ngày và quyền lợi của gói, không chỉ dựa vào vai trò đăng nhập.

---

# 4. CORE FUNCTION 1 — CUSTOMER & CRM MANAGEMENT

## 4.1. Visitor / Walk-in Management

### Chức năng
- Tiếp nhận Visitor.
- Tạo Lead từ Visitor.
- Ghi nhận thông tin ban đầu.
- Ghi nhận Goal.
- Ghi nhận Source.
- Ghi chú nhu cầu.
- Theo dõi trạng thái tiếp cận.

### Input
- Full Name.
- Phone.
- Email.
- Date of Birth.
- Goal.
- Source.
- Notes.

### Source
```text
WALK_IN
WEBSITE
FACEBOOK
INSTAGRAM
REFERRAL
ADVERTISEMENT
OTHER
```

### Goal
```text
WEIGHT_LOSS
MUSCLE_GAIN
GENERAL_FITNESS
STRENGTH
MOBILITY
OTHER
```

## 4.2. Lead Management

### Chức năng
- Create Lead.
- View Lead.
- Edit Lead.
- Search Lead.
- Filter Lead.
- Sort Lead.
- Assign Lead.
- Change Lead Status.
- Convert Lead.
- Mark Lead Lost.
- Reopen Lead nếu business policy cho phép.

### Lead Status
```text
NEW
CONTACTED
INTERESTED
TRIAL
NEGOTIATING
CONVERTED
LOST
```

### Business Rules
- Lead phải có Name và Phone.
- Converted Lead không được convert lần hai.
- Lead history không bị xóa khi convert.
- Chỉ Staff có quyền CRM mới được chuyển đổi Lead.
- Lead chỉ thuộc một Studio. Tìm Lead trùng theo số điện thoại/email trong Studio đó; Staff xác nhận liên kết Customer hiện có thay vì tạo hồ sơ trùng.
- `CONVERTED` và `LOST` là trạng thái kết thúc. Chỉ Manager được mở lại `LOST` và phải ghi lý do; không đổi trực tiếp `CONVERTED` sang trạng thái khác.

## 4.3. Lead Activity Management

### Chức năng
- Add Activity.
- View Activity Timeline.
- Filter Activity.
- Add Note.

### Activity Type
```text
CALL
MESSAGE
CONSULTATION
TRIAL
FOLLOW_UP
OTHER
```

### Activity Data
- Actor.
- Type.
- Note.
- Timestamp.
- Related Lead.

## 4.4. Consultation Management

### Chức năng
- Schedule Consultation.
- Record Consultation.
- Update Consultation Result.
- Follow-up sau Consultation.

### Output
- Interested.
- Not Interested.
- Schedule Trial.
- Continue Follow-up.

## 4.5. Trial Management

### Chức năng
- Schedule Trial.
- Reschedule Trial.
- Cancel Trial.
- Mark Trial Completed.
- Record Trial Notes.
- Follow-up Trial.

Trial có thể dẫn tới:
- Purchase.
- Continue Negotiation.
- Lost.

MVP: Trial được đặt cho Lead/Customer chưa có Membership; mỗi người được một Trial miễn phí cho cùng dịch vụ tại một Studio. Trial có lịch, sức chứa, trạng thái, người phụ trách và kết quả riêng; hủy Trial phải trả lại chỗ. Trial không tự tạo Membership hoặc quyền sử dụng dịch vụ khác.

## 4.6. Lead Conversion

### Flow

```text
LEAD
  ↓
CONVERT
  ↓
CUSTOMER PROFILE
  ↓
ACCOUNT / INVITATION (có thể thực hiện sau)
  ↓
ORDER (khi khách quyết định mua)
  ↓
PAYMENT
  ↓
MEMBERSHIP
```

### Chức năng
- Validate Lead.
- Convert Lead.
- Create Customer Profile.
- Create Account.
- Send Invitation.
- Preserve Lead history.

Chuyển đổi Lead chỉ tạo hoặc liên kết Customer trong cùng Studio và giữ nguyên lịch sử Lead. Tài khoản đăng nhập được liên kết với Customer qua lời mời hoặc xác minh danh tính; không tự tạo Membership. Nếu Customer đã tồn tại, không tạo Customer thứ hai.

## 4.7. Customer Management

### Chức năng
- Create Customer.
- View Customer.
- Edit Customer.
- Search Customer.
- Filter Customer.
- View Membership.
- View Booking.
- View Workout.
- View Progress.
- View Payments.
- View Activity Timeline.

### Customer Detail
```text
Overview
Membership
Bookings
Workout
Progress
Payments
Activity
```

## 4.8. Renewal / Retention

### Chức năng
- Detect Membership nearing expiration.
- Create Renewal Opportunity.
- Display Expiring Members.
- Contact Customer.
- Renew Membership.
- Track Renewal History.

### MVP rule
Nếu Membership hết hạn trong 7 ngày: → tạo Renewal Opportunity.

Mỗi Membership chỉ có tối đa một cơ hội gia hạn đang mở cho cùng kỳ hết hạn. Khi đã gia hạn hoặc hủy, cơ hội được đóng; không tạo lại ở mỗi lần chạy kiểm tra. Cơ hội gồm Studio, Customer, Membership, ngày hết hạn, người phụ trách và trạng thái liên hệ.

Không bắt buộc dùng ML churn prediction.

---

# 5. CORE FUNCTION 2 — SERVICE & SALES MANAGEMENT

## 5.1. Service Plan Management

FITOS quản lý hai nhóm dịch vụ:

```text
MEMBERSHIP
PT_PACKAGE
```

### Plan Fields
- Name.
- Type.
- Description.
- Price.
- Duration.
- Included Sessions.
- Status.

## 5.2. Membership Plan Management

Plan thuộc một Studio. Giá, thời hạn, số buổi và quyền lợi được chụp lại trên Order Item lúc mua; sửa Plan sau đó không thay đổi giao dịch hoặc gói đã bán. Plan ngừng bán vẫn hiển thị trong lịch sử.

### Chức năng
- Create Plan.
- Edit Plan.
- View Plan.
- Activate/Deactivate Plan.
- Configure Price.
- Configure Duration.
- Configure Benefits.
- Configure Sessions nếu cần.

Ví dụ:
- Basic.
- Premium.
- VIP.

## 5.3. PT Package Management

### Chức năng
- Create PT Package.
- Set Total Sessions.
- Set Expiration.
- View Remaining Sessions.
- Track Used Sessions.
- Activate/Deactivate Package.

Ví dụ:
- PT 5 sessions.
- PT 10 sessions.
- PT 20 sessions.

## 5.4. Sales Consultation

Staff có thể:
- Xem customer/Lead profile.
- Xem goal.
- Xem plans.
- So sánh plans.
- Chọn plan phù hợp.
- Áp dụng discount nếu có quyền.
- Tạo Order.

## 5.5. Order Management

### Chức năng
- Create Order.
- View Order.
- Search Order.
- Filter Order.
- Calculate subtotal.
- Apply discount.
- Calculate total.
- Cancel Order.
- View Order Detail.

### Order Status
```text
PENDING
PAID
CANCELLED
REFUNDED
```

### Rule
Total phải được tính và validate ở backend.
Order có một hoặc nhiều Order Item, mỗi item ghi Plan, số lượng, đơn giá tại thời điểm mua và giảm giá được duyệt. `subtotal = tổng giá item`; `total = max(0, subtotal - discount)`. Chỉ Manager hoặc nhân sự được cấp quyền mới áp dụng giảm giá. Order `PENDING` có thể thanh toán lại sau một Payment thất bại; `FAILED` là trạng thái Payment, không phải Order. Hủy Order đã thanh toán phải đi qua quy trình hoàn tiền.

## 5.6. Service Activation

Flow:

```text
ORDER
 ↓
PAYMENT SUCCESS
 ↓
SERVICE ACTIVATION
 ↓
MEMBERSHIP / PT PACKAGE ACTIVE
```

Không activate dịch vụ nếu Payment chưa hợp lệ, ngoại trừ offline payment đã được Staff có quyền xác nhận.

Thanh toán tiền mặt/chuyển khoản chỉ hợp lệ sau khi Staff được cấp quyền xác nhận số tiền và mã tham chiếu. Mỗi Order Item chỉ được kích hoạt một lần, kể cả khi callback hoặc thao tác xác nhận được gửi lặp. Thanh toán một phần không kích hoạt dịch vụ trong MVP. Hoàn tiền toàn phần thu hồi quyền sử dụng chưa tiêu thụ; trường hợp đã sử dụng dịch vụ cần Manager xử lý và lưu quyết định trong Audit Log.

---

# 6. CORE FUNCTION 3 — MEMBERSHIP & SERVICE ACCESS MANAGEMENT

## 6.1. Membership Creation

Membership được tạo từ Order sau khi payment hợp lệ.

Thông tin:
- Customer.
- Plan.
- Start Date.
- End Date.
- Status.
- Auto Renew nếu hỗ trợ.

Membership thuộc đúng Customer và Studio của Order. Ngày bắt đầu mặc định là ngày kích hoạt, trừ khi Order ghi ngày bắt đầu tương lai. Ngày kết thúc được tính từ thời hạn Plan theo múi giờ của Studio.

## 6.2. Membership Lifecycle

```text
PENDING
 ↓
ACTIVE
 ↓
EXPIRED
```

Hoặc:
```text
ACTIVE → CANCELLED
```

### Status
```text
PENDING
ACTIVE
EXPIRED
CANCELLED
```

## 6.3. Membership Tracking

### Chức năng
- View Current Membership.
- View Expiration.
- View Remaining Days.
- View Benefits.
- View Status.
- View Membership History.

## 6.4. Membership Renewal

### Chức năng
- Renew.
- Calculate new end date.
- Create new transaction.
- Create new membership period.
- Preserve history.

Gia hạn luôn tạo Order và Payment mới, sau đó tạo kỳ Membership mới liên kết với kỳ trước để giữ lịch sử. Nếu gói cũ còn hiệu lực và cùng Plan, kỳ mới bắt đầu ngay sau ngày kết thúc kỳ cũ; nếu đã hết hạn, bắt đầu từ ngày kích hoạt mới. Đổi Plan là giao dịch đổi gói riêng. Một Payment không được tạo hai kỳ gia hạn.

## 6.5. Membership Pause / Resume

Pause/Resume là tính năng sau MVP. Nếu Studio bật tính năng này về sau, phải xác định số lần và thời gian tạm dừng, cách dời ngày hết hạn và tác động lên Booking đã có.

Chức năng sau MVP:
- Pause.
- Resume.
- Validate allowed period.

## 6.6. Membership Access Validation

Trước các operation cần membership:

```text
Is Membership Active?
Is Membership Within Valid Date?
Is Customer Eligible?
Does Plan include requested service?
Does Membership belong to current Studio?
```

Nếu không:
```text
ACCESS_DENIED
```

## 6.7. PT Package Usage

### Chức năng
- View Total Sessions.
- View Remaining Sessions.
- Complete PT Session.
- Decrease Remaining Sessions.
- Detect Expiration.

### Rule
Nếu Remaining Sessions = 0: → không cho tạo/complete thêm session.

PT Package phải còn hiệu lực ngày, đúng Studio và đúng Customer. Một buổi PT chỉ trừ lượt khi chuyển sang `COMPLETED` lần đầu. Buổi bị hủy trước khi hoàn thành không trừ lượt. Package hết hạn hoặc hết lượt không được đặt buổi mới; buổi đã đặt trước ngày hết hạn phải được hoàn thành trong thời hạn, trừ khi Manager xử lý ngoại lệ.

---

# 7. CORE FUNCTION 4 — CLASS, BOOKING & SCHEDULING

## 7.1. Class Management

### Chức năng
- Create Class.
- Edit Class.
- View Class.
- Cancel Class.
- Set Capacity.
- Set Room.
- Assign Trainer.

Ví dụ:
- Yoga.
- HIIT.
- Pilates.
- Strength.

## 7.2. Class Session Management

Một class có nhiều session.

### Session Fields
- Class Name.
- Trainer.
- Date.
- Start Time.
- End Time.
- Room.
- Capacity.
- Status.

### Status
```text
SCHEDULED
CANCELLED
COMPLETED
```

## 7.3. Schedule Management

### Manager
- View all sessions.
- Filter date.
- Filter trainer.
- Filter room.

### Trainer
- View own schedule.

### Member
- View available sessions.

## 7.4. Booking Management

### Member
- View Classes.
- View Availability.
- Book.
- View Booking.
- Cancel.
- View History.

### Receptionist
- Book on behalf of Customer.
- Cancel Booking.

### Booking Status
```text
PENDING
CONFIRMED
CANCELLED
COMPLETED
NO_SHOW
```

MVP tạo Booking ở `CONFIRMED` khi còn chỗ và Customer đủ quyền; `PENDING` chỉ dùng nếu bổ sung luồng chờ xác nhận sau MVP. `CONFIRMED` giữ chỗ cho đến khi Session kết thúc; check-in tạo Attendance nhưng không đổi ngay Booking thành `COMPLETED`. Sau Session, Booking có Attendance chuyển `COMPLETED`, Booking vắng mặt chuyển `NO_SHOW`. Hủy `CONFIRMED` trước Session trả lại một chỗ; `COMPLETED` và `NO_SHOW` không trả chỗ. Khi Class Session bị hủy, mọi Booking còn hiệu lực chuyển `CANCELLED` và Customer được thông báo.

## 7.5. Capacity Management

Theo dõi:
```text
Capacity
Booked
Remaining
```

Ví dụ:
```text
Capacity = 20
Booked = 15
Remaining = 5
```

Không được vượt Capacity.

Trước khi Session kết thúc, `Booked` là số Booking `CONFIRMED` của Session; `Remaining = Capacity - Booked`. Kiểm tra và tăng số chỗ phải cùng một giao dịch để không vượt Capacity khi nhiều người đặt đồng thời. Sau Session, báo cáo số chỗ đã đặt tính cả Booking `COMPLETED` và `NO_SHOW`.

## 7.6. Duplicate Booking Prevention

Một Customer không được book cùng một Class Session hai lần.

Quy tắc áp dụng cho Booking đang `CONFIRMED`; sau khi hủy, Customer được đặt lại nếu còn chỗ và vẫn đủ quyền. Không cho đặt Session đã hủy, đã hoàn thành hoặc đã qua giờ bắt đầu. Điều kiện hủy muộn do Studio cấu hình; MVP mặc định cho hủy trước giờ bắt đầu, sau giờ bắt đầu phải do Staff xử lý.

## 7.7. Concurrency Handling

Nếu còn 1 slot:
- Customer A và B cùng booking.
- Transaction/locking đảm bảo chỉ một request thành công.

Request còn lại nhận:
```text
CLASS_FULL
```

Đây là business rule và Software Engineering test case quan trọng.

---

# 8. CORE FUNCTION 5 — TRAINER & PERSONAL TRAINING

## 8.1. Trainer Management

### Manager
- Create Trainer.
- Edit Trainer.
- Activate/Deactivate Trainer.
- View Specialization.
- View Assigned Clients.
- View Schedule.

Không triển khai full HRM.

## 8.2. Trainer Assignment

Manager:
```text
Customer
 ↓
Assign Trainer
```

Trainer chỉ được xem Assigned Clients.

## 8.3. Trainer Schedule

Hiển thị:
- Class.
- PT Session.
- Customer.
- Time.
- Room.

## 8.4. PT Session Management

### Chức năng
- Create PT Session.
- Schedule PT.
- Reschedule.
- Cancel.
- Start.
- Complete.
- Add Training Notes.

Khi Complete:
```text
PT Package Remaining Sessions -= 1
```

Phải idempotent để không trừ hai lần.

PT Session chỉ được đặt khi Trainer còn lịch trống, Customer có PT Package còn hiệu lực và còn lượt chưa được giữ chỗ. Số lượt khả dụng = `remaining_sessions - số PT Session SCHEDULED`; đặt lịch và giữ lượt phải cùng giao dịch để không nhận quá số lượt khi có request đồng thời. `SCHEDULED → COMPLETED` trừ đúng một lượt trong cùng giao dịch; `SCHEDULED → CANCELLED` giải phóng lượt giữ chỗ và không trừ lượt. `COMPLETED` không được hoàn thành lại với mã tham chiếu khác. Buổi `NO_SHOW` và hủy muộn do Manager quyết định có trừ lượt hay không, quyết định phải được ghi Audit Log.

## 8.5. Trainer Client Management

Trainer xem:
- Customer profile.
- Goal.
- Membership.
- Booking.
- Workout history.
- Progress.
- Attendance.

---

# 9. CORE FUNCTION 6 — WORKOUT, FITNESS DATA & PROGRESS

## 9.1. Exercise Library

### Fields
- Name.
- Muscle Group.
- Equipment.
- Difficulty.
- Instructions.

### Chức năng
- Create.
- Edit.
- View.
- Search.
- Filter.

## 9.2. Workout Plan

### Trainer
- Create Plan.
- Edit Plan.
- Activate Plan.
- Complete Plan.
- Cancel Plan.
- Add Notes.

### Fields
- Customer.
- Trainer.
- Goal.
- Start Date.
- End Date.
- Status.

### Status
```text
DRAFT
ACTIVE
COMPLETED
CANCELLED
```

## 9.3. Workout Plan Exercises

Mỗi Exercise Entry:
- Day Number.
- Exercise.
- Sets.
- Reps.
- Target Weight.
- Rest Seconds.

## 9.4. Workout Execution

Member:
- Start Workout.
- View Exercises.
- Enter Actual Sets.
- Enter Reps.
- Enter Weight.
- Enter RPE.
- Add Notes.
- Complete Workout.

## 9.5. Workout Logging

Lưu:
- Customer.
- Workout Plan.
- Exercise.
- Performed At.
- Sets Completed.
- Reps Completed.
- Weight.
- RPE.
- Notes.

## 9.6. Workout History

### Chức năng
- View history.
- Filter by date.
- View session detail.
- Compare performance.

## 9.7. Goal Management

Goal:
```text
MUSCLE_GAIN
WEIGHT_LOSS
STRENGTH
GENERAL_FITNESS
MOBILITY
OTHER
```

Goal data:
- Current value.
- Target value.
- Unit.
- Target date.
- Status.

Goal có mục tiêu định lượng là tính năng sau MVP; MVP chỉ lưu Fitness Goal dạng danh mục trên Customer để phục vụ giáo án và AI.

## 9.8. Body Metrics

Tính năng sau MVP:
- Weight.
- Body Fat.
- Muscle Mass.
- Waist.

Theo dõi theo thời gian để tạo Progress Chart.

## 9.9. Progress Tracking

Dashboard:
- Workouts Completed.
- Attendance.
- Current Weight.
- Goal.
- Strength Progress.
- Workout Frequency.

Không hiển thị metrics nếu chưa có dữ liệu.

MVP Progress tính từ Workout Log và Attendance thật. Khi chưa có dữ liệu, trả về trạng thái rỗng; không tự tạo số liệu mẫu. Biểu đồ cân nặng chỉ bật khi tính năng Body Metrics được triển khai.

---

# 10. CORE FUNCTION 7 — AI COACHING

## 10.1. AI Context Management

AI được phép dùng authorized context:

```text
Customer Profile
Goal
Current Workout Plan
Workout History
Recent Progress
```

Không được truy cập customer khác.

Backend xác định Customer và Studio từ tài khoản đã xác thực, chỉ gửi cho AI dữ liệu cần thiết cho câu hỏi hiện tại. Client không được tự khai Customer ID, chấn thương hoặc số đo để thay thế dữ liệu hồ sơ đã lưu. Lịch sử hội thoại thuộc Customer và Studio tương ứng.

## 10.2. AI Fitness Assistant

User hỏi:
- “Hôm nay tôi nên tập gì?”
- “Tóm tắt tiến độ của tôi.”
- “Tôi chỉ có 30 phút, hãy điều chỉnh workout.”
- “Giải thích bài tập này.”

AI trả lời dựa trên context.

## 10.3. AI Workout Recommendation

AI có thể đề xuất:
- Exercise.
- Sets.
- Reps.
- Target weight.
- Volume.
- Intensity.
- Workout duration.
- Exercise substitution.

Recommendation phải có:
- Title.
- Reason.
- Suggested Changes.
- Expected Result/Duration nếu phù hợp.

## 10.4. Recommendation Approval

```text
AI Recommendation
        ↓
Member Review
        ↓
ACCEPT / DISMISS
        ↓
Trainer Review (nếu ACCEPT)
        ↓
Backend Validation
        ↓
Apply Change
```

AI không được tự ý mutate workout plan.

MVP: AI chỉ đề xuất thay đổi. Customer có thể lưu gợi ý để Trainer xem; chỉ Trainer được phân công hoặc Manager được áp dụng thay đổi vào giáo án đang hoạt động. Mọi thay đổi phải qua kiểm tra quyền, tính hợp lệ của bài tập và ghi lịch sử phiên bản giáo án.

## 10.5. AI Progress Summary

AI tổng hợp:
- Training frequency.
- Attendance.
- Workout trends.
- Performance progression.

## 10.6. AI Conversation

### Chức năng
- Start Conversation.
- Send Message.
- Receive Response.
- Continue Conversation.
- View History.

## 10.7. AI Safety

Không:
- Medical diagnosis.
- Prescription.
- Treatment.
- Medical claims.

Nếu phát hiện nội dung sức khỏe nghiêm trọng:
→ hướng user tới chuyên gia phù hợp.

---

# 11. SUPPORT FUNCTION 8 — PAYMENT & TRANSACTION MANAGEMENT

## 11.1. Payment Creation

Fields:
- Order.
- Method.
- Reference.
- Amount.
- Status.
- Paid At.

Trạng thái Payment trong MVP: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`. Mỗi lần thử thanh toán là một Payment riêng; `reference` duy nhất trong phạm vi nhà cung cấp/phương thức. Số tiền thu hợp lệ của Order phải bằng `total` trước khi Order thành `PAID` và kích hoạt dịch vụ. MVP không hỗ trợ thanh toán một phần. Khi hoàn tiền, ghi riêng số tiền và thời điểm hoàn để báo cáo vẫn nhận biết được khoản đã thu ban đầu.

## 11.2. Payment Methods

MVP:
- Cash.
- Bank Transfer.
- Sandbox payment.

Cash và Bank Transfer cần Staff có quyền xác nhận thủ công, kèm người xác nhận và thời điểm. Sandbox payment chỉ thành công sau khi backend xác minh callback hoặc kết quả từ nhà cung cấp; kết quả do trình duyệt gửi không đủ để kích hoạt dịch vụ.

Optional:
- VNPay Sandbox.
- Stripe Test.

## 11.3. Payment Processing

Flow:
```text
Order Created
 ↓
Payment Pending
 ↓
Payment Success
 ↓
Service Activation
```

## 11.4. Payment Failure

- Show failure.
- Do not activate service.
- Allow retry.

## 11.5. Payment Idempotency

Duplicate callback không được tạo duplicate business effect.

Callback phải xác minh nguồn gửi, số tiền, Order và mã giao dịch; cùng một `reference` chỉ được ghi nhận một lần. Callback lặp trả về kết quả đã xử lý, không tạo Payment, Membership hoặc PT Package mới.

## 11.6. Payment History

Member:
- Xem own payments.

Manager:
- Xem business payments.

---

# 12. SUPPORT FUNCTION 9 — CHECK-IN & ATTENDANCE

## 12.1. Check-in

Reception:
- Search customer.
- Verify membership.
- Verify booking nếu cần.
- Check-in.

Check-in lớp yêu cầu Booking `CONFIRMED`, đúng Customer và đúng Session; check-in khu vực tập yêu cầu Membership còn hiệu lực và có quyền vào khu vực đó. Mỗi Booking chỉ có một Attendance. MVP cho check-in từ 30 phút trước đến 15 phút sau giờ bắt đầu; ngoại lệ cần Staff có quyền ghi lý do. Check-in không tự gia hạn Membership.

## 12.2. Attendance

Ghi:
- Customer.
- Date/time.
- Class/PT/session.
- Status.

## 12.3. Attendance History

Member:
- Xem own history.

Trainer:
- Xem assigned customer.

Manager:
- Xem aggregated attendance.

## 12.4. QR Check-in

Optional:
- Generate QR/member code.
- Scan.
- Validate.
- Create attendance.

Không bắt buộc MVP.

---

# 13. SUPPORT FUNCTION 10 — BUSINESS DASHBOARD & REPORTING

## 13.1. Manager Dashboard

KPI:
- Revenue.
- Active Members.
- New Leads.
- Conversion Rate.
- Class Occupancy.

Tất cả KPI mặc định chỉ tính dữ liệu Studio hiện tại theo múi giờ Studio, hiển thị kỳ lọc và thời điểm cập nhật. Định nghĩa MVP:

- `Revenue`: tổng tiền được ghi nhận đã thu trong kỳ, trừ số tiền hoàn thực tế trong kỳ; vẫn tính khoản thu ban đầu kể cả khi Payment sau đó chuyển `REFUNDED`. Không lấy tổng Order chưa thanh toán.
- `Active Members`: số Customer duy nhất có Membership `ACTIVE`, trong thời hạn và chưa bị thu hồi tại thời điểm xem.
- `New Leads`: số Lead được tạo trong kỳ.
- `Conversion Rate`: số Lead tạo trong kỳ đã `CONVERTED` chia số Lead tạo trong kỳ; hiển thị `0%` nếu mẫu số bằng 0.
- `Class Occupancy`: tổng Booking `CONFIRMED`, `COMPLETED` và `NO_SHOW` của các Session trong kỳ chia tổng sức chứa các Session không bị hủy trong kỳ; hiển thị `0%` nếu sức chứa bằng 0.
- `MRR` chỉ hiển thị nếu có sản phẩm thu phí định kỳ và quy tắc quy đổi doanh thu tháng; không dùng toàn bộ tiền thu trong tháng để gọi là MRR.

## 13.2. CRM Report

- New Leads.
- Contacted.
- Interested.
- Trial.
- Converted.
- Lost.

## 13.3. Sales Report

- Order count.
- Revenue.
- Average order value nếu có.
- Popular plans.
- Payment status.

## 13.4. Membership Report

- Active.
- Expiring.
- Expired.
- Renewed.

## 13.5. Class Report

- Capacity.
- Bookings.
- Occupancy.
- Attendance.
- No-show.

## 13.6. Renewal Report

- Customer.
- Expiration.
- Days remaining.
- Contact status.
- Renewal status.

---

# 14. SUPPORT FUNCTION 11 — IDENTITY, SECURITY & SYSTEM ADMINISTRATION

## 14.1. Authentication

- Register.
- Login.
- Logout.
- Forgot Password.
- Reset Password.
- Change Password.

## 14.2. Authorization / RBAC

Roles:
```text
ADMIN
MANAGER
RECEPTIONIST
TRAINER
MEMBER
PLATFORM_ADMIN
```

Backend bắt buộc kiểm tra authorization.

Frontend chỉ dùng để ẩn/hiện UI.

`MEMBER` là vai trò tài khoản dành cho Customer tại một Studio, không chứng minh Membership đang hiệu lực. `ADMIN` quản trị Studio được cấp quyền; `PLATFORM_ADMIN` tạo Studio và cấp quyền quản trị Studio. Một User có thể có vai trò khác nhau ở các Studio. Mỗi request nghiệp vụ phải kiểm tra cả vai trò ở Studio đang chọn và quyền truy cập đúng Customer/Trainer được phân công.

## 14.3. User Management

Admin:
- Create.
- Edit.
- Disable.
- Reset Password.
- Change Role.

## 14.4. Profile Management

- View Profile.
- Edit Profile.
- Change Password.
- Logout.

## 14.5. Audit Log

Ghi các hành động quan trọng:

```text
Actor
Action
Entity Type
Entity ID
Timestamp
Metadata
```

Ví dụ:
- Create Lead.
- Convert Lead.
- Create Order.
- Confirm Payment.
- Cancel Membership.
- Change Role.

---

# 15. CROSS-CUTTING BUSINESS RULES

## Authentication
- Email user phải unique.
- Password phải hash.
- Disabled user không được login.

## CRM
- Name + Phone là required.
- Converted Lead không convert lần hai.
- Không mất Lead history.

## Membership
- Không active trước payment hợp lệ.
- Expired Membership không được booking.

## Booking
- Không duplicate.
- Không vượt capacity.
- Concurrency phải an toàn.

## PT
- Không dùng khi remaining sessions = 0.
- Complete session chỉ decrement đúng một lần.

## Order
- Total server-side calculation.
- Total không âm.

## Payment
- Transaction reference unique.
- Callback idempotent.

## AI
- Chỉ được đọc authorized customer context.
- Output phải được validate trước khi mutate dữ liệu.
- Không medical diagnosis/treatment.

---

# 16. CORE VS SUPPORTING FUNCTIONS

## Core — bản chất FITOS

```text
1. Customer & CRM
        ↓
2. Service & Sales
        ↓
3. Membership
        ↓
4. Booking & Operations
        ↓
5. Trainer / PT
        ↓
6. Workout / Progress
        ↓
7. AI Coaching
```

## Supporting

```text
8. Payment
9. Check-in / Attendance
10. Dashboard / Reports
11. Identity / Administration
```

---

# 17. MVP BẮT BUỘC

### Foundation
- Authentication.
- RBAC.
- User/Profile.

### CRM
- Visitor/Lead.
- Lead Activity.
- Consultation/Trial.
- Lead Conversion.
- Customer.

### Commerce
- Plans.
- Order.
- Payment.
- Membership.

### Operations
- Class.
- Schedule.
- Booking.
- Check-in.

### Training
- Trainer.
- Client Assignment.
- Workout Plan.
- Workout Log.
- Progress.

### AI
- AI Chat.
- AI Recommendation.
- AI Progress Summary.

### Business
- Manager Dashboard.
- Reports.
- Renewal.

### Quy tắc nghiệm thu MVP

- Một Studio không thể đọc hoặc sửa dữ liệu của Studio khác qua API, kể cả khi biết ID bản ghi.
- Customer có thể hoàn thành luồng Order → Payment hợp lệ → Membership/PT Package → sử dụng đúng dịch vụ được mua.
- Booking và PT không vượt sức chứa/số lượt khi có request đồng thời; callback Payment và hoàn thành PT lặp không tạo tác động trùng.
- Mọi số liệu Dashboard/Progress có nguồn dữ liệu thật; trường hợp chưa có dữ liệu hiển thị trạng thái rỗng.

---

# 18. STRETCH FEATURES

Chỉ làm sau khi MVP hoàn chỉnh:

- QR Check-in.
- Notifications.
- RAG.
- Body Metrics.
- Customer Goals.
- Membership Pause / Resume.
- Churn Risk.
- AI Progress Summary nâng cao.
- Multi-branch.
- Waitlist.
- Calendar integration.

---

# 19. NGOÀI SCOPE

Không triển khai trong đồ án:

- Full ERP.
- Full HRM.
- Payroll.
- Recruitment.
- Wearables.
- Apple Health.
- Google Fit.
- IoT equipment.
- Face Recognition.
- Computer Vision.
- Real-time Heart Rate.
- Medical Diagnosis.
- Advanced ML.
- Tách CRM, Commerce và Operations thành các microservice riêng; dịch vụ AI độc lập được phép dùng.
- Kubernetes.

---

# 20. TOÀN BỘ CHỨC NĂNG DẠNG TREE

```text
FITOS
│
├── 1. CUSTOMER & CRM
│   ├── Visitor / Walk-in
│   ├── Lead
│   │   ├── Create
│   │   ├── View
│   │   ├── Edit
│   │   ├── Search
│   │   ├── Filter
│   │   ├── Assign
│   │   ├── Status
│   │   ├── Convert
│   │   └── Lost
│   ├── Lead Activity
│   ├── Consultation
│   ├── Trial
│   ├── Customer
│   └── Renewal / Retention
│
├── 2. SERVICE & SALES
│   ├── Membership Plans
│   ├── PT Packages
│   ├── Service Consultation
│   ├── Order
│   │   ├── Create
│   │   ├── Calculate
│   │   ├── Discount
│   │   ├── View
│   │   └── Cancel
│   └── Service Activation
│
├── 3. MEMBERSHIP
│   ├── Create
│   ├── Activate
│   ├── View
│   ├── Pause [Sau MVP]
│   ├── Resume [Sau MVP]
│   ├── Renew
│   ├── Cancel
│   ├── Expiration
│   └── Access Validation
│
├── 4. CLASS / BOOKING
│   ├── Class
│   ├── Session
│   ├── Schedule
│   ├── Booking
│   ├── Cancellation
│   ├── Capacity
│   ├── Duplicate Prevention
│   └── Concurrency
│
├── 5. TRAINER / PT
│   ├── Trainer
│   ├── Assignment
│   ├── Schedule
│   ├── PT Session
│   ├── Completion
│   └── Training Notes
│
├── 6. WORKOUT / PROGRESS
│   ├── Exercise Library
│   ├── Workout Plan
│   ├── Workout Plan Exercises
│   ├── Workout Execution
│   ├── Workout Log
│   ├── Workout History
│   ├── Goals [Định lượng sau MVP]
│   ├── Body Metrics [Sau MVP]
│   └── Progress
│
├── 7. AI COACH
│   ├── Context
│   ├── AI Assistant
│   ├── Workout Recommendation
│   ├── Progress Summary
│   ├── Chat
│   ├── Accept / Dismiss
│   ├── Validation
│   └── Safety
│
├── 8. PAYMENT
│   ├── Create
│   ├── Process
│   ├── Success
│   ├── Failed
│   ├── Refund
│   ├── History
│   └── Idempotency
│
├── 9. CHECK-IN / ATTENDANCE
│   ├── Check-in
│   ├── Verify Membership
│   ├── Verify Booking
│   ├── Attendance
│   ├── History
│   └── QR Check-in [Optional]
│
├── 10. BUSINESS MANAGEMENT
│   ├── Dashboard
│   ├── CRM Report
│   ├── Sales Report
│   ├── Membership Report
│   ├── Class Report
│   └── Renewal Report
│
└── 11. ADMINISTRATION
    ├── Authentication
    ├── RBAC
    ├── Users
    ├── Roles
    ├── Profile
    ├── Settings
    └── Audit Log
```

---

# 21. FUNCTION COMPLETION CRITERIA

Một chức năng chỉ được coi là hoàn thành khi có đầy đủ:

```text
UI
 ↓
API
 ↓
Business Logic
 ↓
Database
 ↓
Validation
 ↓
Authorization
 ↓
Loading / Error / Empty / Success
 ↓
Test
 ↓
Deployment
```

Không coi một màn hình UI đẹp là chức năng hoàn chỉnh.

---

# 22. FINAL SYSTEM DEFINITION

FITOS không đơn giản là “Gym Management System”. Bản chất đầy đủ của nó là:

> **Customer Lifecycle Management + Fitness Service Commerce + Fitness Operations + Personal Training + Workout/Progress Management + AI Coaching.**

Toàn bộ hệ thống phải nhất quán từ:

```text
Visitor
  ↓
Lead
  ↓
Customer
  ↓
Order
  ↓
Payment
  ↓
Membership / PT Package
  ↓
Member sử dụng dịch vụ
  ↓
Booking / PT
  ↓
Training
  ↓
Workout
  ↓
Progress
  ↓
AI Coaching
  ↓
Renewal / Retention
```

Mục tiêu cuối cùng là xây một sản phẩm software hoàn chỉnh ở mức intern → junior, có nghiệp vụ doanh nghiệp thật, trải nghiệm người dùng rõ ràng, architecture có chủ đích, AI có giá trị, database nhất quán và có thể triển khai cloud thực tế.
