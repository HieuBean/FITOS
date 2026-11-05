package com.fitos.plan;

import com.fitos.common.security.CurrentUserService;
import com.fitos.common.security.Roles;
import com.fitos.plan.dto.PlanDtos.PlanRequest;
import com.fitos.plan.dto.PlanDtos.PlanResponse;
import com.fitos.plan.dto.PlanDtos.PlanStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Task #34 — GET/POST/PUT/PATCH /api/plans.
 * GET là public (trang /plans) → SecurityConfig của A cần permitAll GET /api/plans/**.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final CurrentUserService currentUser;

    @GetMapping
    public List<PlanResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive,
                                   @RequestParam(required = false) PlanType type) {
        // Chỉ quản lý mới xem được gói đã tắt
        boolean canSeeInactive = includeInactive && (currentUser.hasRole("ADMIN") || currentUser.hasRole("MANAGER"));
        return planService.list(canSeeInactive, type);
    }

    @GetMapping("/{id}")
    public PlanResponse get(@PathVariable Long id) {
        return planService.get(id, currentUser.isStaff());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.MANAGEMENT)
    public PlanResponse create(@Valid @RequestBody PlanRequest req) {
        return planService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.MANAGEMENT)
    public PlanResponse update(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        return planService.update(id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.MANAGEMENT)
    public PlanResponse setStatus(@PathVariable Long id, @Valid @RequestBody PlanStatusRequest req) {
        return planService.setActive(id, req.active());
    }
}
