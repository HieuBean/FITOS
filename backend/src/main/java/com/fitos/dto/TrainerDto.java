package com.fitos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

public class TrainerDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        private String phone;
        private String specialty;
        private String bio;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String name;
        private String phone;
        private String specialty;
        private String bio;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignCustomerRequest {
        private Long customerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String specialty;
        private String bio;
        private Boolean isActive;
        private Integer assignedCustomerCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleResponse {
        private Long trainerId;
        private String trainerName;
        private List<PTSessionItem> ptSessions;
        private List<ClassSessionItem> classSessions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PTSessionItem {
        private Long id;
        private Long customerId;
        private String customerName;
        private String startTime;
        private String endTime;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassSessionItem {
        private Long id;
        private String className;
        private String startTime;
        private String endTime;
        private Integer currentBookings;
        private Integer maxCapacity;
        private String status;
    }
}

