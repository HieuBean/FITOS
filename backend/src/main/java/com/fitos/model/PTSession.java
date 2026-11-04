package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pt_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PTSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pt_package_id")
    private PTPackage ptPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Builder.Default
    @Column(nullable = false)
    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED

    @Column(name = "completion_ref", unique = true)
    private String completionRef;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

