package com.fitos.controller;

import com.fitos.common.security.Roles;
import com.fitos.dto.WorkoutDto;
import com.fitos.security.UserPrincipal;
import com.fitos.service.WorkoutPlanService;
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
 * Quản lý giáo án tập luyện.
 *
 * <p>Trước đây controller này KHÔNG có phân quyền nào, nên bất kỳ hội viên nào cũng
 * tạo/sửa/kích hoạt/huỷ được giáo án của người khác. Nay chỉ nhân viên và huấn luyện
 * viên được ghi; hội viên chỉ đọc giáo án của chính mình.
 */
@RestController
@RequestMapping("/api/workout-plans")
@RequiredArgsConstructor
@Tag(name = "Workout Plans API", description = "Quản lý kế hoạch và giáo án tập luyện")
public class WorkoutPlanController {

    private final WorkoutPlanService workoutPlanService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách giáo án (nhân viên xem tất cả, hội viên chỉ xem của mình)")
    public ResponseEntity<List<WorkoutDto.PlanResponse>> getAll(
            @RequestParam(required = false) Long customerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workoutPlanService.findAllForViewer(customerId, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết kế hoạch tập luyện")
    public ResponseEntity<WorkoutDto.PlanResponse> getById(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workoutPlanService.getByIdForViewer(id, principal));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Toàn bộ giáo án của một hội viên")
    public ResponseEntity<List<WorkoutDto.PlanResponse>> getByCustomer(
            @PathVariable Long customerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workoutPlanService.getCustomerPlansForViewer(customerId, principal));
    }

    @PostMapping
    @PreAuthorize(Roles.STAFF_OR_TRAINER)
    @Operation(summary = "Tạo mới kế hoạch tập luyện (kèm danh sách bài tập theo ngày)")
    public ResponseEntity<WorkoutDto.PlanResponse> create(@Valid @RequestBody WorkoutDto.CreatePlanRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutPlanService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF_OR_TRAINER)
    @Operation(summary = "Cập nhật kế hoạch tập luyện")
    public ResponseEntity<WorkoutDto.PlanResponse> update(
            @PathVariable Long id,
            @RequestBody WorkoutDto.UpdatePlanRequest req) {
        return ResponseEntity.ok(workoutPlanService.update(id, req));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize(Roles.STAFF_OR_TRAINER)
    @Operation(summary = "Kích hoạt kế hoạch tập luyện (tự động deactivate kế hoạch cũ)")
    public ResponseEntity<WorkoutDto.PlanResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(workoutPlanService.activate(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize(Roles.STAFF_OR_TRAINER)
    @Operation(summary = "Đánh dấu hoàn thành kế hoạch")
    public ResponseEntity<Void> complete(@PathVariable Long id) {
        workoutPlanService.complete(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize(Roles.STAFF_OR_TRAINER)
    @Operation(summary = "Hủy kế hoạch tập luyện")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        workoutPlanService.cancel(id);
        return ResponseEntity.ok().build();
    }
}
