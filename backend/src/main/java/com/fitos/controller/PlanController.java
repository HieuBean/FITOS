package com.fitos.controller;

import com.fitos.dto.commerce.PlanDtos.PlanRequest;
import com.fitos.dto.commerce.PlanDtos.PlanResponse;
import com.fitos.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Task #34: CRUD gói dịch vụ. GET mở công khai cho trang /plans. */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Tag(name = "Service Plans", description = "Gói hội viên và gói PT")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @Operation(summary = "Danh sách gói dịch vụ đang bán (hoặc tất cả nếu includeInactive=true)")
    public ResponseEntity<List<PlanResponse>> list(
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(planService.list(includeInactive));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một gói")
    public ResponseEntity<PlanResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(planService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo gói dịch vụ")
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest req) {
        return new ResponseEntity<>(planService.create(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật gói dịch vụ")
    public ResponseEntity<PlanResponse> update(@PathVariable("id") Long id, @Valid @RequestBody PlanRequest req) {
        return ResponseEntity.ok(planService.update(id, req));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Bật/tắt bán gói dịch vụ")
    public ResponseEntity<PlanResponse> toggle(@PathVariable("id") Long id) {
        return ResponseEntity.ok(planService.toggleActive(id));
    }
}
