package com.fitos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class ClassDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateClassRequest {
        @NotBlank(message = "Name is required")
        private String name;
        private String description;
        private String type;
        private Integer durationMinutes;
        private Integer maxCapacity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateClassRequest {
        private String name;
        private String description;
        private String type;
        private Integer durationMinutes;
        private Integer maxCapacity;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassResponse {
        private Long id;
        private String name;
        private String description;
        private String type;
        private Integer durationMinutes;
        private Integer maxCapacity;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateSessionRequest {
        @NotNull(message = "Class ID is required")
        private Long classId;
        private Long trainerId;
        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;
        @NotNull(message = "End time is required")
        private LocalDateTime endTime;
        private Integer maxCapacity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionResponse {
        private Long id;
        private Long classId;
        private String className;
        private Long trainerId;
        private String trainerName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer maxCapacity;
        private Integer currentBookings;
        private String status;
        private String cancelReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateSessionRequest {
        private Long trainerId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer maxCapacity;
        private String status;
        private String cancelReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelSessionRequest {
        private String reason;
    }
}

