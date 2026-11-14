package com.fitos.customer.dto;

import com.fitos.model.Customer;
import com.fitos.model.Membership;
import com.fitos.model.PtPackage;
import com.fitos.model.Order;
import com.fitos.model.Payment;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    // ------------------------------------------------------------ list (#30)

    public record MembershipBrief(Long id, Long planId, String planName, String status,
                                  LocalDate startDate, LocalDate endDate, long daysRemaining) {
        public static MembershipBrief from(Membership m, String planName, LocalDate today) {
            return new MembershipBrief(m.getId(), m.getPlanId(), planName, m.getStatus(), m.getStartDate(),
                    m.getEndDate(), m.daysRemaining(today));
        }
    }

    public record CustomerListItem(Long id, String fullName, String email, String phone, String fitnessGoal,
                                   OffsetDateTime createdAt, MembershipBrief membership) {
    }

    // ------------------------------------------------------------ detail (#31)

    public record CustomerProfile(Long id, Long userId, Long leadId, String fullName, String email, String phone,
                                  LocalDate dateOfBirth, String gender, String fitnessGoal, String notes,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        public static CustomerProfile from(Customer c) {
            OffsetDateTime cat = c.getCreatedAt() != null ? c.getCreatedAt().atOffset(ZoneOffset.UTC) : null;
            OffsetDateTime uat = c.getUpdatedAt() != null ? c.getUpdatedAt().atOffset(ZoneOffset.UTC) : null;
            return new CustomerProfile(c.getId(), c.getUserId(), c.getLeadId(), c.getFullName(), c.getEmail(),
                    c.getPhone(), c.getDateOfBirth(), c.getGender(), c.getFitnessGoal(), c.getNotes(),
                    cat, uat);
        }
    }

    public record PtPackageBrief(Long id, Long planId, String planName, String status,
                                 int totalSessions, int remainingSessions, LocalDate startDate,
                                 LocalDate expiryDate) {
        public static PtPackageBrief from(PtPackage p, String planName) {
            return new PtPackageBrief(p.getId(), p.getPlanId(), planName, p.getStatus(),
                    p.getTotalSessions() == null ? 0 : p.getTotalSessions(),
                    p.getRemainingSessions() == null ? 0 : p.getRemainingSessions(),
                    null, p.getExpiresAt());
        }
    }

    public record PaymentSummary(Long id, String transactionRef, BigDecimal amount, String paymentMethod,
                                 String status, Instant paidAt, String failureReason) {
        public static PaymentSummary from(Payment p) {
            return new PaymentSummary(p.getId(), p.getTransactionRef(), p.getAmount(),
                    p.getPaymentMethod(), p.getStatus(), p.getPaidAt(), null);
        }
    }

    public record OrderResponse(
            Long id,
            String orderReference,
            Long customerId,
            String customerName,
            String customerEmail,
            String status,
            BigDecimal totalAmount,
            Instant createdAt,
            List<PaymentSummary> payments
    ) {
        public static OrderResponse from(Order o, String customerName, String customerEmail,
                                         List<PaymentSummary> payments) {
            return new OrderResponse(o.getId(), o.getOrderReference(), o.getCustomerId(),
                    customerName, customerEmail, o.getStatus(), o.getTotalAmount(),
                    o.getCreatedAt(), payments);
        }
    }

    /** Booking lớp học (bảng của C/D). noShowProbability có thể null nếu chưa gọi AI. */
    public record BookingItem(Long id, String className, OffsetDateTime sessionStart, String status,
                              OffsetDateTime bookedAt, Double noShowProbability) {
    }

    public record WorkoutItem(Long id, String title, OffsetDateTime startedAt, OffsetDateTime completedAt,
                              String status) {
    }

    public record AttendanceItem(Long id, OffsetDateTime checkedInAt, String source) {
    }

    public record CustomerStats(BigDecimal totalSpent, long paidOrders, long totalBookings,
                                long visitsLast30Days, long workoutsLast30Days, OffsetDateTime lastVisitAt) {
    }

    /** Customer 360 — gộp 4 module: CRM, Commerce, Booking, Workout. */
    public record CustomerDetailResponse(
            CustomerProfile profile,
            MembershipBrief currentMembership,
            List<MembershipBrief> memberships,
            List<PtPackageBrief> ptPackages,
            List<OrderResponse> orders,
            List<BookingItem> bookings,
            List<WorkoutItem> workouts,
            List<AttendanceItem> attendance,
            CustomerStats stats
    ) {
    }

    // ------------------------------------------------------------ update (#32)

    public record UpdateCustomerRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Pattern(regexp = "^[0-9+()\\-\\s]{8,20}$", message = "Invalid phone number") String phone,
            @Past LocalDate dateOfBirth,
            @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "gender must be MALE, FEMALE or OTHER") String gender,
            @Size(max = 50) String fitnessGoal,
            @Size(max = 2000) String notes
    ) {
    }
}
