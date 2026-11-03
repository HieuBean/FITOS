package com.fitos.controller;

import com.fitos.dto.common.PageResponse;
import com.fitos.dto.ops.OpsDtos.AttendanceResponse;
import com.fitos.dto.ops.OpsDtos.CheckInRequest;
import com.fitos.dto.ops.OpsDtos.CheckInResponse;
import com.fitos.security.UserPrincipal;
import com.fitos.service.AttendanceService;
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

/** Task #52 và #53: điểm danh (kiểm tra gói còn hạn) và tra cứu lịch sử điểm danh. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Check-in & Attendance", description = "Điểm danh tại quầy")
public class CheckInController {

    private final AttendanceService attendanceService;

    @PostMapping("/api/checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Điểm danh hội viên (bắt buộc còn gói hiệu lực)")
    public ResponseEntity<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest req,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return new ResponseEntity<>(attendanceService.checkIn(req, principal == null ? null : principal.getId()),
                HttpStatus.CREATED);
    }

    @GetMapping("/api/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Lịch sử điểm danh gần đây của studio")
    public ResponseEntity<PageResponse<AttendanceResponse>> recent(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.recent(page, size));
    }

    @GetMapping("/api/attendance/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Lịch sử điểm danh của tôi")
    public ResponseEntity<List<AttendanceResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(attendanceService.mine(principal.getId()));
    }
}
