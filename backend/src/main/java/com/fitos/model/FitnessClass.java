package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    
    @Column(name = "class_type")
    private String type; // YOGA, HIIT, PILATES, SPINNING, BOXING

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

