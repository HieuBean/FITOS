package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "body_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BodyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "body_fat_pct")
    private BigDecimal bodyFatPct;

    @Column(name = "muscle_mass_kg")
    private BigDecimal muscleMassKg;

    private BigDecimal bmi;

    @Column(name = "waist_cm")
    private BigDecimal waistCm;

    @Column(name = "chest_cm")
    private BigDecimal chestCm;

    @Builder.Default
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    private String notes;
}

