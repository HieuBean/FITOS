package com.fitos.controller;

import com.fitos.dto.WorkoutDto;
import com.fitos.service.WorkoutPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workout-plans")
@RequiredArgsConstructor
@Tag(name = "Workout Plans API", description = "Quản lý kế hoạch và giáo án tập luyện (Task 66)")
public class WorkoutPlanController {

    private final WorkoutPlanService workoutPlanService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các kế hoạch tập luyện (có thể lọc theo customerId)")
    public ResponseEntity<List<WorkoutDto.PlanResponse>> getAll(@RequestParam(required = false) Long customerId) {
        return ResponseEntity.ok(workoutPlanService.findAll(customerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết kế hoạch tập luyện")
    public ResponseEntity<WorkoutDto.PlanResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(workoutPlanService.getById(id));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Lấy toàn bộ kế hoạch tập luyện của một hội viên")
    public ResponseEntity<List<WorkoutDto.PlanResponse>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(workoutPlanService.getCustomerPlans(customerId));
    }

    @PostMapping
    @Operation(summary = "Tạo mới kế hoạch tập luyện (kèm danh sách bài tập theo ngày)")
    public ResponseEntity<WorkoutDto.PlanResponse> create(@Valid @RequestBody WorkoutDto.CreatePlanRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutPlanService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật kế hoạch tập luyện (Task 66)")
    public ResponseEntity<WorkoutDto.PlanResponse> update(
            @PathVariable Long id,
            @RequestBody WorkoutDto.UpdatePlanRequest req) {
        return ResponseEntity.ok(workoutPlanService.update(id, req));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Kích hoạt kế hoạch tập luyện (tự động deactivate kế hoạch cũ)")
    public ResponseEntity<WorkoutDto.PlanResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(workoutPlanService.activate(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Đánh dấu hoàn thành kế hoạch")
    public ResponseEntity<Void> complete(@PathVariable Long id) {
        workoutPlanService.complete(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Hủy kế hoạch tập luyện")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        workoutPlanService.cancel(id);
        return ResponseEntity.ok().build();
    }
}

