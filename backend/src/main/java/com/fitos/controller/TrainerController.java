package com.fitos.controller;

import com.fitos.dto.TrainerDto;
import com.fitos.service.TrainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
@Tag(name = "Trainers API", description = "Quản lý huấn luyện viên & lịch dạy (Task 58, 59)")
public class TrainerController {

    private final TrainerService trainerService;

    @GetMapping
    @Operation(summary = "Danh sách huấn luyện viên (phân trang)")
    public ResponseEntity<Page<TrainerDto.Response>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(trainerService.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết huấn luyện viên")
    public ResponseEntity<TrainerDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(trainerService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo mới huấn luyện viên")
    public ResponseEntity<TrainerDto.Response> create(@Valid @RequestBody TrainerDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainerService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật thông tin huấn luyện viên")
    public ResponseEntity<TrainerDto.Response> update(@PathVariable Long id,
                                                      @Valid @RequestBody TrainerDto.UpdateRequest req) {
        return ResponseEntity.ok(trainerService.update(id, req));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Gán hội viên cho huấn luyện viên")
    public ResponseEntity<Void> assignCustomer(@PathVariable Long id,
                                               @RequestBody TrainerDto.AssignCustomerRequest req) {
        trainerService.assignCustomer(id, req.getCustomerId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Lấy ma trận lịch dạy tuần của huấn luyện viên")
    public ResponseEntity<TrainerDto.ScheduleResponse> getSchedule(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        LocalDate startDate = week != null ? week : LocalDate.now();
        return ResponseEntity.ok(trainerService.getWeeklySchedule(id, startDate));
    }
}

