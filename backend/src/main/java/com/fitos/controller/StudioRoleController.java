package com.fitos.controller;

import com.fitos.service.StudioAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class StudioRoleController {
    private final StudioAccessService studioAccess;

    @PostMapping("/{studioId}/roles")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Map<String, Object> grantStaffRole(@PathVariable Long studioId, @Valid @RequestBody GrantRoleRequest request) {
        studioAccess.grantStaffRole(request.userId(), studioId, request.role());
        return Map.of("userId", request.userId(), "studioId", studioId, "role", request.role());
    }

    public record GrantRoleRequest(@NotNull Long userId, @NotBlank String role) {}
}
