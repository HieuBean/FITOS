package com.fitos.controller;

import com.fitos.dto.PTDto;
import com.fitos.service.PTPackageService;
import com.fitos.service.PTSessionService;
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
@Tag(name = "PT Management API", description = "Quản lý gói PT & đặt lịch huấn luyện viên cá nhân (Task 62-65)")
public class PTController {

    private final PTPackageService ptPackageService;
    private final PTSessionService ptSessionService;

    // === Task 62: PT Packages ===

    @GetMapping("/api/pt-packages/me")
    @Operation(summary = "Lấy danh sách gói tập PT của hội viên")
    public ResponseEntity<List<PTDto.PackageResponse>> getMyPackages(@RequestParam(defaultValue = "1") Long customerId) {
        return ResponseEntity.ok(ptPackageService.getCustomerPackages(customerId));
    }

    @PatchMapping("/api/pt-packages/{id}/use")
    @Operation(summary = "Trừ 1 buổi tập PT với Optimistic Locking (@Version chống race condition)")
    public ResponseEntity<PTDto.PackageResponse> usePackageSession(@PathVariable Long id) {
        return ResponseEntity.ok(ptPackageService.useSession(id));
    }

    // === Task 63, 64, 65: PT Sessions ===

    @GetMapping("/api/pt-sessions")
    @Operation(summary = "Lấy danh sách các buổi tập PT theo customerId hoặc trainerId")
    public ResponseEntity<List<PTDto.SessionResponse>> getSessions(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long trainerId) {
        return ResponseEntity.ok(ptSessionService.getSessions(customerId, trainerId));
    }

    @PostMapping("/api/pt-sessions")
    @Operation(summary = "Đặt lịch tập PT với HLV (kiểm tra xung đột lịch)")
    public ResponseEntity<PTDto.SessionResponse> scheduleSession(@Valid @RequestBody PTDto.ScheduleSessionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ptSessionService.scheduleSession(req));
    }

    @PatchMapping("/api/pt-sessions/{id}/reschedule")
    @Operation(summary = "Dời lịch buổi tập PT")
    public ResponseEntity<PTDto.SessionResponse> rescheduleSession(
            @PathVariable Long id,
            @Valid @RequestBody PTDto.RescheduleSessionRequest req) {
        return ResponseEntity.ok(ptSessionService.rescheduleSession(id, req));
    }

    @PatchMapping("/api/pt-sessions/{id}/cancel")
    @Operation(summary = "Hủy buổi tập PT")
    public ResponseEntity<Void> cancelSession(
            @PathVariable Long id,
            @RequestBody(required = false) PTDto.CancelSessionRequest req) {
        String reason = req != null ? req.getReason() : "Hủy lịch theo yêu cầu";
        ptSessionService.cancelSession(id, reason);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/api/pt-sessions/{id}/complete")
    @Operation(summary = "Hoàn tất buổi tập PT - Idempotent via completion_ref UUID (Task 65)")
    public ResponseEntity<PTDto.SessionResponse> completeSession(@PathVariable Long id) {
        return ResponseEntity.ok(ptSessionService.completeSession(id));
    }
}

