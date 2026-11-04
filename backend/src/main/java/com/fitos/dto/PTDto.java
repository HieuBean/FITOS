package com.fitos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PTDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PackageResponse {
        private Long id;
        private Long customerId;
        private Long trainerId;
        private String trainerName;
        private Integer totalSessions;
        private Integer remainingSessions;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long version;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleSessionRequest {
        @NotNull(message = "Customer ID is required")
        private Long customerId;
        @NotNull(message = "Trainer ID is required")
        private Long trainerId;
        private Long ptPackageId;
        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;
        @NotNull(message = "End time is required")
        private LocalDateTime endTime;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RescheduleSessionRequest {
        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;
        @NotNull(message = "End time is required")
        private LocalDateTime endTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelSessionRequest {
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionResponse {
        private Long id;
        private Long ptPackageId;
        private Long customerId;
        private String customerName;
        private Long trainerId;
        private String trainerName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private String completionRef;
        private LocalDateTime completedAt;
        private String notes;
        private String cancelReason;
    }
}

