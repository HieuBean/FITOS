package com.fitos.controller;

import com.fitos.dto.WorkoutDto;
import com.fitos.model.Customer;
import com.fitos.security.UserPrincipal;
import com.fitos.service.CurrentMemberService;
import com.fitos.service.WorkoutLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ghi nhận buổi tập, set/reps và thống kê tiến độ.
 *
 * <p>Trước đây mọi endpoint ở đây không có {@code @PreAuthorize} và nhận
 * {@code customerId} từ query string/body, nên bất kỳ hội viên nào cũng đọc và ghi
 * được dữ liệu tập luyện của người khác (IDOR). Nay hồ sơ khách hàng được suy ra từ
 * chính danh tính đang đăng nhập, và service kiểm tra quyền sở hữu buổi tập.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Workout Execution & Logs API", description = "Ghi nhận buổi tập, sets/reps và tính toán Streak/PR")
public class WorkoutExecutionController {

    private final WorkoutLogService workoutLogService;
    private final CurrentMemberService currentMember;

    // === Workout Sessions ===

    @PostMapping("/api/workout-sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bắt đầu buổi tập mới cho hội viên đang đăng nhập")
    public ResponseEntity<WorkoutDto.SessionResponse> startSession(
            @RequestBody(required = false) WorkoutDto.StartSessionRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        Customer customer = currentMember.require(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutLogService.startSession(customer, req));
    }

    @PatchMapping("/api/workout-sessions/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hoàn thành buổi tập (ghi nhận thời gian & calories)")
    public ResponseEntity<WorkoutDto.SessionResponse> completeSession(
            @PathVariable Long id,
            @RequestBody(required = false) WorkoutDto.CompleteSessionRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workoutLogService.completeSession(id, req, principal));
    }

    @GetMapping("/api/workout-sessions/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lịch sử các buổi tập của hội viên đang đăng nhập")
    public ResponseEntity<List<WorkoutDto.SessionResponse>> getMySessions(
            @AuthenticationPrincipal UserPrincipal principal) {
        Customer customer = currentMember.require(principal);
        return ResponseEntity.ok(workoutLogService.getCustomerSessions(customer.getId()));
    }

    @GetMapping("/api/workout-sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết buổi tập (chỉ chủ sở hữu hoặc nhân viên)")
    public ResponseEntity<WorkoutDto.SessionResponse> getSessionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workoutLogService.getSessionById(id, principal));
    }

    // === Workout Logs & Progress ===

    @PostMapping("/api/workout-logs")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ghi log một set tập (sets, reps, weight_kg, rpe)")
    public ResponseEntity<WorkoutDto.LogResponse> createLog(
            @Valid @RequestBody WorkoutDto.CreateLogRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutLogService.createLog(req, principal));
    }

    @GetMapping({"/api/workout-logs/{sessionId}/logs", "/api/workout-sessions/{sessionId}/logs"})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách các set trong một buổi tập")
    public ResponseEntity<List<WorkoutDto.LogResponse>> getLogsBySession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workoutLogService.getLogsBySession(sessionId, principal));
    }

    /**
     * Chỉ còn MỘT đường dẫn cho tiến độ hội viên: {@code /api/progress/me}
     * (do {@link ProgressController} đảm nhiệm, trả {@code ProgressResponse}).
     * Trước đây chính endpoint này map trùng {@code /api/progress/me} khiến Spring
     * báo "Ambiguous handler methods" và trả 400 cho mọi lần gọi.
     */
    @GetMapping("/api/workout-logs/progress/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Thống kê tiến độ theo dữ liệu tập luyện của chính hội viên")
    public ResponseEntity<WorkoutDto.ProgressSummaryResponse> getMyProgress(
            @AuthenticationPrincipal UserPrincipal principal) {
        Customer customer = currentMember.require(principal);
        return ResponseEntity.ok(workoutLogService.getProgress(customer.getId()));
    }
}
