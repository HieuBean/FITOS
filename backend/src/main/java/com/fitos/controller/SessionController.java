package com.fitos.controller;

import com.fitos.dto.ClassDto;
import com.fitos.service.ClassSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions API", description = "Quản lý ca tập / buổi học nhóm (Task 61)")
public class SessionController {

    private final ClassSessionService classSessionService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các ca tập theo khoảng thời gian")
    public ResponseEntity<List<ClassDto.SessionResponse>> getSessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(classSessionService.getSessions(from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết ca học")
    public ResponseEntity<ClassDto.SessionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(classSessionService.findSessionById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo ca học mới")
    public ResponseEntity<ClassDto.SessionResponse> createSession(@Valid @RequestBody ClassDto.CreateSessionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classSessionService.createSession(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật ca học (Task 61)")
    public ResponseEntity<ClassDto.SessionResponse> updateSession(
            @PathVariable Long id,
            @RequestBody ClassDto.UpdateSessionRequest req) {
        return ResponseEntity.ok(classSessionService.updateSession(id, req));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAINER')")
    @Operation(summary = "Hủy ca học")
    public ResponseEntity<Void> cancelSession(
            @PathVariable Long id,
            @RequestBody(required = false) ClassDto.CancelSessionRequest req) {
        String reason = req != null ? req.getReason() : "Hủy theo yêu cầu quản lý";
        classSessionService.cancelSession(id, reason);
        return ResponseEntity.ok().build();
    }
}

