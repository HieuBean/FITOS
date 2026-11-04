package com.fitos.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id", nullable = false)
    @JsonBackReference
    private WorkoutSession workoutSession;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @Column(name = "reps_actual")
    private Integer repsActual;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "duration_secs")
    private Integer durationSecs;

    private Integer rpe; // 1 - 10
    private String notes;

    @Builder.Default
    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt = LocalDateTime.now();
}

