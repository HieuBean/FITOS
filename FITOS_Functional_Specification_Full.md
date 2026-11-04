# FITOS — FITNESS STUDIO MANAGEMENT & AI COACHING PLATFORM

> **Functional Specification — Full Function Tree**
>
> Tài liệu này mô tả toàn bộ chức năng nghiệp vụ của FITOS theo cấu trúc: **Chức năng chính → chức năng phụ → mô tả → actor → business rules**.
>
> FITOS là nền tảng B2B2C cho Fitness Studio/Gym: doanh nghiệp là khách hàng của FITOS; Manager/Owner, Receptionist và Trainer vận hành hệ thống; Member là người dùng cuối. Quy trình trung tâm là **Visitor → Lead → Customer → Member → Purchase → Payment → Membership → Booking/PT → Workout → Progress → AI → Renewal/Upsell**.

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

Khách đã chuyển đổi và có tài khoản.

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
     PURCHASE
        ↓
     PAYMENT
        ↓
MEMBERSHIP ACTIVE
        ↓
      MEMBER
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

## 4.6. Lead Conversion

### Flow

```text
LEAD
  ↓
CONVERT
  ↓
CUSTOMER PROFILE
  ↓
ACCOUNT
  ↓
ORDER
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
FAILED
CANCELLED
REFUNDED
```

### Rule
Total phải được tính và validate ở backend.

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

## 6.2. Membership Lifecycle

```text
PENDING
 ↓
ACTIVE
 ↓
PAUSED
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
PAUSED
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
- Update membership.
- Preserve history.

## 6.5. Membership Pause / Resume

Nếu business policy hỗ trợ:
- Pause.
- Resume.
- Validate allowed period.

## 6.6. Membership Access Validation

Trước các operation cần membership:

```text
Is Membership Active?
Is Membership Within Valid Date?
Is Customer Eligible?
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

## 7.6. Duplicate Booking Prevention

Một Customer không được book cùng một Class Session hai lần.

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

## 9.8. Body Metrics

Nếu triển khai:
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
User Review
        ↓
ACCEPT / DISMISS
        ↓
Backend Validation
        ↓
Apply Change
```

AI không được tự ý mutate workout plan.

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

## 11.2. Payment Methods

MVP:
- Cash.
- Bank Transfer.
- Sandbox payment.

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
CUSTOMER
```

Backend bắt buộc kiểm tra authorization.

Frontend chỉ dùng để ẩn/hiện UI.

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

---

# 18. STRETCH FEATURES

Chỉ làm sau khi MVP hoàn chỉnh:

- QR Check-in.
- Notifications.
- RAG.
- Body Metrics.
- Customer Goals.
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
- Microservices.
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
│   ├── Pause
│   ├── Resume
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
│   ├── Goals
│   ├── Body Metrics
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
Member
  ↓
Sales
  ↓
Payment
  ↓
Membership
  ↓
Booking
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
