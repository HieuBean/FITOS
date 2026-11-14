package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "paused_at")
    private LocalDate pausedAt;

    @Builder.Default
    @Column(name = "pause_days_used", nullable = false)
    private Integer pauseDaysUsed = 0;

    @Builder.Default
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Status {
        PENDING, ACTIVE, PAUSED, EXPIRED, CANCELLED
    }

    public Status getStatusEnum() {
        try {
            return status != null ? Status.valueOf(status) : Status.ACTIVE;
        } catch (Exception e) {
            return Status.ACTIVE;
        }
    }

    /**
     * Số ngày còn lại, tính cả ngày hôm nay.
     *
     * <p>Gói 30 ngày bắt đầu 01/10 thì kết thúc 30/10; hôm 01/10 phải báo "còn 30 ngày"
     * và hôm 30/10 (ngày cuối) báo "còn 1 ngày". Trước đây hàm trả về hiệu số thuần
     * nên ngày cuối cùng hiển thị 0 ngày, khiến hội viên tưởng gói đã hết hạn.
     */
    public long daysRemaining(LocalDate today) {
        if (endDate == null || today == null) return 0;
        long diff = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
        return diff < 0 ? 0 : diff + 1;
    }

    public static Membership activate(Long customerId, ServicePlan plan, Long orderId, LocalDate startDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        int days = plan.getDurationDays() != null ? plan.getDurationDays() : 30;
        LocalDate end = start.plusDays(days - 1);
        return Membership.builder()
                .studioId(plan.getStudioId() != null ? plan.getStudioId() : 1L)
                .customerId(customerId)
                .planId(plan.getId())
                .orderId(orderId)
                .startDate(start)
                .endDate(end)
                .status("ACTIVE")
                .pauseDaysUsed(0)
                .autoRenew(false)
                .build();
    }

    public void pause(LocalDate pauseDate) {
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Only ACTIVE membership can be paused");
        }
        this.status = "PAUSED";
        this.pausedAt = pauseDate;
    }

    public long resume(LocalDate resumeDate) {
        if (!"PAUSED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Only PAUSED membership can be resumed");
        }
        if (pausedAt != null && resumeDate != null && resumeDate.isBefore(pausedAt)) {
            throw new IllegalArgumentException("Resume date cannot be before pause date");
        }
        long days = 0;
        if (pausedAt != null && resumeDate != null) {
            days = java.time.temporal.ChronoUnit.DAYS.between(pausedAt, resumeDate);
            this.endDate = this.endDate.plusDays(days);
            this.pauseDaysUsed = (this.pauseDaysUsed != null ? this.pauseDaysUsed : 0) + (int) days;
        }
        this.status = "ACTIVE";
        this.pausedAt = null;
        return days;
    }

    public boolean isActiveOn(LocalDate date) {
        if (!"ACTIVE".equalsIgnoreCase(status) || startDate == null || endDate == null || date == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public Integer getTotalPausedDays() {
        return pauseDaysUsed != null ? pauseDaysUsed : 0;
    }

    public void setTotalPausedDays(Integer days) {
        this.pauseDaysUsed = days;
    }
}
