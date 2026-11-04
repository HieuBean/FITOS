package com.fitos.controller;

import com.fitos.dto.WorkoutDto;
import com.fitos.service.WorkoutLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Workout Execution & Logs API", description = "Ghi nhận buổi tập, sets/reps và tính toán Streak/PR (Task 67-68)")
public class WorkoutExecutionController {

    private final WorkoutLogService workoutLogService;

    // === Task 67: Workout Sessions ===

    @PostMapping("/api/workout-sessions")
    @Operation(summary = "Bắt đầu buổi tập mới")
    public ResponseEntity<WorkoutDto.SessionResponse> startSession(@Valid @RequestBody WorkoutDto.StartSessionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutLogService.startSession(req));
    }

    @PatchMapping("/api/workout-sessions/{id}/complete")
    @Operation(summary = "Hoàn thành buổi tập (ghi nhận thời gian & calories)")
    public ResponseEntity<WorkoutDto.SessionResponse> completeSession(
            @PathVariable Long id,
            @RequestBody(required = false) WorkoutDto.CompleteSessionRequest req) {
        return ResponseEntity.ok(workoutLogService.completeSession(id, req));
    }

    @GetMapping("/api/workout-sessions/me")
    @Operation(summary = "Lấy lịch sử các buổi tập của hội viên")
    public ResponseEntity<List<WorkoutDto.SessionResponse>> getMySessions(@RequestParam(defaultValue = "1") Long customerId) {
        return ResponseEntity.ok(workoutLogService.getCustomerSessions(customerId));
    }

    @GetMapping("/api/workout-sessions/{id}")
    @Operation(summary = "Xem chi tiết buổi tập")
    public ResponseEntity<WorkoutDto.SessionResponse> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(workoutLogService.getSessionById(id));
    }

    // === Task 68: Workout Logs & Progress ===

    @PostMapping("/api/workout-logs")
    @Operation(summary = "Ghi log một set tập (sets, reps, weight_kg, rpe)")
    public ResponseEntity<WorkoutDto.LogResponse> createLog(@Valid @RequestBody WorkoutDto.CreateLogRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutLogService.createLog(req));
    }

    @GetMapping({"/api/workout-logs/{sessionId}/logs", "/api/workout-sessions/{sessionId}/logs"})
    @Operation(summary = "Lấy danh sách các set tập trong một buổi tập")
    public ResponseEntity<List<WorkoutDto.LogResponse>> getLogsBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(workoutLogService.getLogsBySession(sessionId));
    }

    @GetMapping({"/api/workout-logs/progress/me", "/api/progress/me"})
    @Operation(summary = "Thống kê tiến độ hội viên: Streak liên tiếp, tổng buổi tập và kỷ lục cá nhân (PRs)")
    public ResponseEntity<WorkoutDto.ProgressSummaryResponse> getMyProgress(@RequestParam(defaultValue = "1") Long customerId) {
        return ResponseEntity.ok(workoutLogService.getProgress(customerId));
    }
}

