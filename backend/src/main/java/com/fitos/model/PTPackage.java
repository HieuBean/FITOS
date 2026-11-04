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
public class PTPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions;

    @Column(name = "remaining_sessions", nullable = false)
    private Integer remainingSessions;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, EXHAUSTED, EXPIRED

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Version
    private Long version;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

