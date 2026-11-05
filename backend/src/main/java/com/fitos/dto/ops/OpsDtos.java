package com.fitos.dto.ops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class OpsDtos {

    private OpsDtos() {
    }

    // ---------------------------------------------------------------- classes
    public record ClassRequest(
            @NotBlank String name,
            @NotBlank String category,
            String description,
            @Positive Integer defaultCapacity,
            String colorCode,
            Boolean isActive
    ) {
    }

    public record ClassResponse(
            Long id,
            String name,
            String category,
            String description,
            Integer defaultCapacity,
            String colorCode,
            Boolean isActive
    ) {
    }

    // --------------------------------------------------------------- sessions
    public record SessionRequest(
            @NotNull Long classId,
            Long trainerId,
            @NotNull LocalDate sessionDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            String room,
            @Positive Integer capacity,
            String status
    ) {
    }

    public record SessionResponse(
            Long id,
            Long classId,
            String className,
            String category,
            String colorCode,
            Long trainerId,
            String trainerName,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            Integer capacity,
            Integer bookedCount,
            Integer availableSpots,
            String status
    ) {
    }

    // --------------------------------------------------------------- bookings
    public record BookingRequest(Long sessionId, Long customerId) {
    }

    public record BookingResponse(
            Long id,
            Long customerId,
            String customerName,
            Long sessionId,
            String className,
            LocalDate sessionDate,
            LocalTime startTime,
            String status,
            Instant bookedAt,
            boolean checkedIn
    ) {
    }

    public record CancelRequest(String reason) {
    }

    // --------------------------------------------------------------- check-in
    public record CheckInRequest(
            Long bookingId,
            Long customerId,
            @Pattern(regexp = "DESK|QR|MANUAL", message = "method phải là DESK, QR hoặc MANUAL") String method,
            String notes
    ) {
    }

    public record CheckInResponse(
            Long attendanceId,
            Long customerId,
            String customerName,
            Long bookingId,
            LocalDateTime checkinTime,
            String method,
            String membershipStatus
    ) {
    }

    public record AttendanceResponse(
            Long id,
            Long customerId,
            String customerName,
            Long bookingId,
            Long classSessionId,
            LocalDateTime checkinTime,
            String method,
            String notes
    ) {
    }

    // ------------------------------------------------------------- pt session
    public record PtSessionRequest(
            @NotNull Long trainerId,
            @NotNull Long customerId,
            Long ptPackageId,
            @NotNull LocalDate sessionDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            String trainingNotes
    ) {
    }

    public record PtSessionResponse(
            Long id,
            Long trainerId,
            String trainerName,
            Long customerId,
            String customerName,
            Long ptPackageId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String status,
            String completionRef,
            String trainingNotes
    ) {
    }

    public record PtCompleteRequest(@NotBlank String completionRef, String trainingNotes) {
    }

    // ---------------------------------------------------------------- trainer
    public record TrainerRequest(
            @NotBlank String fullName,
            @NotBlank String phone,
            String email,
            String specialization,
            String bio,
            Integer experienceYears,
            BigDecimal rating,
            Boolean isActive
    ) {
    }

    public record TrainerResponse(
            Long id,
            Long userId,
            String fullName,
            String phone,
            String email,
            String specialization,
            Integer experienceYears,
            BigDecimal rating,
            Boolean isActive,
            List<Long> customerIds
    ) {
    }

    // --------------------------------------------------------------- exercise
    public record ExerciseRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "COMPOUND|UPPER_PUSH|UPPER_PULL|LOWER_BODY|CORE|CARDIO",
                    message = "category không hợp lệ") String category,
            @NotBlank String muscleGroup,
            @NotBlank @Pattern(regexp = "Barbell|Dumbbell|Machine|Cable|Bodyweight",
                    message = "equipment không hợp lệ") String equipment,
            @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED", message = "difficulty không hợp lệ") String difficulty,
            String instruction,
            String videoUrl
    ) {
    }

    public record ExerciseResponse(
            Long id,
            String name,
            String category,
            String muscleGroup,
            String equipment,
            String difficulty,
            String instruction,
            String videoUrl
    ) {
    }
}
