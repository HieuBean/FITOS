package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String service; // WORKOUT, CRM, etc.

    @Column(name = "rec_type", nullable = false)
    private String recType;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String recommendation; // JSON string

    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, ACCEPTED, DISMISSED

    @Column(name = "acted_at")
    private LocalDateTime actedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

