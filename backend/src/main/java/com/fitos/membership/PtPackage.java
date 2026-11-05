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

/**
 * Bảng pt_packages (V2_4 — Người B).
 * remaining_sessions bị trừ bởi PATCH /pt-packages/:id/use (D — #44) với @Version.
 */
@Entity
@Table(name = "pt_packages")
@Getter
@Setter
@NoArgsConstructor
public class PtPackage {

    public enum Status { ACTIVE, DEPLETED, EXPIRED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions;

    @Column(name = "remaining_sessions", nullable = false)
    private Integer remainingSessions;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Factory cho ServiceActivationService (C — #40). */
    public static PtPackage activate(Long customerId, ServicePlan plan, Long orderId, LocalDate startDate) {
        if (!plan.isPtPackage() || plan.getTotalSessions() == null) {
            throw new IllegalArgumentException("Plan " + plan.getId() + " is not a PT package");
        }
        PtPackage p = new PtPackage();
        p.customerId = customerId;
        p.planId = plan.getId();
        p.orderId = orderId;
        p.totalSessions = plan.getTotalSessions();
        p.remainingSessions = plan.getTotalSessions();
        p.startDate = startDate;
        p.expiryDate = startDate.plusDays(plan.getDurationDays() - 1L);
        p.status = Status.ACTIVE;
        return p;
    }

    /** Trừ 1 buổi; hết buổi → DEPLETED. */
    public void useOneSession(LocalDate today) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("PT package is " + status);
        }
        if (today.isAfter(expiryDate)) {
            throw new IllegalStateException("PT package expired on " + expiryDate);
        }
        if (remainingSessions <= 0) {
            throw new IllegalStateException("No remaining sessions");
        }
        remainingSessions--;
        if (remainingSessions == 0) {
            status = Status.DEPLETED;
        }
    }
}
