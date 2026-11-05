package com.fitos.customer.dto;

import com.fitos.customer.Customer;
import com.fitos.membership.Membership;
import com.fitos.membership.PtPackage;
import com.fitos.order.dto.OrderDtos.OrderResponse;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    // ------------------------------------------------------------ list (#30)

    public record MembershipBrief(Long id, Long planId, String planName, Membership.Status status,
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
            return new CustomerProfile(c.getId(), c.getUserId(), c.getLeadId(), c.getFullName(), c.getEmail(),
                    c.getPhone(), c.getDateOfBirth(), c.getGender(), c.getFitnessGoal(), c.getNotes(),
                    c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    public record PtPackageBrief(Long id, Long planId, String planName, PtPackage.Status status,
                                 int totalSessions, int remainingSessions, LocalDate startDate,
                                 LocalDate expiryDate) {
        public static PtPackageBrief from(PtPackage p, String planName) {
            return new PtPackageBrief(p.getId(), p.getPlanId(), planName, p.getStatus(), p.getTotalSessions(),
                    p.getRemainingSessions(), p.getStartDate(), p.getExpiryDate());
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
