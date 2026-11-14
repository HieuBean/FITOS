package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pt_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PtPackage {

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

    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions;

    @Column(name = "remaining_sessions", nullable = false)
    private Integer remainingSessions;

    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, DEPLETED, EXPIRED, CANCELLED

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Version
    private Long version;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status {
        ACTIVE, COMPLETED, DEPLETED, EXPIRED, CANCELLED
    }

    public Status getStatusEnum() {
        try {
            return status != null ? Status.valueOf(status) : Status.ACTIVE;
        } catch (Exception e) {
            return Status.ACTIVE;
        }
    }

    public LocalDate getExpiryDate() {
        return expiresAt;
    }

    public void setExpiryDate(LocalDate date) {
        this.expiresAt = date;
    }

    public static PtPackage activate(Long customerId, ServicePlan plan, Long orderId, LocalDate startDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        int days = plan.getDurationDays() != null ? plan.getDurationDays() : 60;
        int sessions = plan.getTotalSessions() != null ? plan.getTotalSessions() : 10;
        return PtPackage.builder()
                .studioId(plan.getStudioId() != null ? plan.getStudioId() : 1L)
                .customerId(customerId)
                .planId(plan.getId())
                .orderId(orderId)
                .totalSessions(sessions)
                .remainingSessions(sessions)
                .status("ACTIVE")
                .expiresAt(start.plusDays(days))
                .build();
    }

    public void useOneSession(LocalDate today) {
        if (remainingSessions == null || remainingSessions <= 0) {
            throw new IllegalStateException("No sessions remaining");
        }
        this.remainingSessions = this.remainingSessions - 1;
        if (this.remainingSessions == 0) {
            this.status = "DEPLETED";
        }
    }
}
