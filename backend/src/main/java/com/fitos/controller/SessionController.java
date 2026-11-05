package com.fitos.controller;

import com.fitos.dto.ops.OpsDtos.SessionRequest;
import com.fitos.dto.ops.OpsDtos.SessionResponse;
import com.fitos.service.OpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Task #48: buổi học cụ thể trong lịch tuần. */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Class Sessions", description = "Buổi học theo lịch")
public class SessionController {

    private final OpsService opsService;

    @GetMapping
    @Operation(summary = "Buổi học trong khoảng ngày (mặc định 7 ngày từ hôm nay)")
    public ResponseEntity<List<SessionResponse>> list(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(opsService.listSessions(from, to));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo buổi học")
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody SessionRequest req) {
        return new ResponseEntity<>(opsService.createSession(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật buổi học")
    public ResponseEntity<SessionResponse> update(@PathVariable("id") Long id,
                                                  @Valid @RequestBody SessionRequest req) {
        return ResponseEntity.ok(opsService.updateSession(id, req));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Huỷ buổi học")
    public ResponseEntity<SessionResponse> cancel(@PathVariable("id") Long id) {
        return ResponseEntity.ok(opsService.cancelSession(id));
    }
}
