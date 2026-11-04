package com.fitos.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "workout_plan_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutPlanExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id", nullable = false)
    @JsonBackReference
    private WorkoutPlan workoutPlan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1=Mon, ..., 7=Sun

    private Integer sets;
    private Integer reps;

    @Column(name = "duration_secs")
    private Integer durationSecs;

    @Builder.Default
    @Column(name = "rest_secs")
    private Integer restSecs = 60;

    @Column(name = "weight_suggest")
    private BigDecimal weightSuggest;

    private String notes;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}

