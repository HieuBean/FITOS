package com.fitos.membership;

import com.fitos.plan.ServicePlan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Bảng memberships (V2_4 — Người B).
 * Logic tính ngày khi pause/resume nằm ngay trong entity để
 * MembershipService (Người C — #42) gọi lại và test #144 kiểm tra được.
 */
@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
public class Membership {

    public enum Status { ACTIVE, PAUSED, EXPIRED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "paused_at")
    private LocalDate pausedAt;

    @Column(name = "total_paused_days", nullable = false)
    private int totalPausedDays = 0;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Factory cho ServiceActivationService (C — #40).
     * end_date = start + duration_days - 1 (gói 30 ngày dùng đủ 30 ngày kể cả ngày bắt đầu).
     */
    public static Membership activate(Long customerId, ServicePlan plan, Long orderId, LocalDate startDate) {
        Membership m = new Membership();
        m.customerId = customerId;
        m.planId = plan.getId();
        m.orderId = orderId;
        m.startDate = startDate;
        m.endDate = startDate.plusDays(plan.getDurationDays() - 1L);
        m.status = Status.ACTIVE;
        return m;
    }

    public boolean isActiveOn(LocalDate date) {
        return status == Status.ACTIVE && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** Tạm dừng: ghi nhận ngày bắt đầu dừng. */
    public void pause(LocalDate today) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE memberships can be paused (current: " + status + ")");
        }
        if (today.isAfter(endDate)) {
            throw new IllegalStateException("Membership already ended on " + endDate);
        }
        this.status = Status.PAUSED;
        this.pausedAt = today;
    }

    /**
     * Tiếp tục: số ngày đã dừng được cộng bù vào end_date.
     * Dừng và tiếp tục cùng ngày → 0 ngày bù.
     *
     * @return số ngày được cộng thêm
     */
    public long resume(LocalDate today) {
        if (status != Status.PAUSED || pausedAt == null) {
            throw new IllegalStateException("Only PAUSED memberships can be resumed (current: " + status + ")");
        }
        if (today.isBefore(pausedAt)) {
            throw new IllegalArgumentException("Resume date " + today + " is before pause date " + pausedAt);
        }
        long pausedDays = ChronoUnit.DAYS.between(pausedAt, today);
        this.endDate = this.endDate.plusDays(pausedDays);
        this.totalPausedDays += (int) pausedDays;
        this.pausedAt = null;
        this.status = Status.ACTIVE;
        return pausedDays;
    }

    public void cancel(OffsetDateTime now) {
        if (status == Status.CANCELLED || status == Status.EXPIRED) {
            throw new IllegalStateException("Membership is already " + status);
        }
        this.status = Status.CANCELLED;
        this.pausedAt = null;
        this.cancelledAt = now;
    }

    public long daysRemaining(LocalDate today) {
        if (status == Status.CANCELLED || status == Status.EXPIRED) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(today, endDate) + 1);
    }
}
