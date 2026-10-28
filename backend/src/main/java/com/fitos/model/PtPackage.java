package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions;

    @Column(name = "remaining_sessions", nullable = false)
    private Integer remainingSessions;

    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, DEPLETED

    @Version
    private Long version;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

}
