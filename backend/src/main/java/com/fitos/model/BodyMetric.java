package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "body_metrics", uniqueConstraints = {
    @UniqueConstraint(name = "uq_body_metrics_date", columnNames = {"customer_id", "recorded_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BodyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Builder.Default
    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate = LocalDate.now();

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "body_fat_percent", precision = 4, scale = 2)
    private BigDecimal bodyFatPercent;

    @Column(name = "muscle_mass_kg", precision = 5, scale = 2)
    private BigDecimal muscleMassKg;

    @Column(length = 255)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
