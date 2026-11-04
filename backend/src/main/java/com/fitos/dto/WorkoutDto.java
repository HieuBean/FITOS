package com.fitos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class WorkoutDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatePlanRequest {
        @NotNull(message = "Customer ID is required")
        private Long customerId;
        private Long trainerId;
        @NotBlank(message = "Plan name is required")
        private String name;
        private String description;
        private String goal;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<PlanExerciseItem> exercises;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdatePlanRequest {
        private String name;
        private String description;
        private String goal;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<PlanExerciseItem> exercises;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanExerciseItem {
        @NotNull(message = "Exercise ID is required")
        private Long exerciseId;
        @NotNull(message = "Day of week is required")
        private Integer dayOfWeek;
        private Integer sets;
        private Integer reps;
        private Integer durationSecs;
        private Integer restSecs;
        private BigDecimal weightSuggest;
        private String notes;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanResponse {
        private Long id;
        private Long customerId;
        private Long trainerId;
        private String name;
        private String description;
        private String goal;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<PlanExerciseDetail> exercises;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanExerciseDetail {
        private Long id;
        private Long exerciseId;
        private String exerciseName;
        private String muscleGroup;
        private Integer dayOfWeek;
        private Integer sets;
        private Integer reps;
        private Integer durationSecs;
        private Integer restSecs;
        private BigDecimal weightSuggest;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartSessionRequest {
        @NotNull(message = "Customer ID is required")
        private Long customerId;
        private Long workoutPlanId;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteSessionRequest {
        private Integer caloriesBurned;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionResponse {
        private Long id;
        private Long customerId;
        private Long workoutPlanId;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String status;
        private Integer totalDurationSecs;
        private Integer caloriesBurned;
        private String notes;
        private List<LogResponse> logs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateLogRequest {
        @NotNull(message = "Session ID is required")
        private Long workoutSessionId;
        @NotNull(message = "Exercise ID is required")
        private Long exerciseId;
        @NotNull(message = "Set number is required")
        private Integer setNumber;
        private Integer repsActual;
        private BigDecimal weightKg;
        private Integer durationSecs;
        private Integer rpe;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LogResponse {
        private Long id;
        private Long exerciseId;
        private String exerciseName;
        private String muscleGroup;
        private Integer setNumber;
        private Integer repsActual;
        private BigDecimal weightKg;
        private Integer durationSecs;
        private Integer rpe;
        private String notes;
        private LocalDateTime loggedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProgressSummaryResponse {
        private Long customerId;
        private Integer streak;
        private Integer totalSessionsCompleted;
        private Integer sessionsThisMonth;
        private List<PersonalRecordItem> personalRecords;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonalRecordItem {
        private Long exerciseId;
        private String exerciseName;
        private BigDecimal maxWeightKg;
        private Integer maxReps;
    }
}

