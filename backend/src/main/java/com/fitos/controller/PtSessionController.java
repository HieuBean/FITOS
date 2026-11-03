package com.fitos.controller;

import com.fitos.dto.ops.OpsDtos.PtCompleteRequest;
import com.fitos.dto.ops.OpsDtos.PtSessionRequest;
import com.fitos.dto.ops.OpsDtos.PtSessionResponse;
import com.fitos.service.PtSessionScheduleService;
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

/** Task #54, #55, #56: lịch PT, dời/huỷ và hoàn thành idempotent. */
@RestController
@RequestMapping("/api/pt-sessions")
@RequiredArgsConstructor
@Tag(name = "PT Sessions", description = "Buổi tập với huấn luyện viên cá nhân")
public class PtSessionController {

    private final PtSessionScheduleService scheduleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Danh sách buổi PT trong khoảng ngày")
    public ResponseEntity<List<PtSessionResponse>> list(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "trainerId", required = false) Long trainerId,
            @RequestParam(value = "customerId", required = false) Long customerId) {
        return ResponseEntity.ok(scheduleService.list(from, to, trainerId, customerId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Đặt lịch PT (kiểm tra trùng lịch HLV)")
    public ResponseEntity<PtSessionResponse> create(@Valid @RequestBody PtSessionRequest req) {
        return new ResponseEntity<>(scheduleService.create(req), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Dời lịch PT")
    public ResponseEntity<PtSessionResponse> reschedule(@PathVariable("id") Long id,
                                                        @Valid @RequestBody PtSessionRequest req) {
        return ResponseEntity.ok(scheduleService.reschedule(id, req));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Huỷ buổi PT")
    public ResponseEntity<PtSessionResponse> cancel(@PathVariable("id") Long id) {
        return ResponseEntity.ok(scheduleService.cancel(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Hoàn thành buổi PT — idempotent theo completionRef")
    public ResponseEntity<PtSessionResponse> complete(@PathVariable("id") Long id,
                                                      @Valid @RequestBody PtCompleteRequest req) {
        return ResponseEntity.ok(scheduleService.complete(id, req.completionRef(), req.trainingNotes()));
    }
}
