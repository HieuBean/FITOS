package com.fitos.controller;

import com.fitos.dto.progress.BodyMetricRequest;
import com.fitos.dto.progress.BodyMetricResponse;
import com.fitos.dto.progress.ProgressResponse;
import com.fitos.security.UserPrincipal;
import com.fitos.service.ProgressService;
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

@RestController
@RequiredArgsConstructor
@Tag(name = "Member Progress", description = "Endpoints for personal body metrics, streak calculation and PR tracking")
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/api/body-metrics/me")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Log current user's body metric (weight, fat %, muscle mass)")
    public ResponseEntity<BodyMetricResponse> recordBodyMetric(
            @Valid @RequestBody BodyMetricRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        return new ResponseEntity<>(progressService.recordBodyMetric(userId, req), HttpStatus.CREATED);
    }

    @GetMapping("/api/body-metrics/me")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Get historical body metrics for current member")
    public ResponseEntity<List<BodyMetricResponse>> getBodyMetrics(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        return ResponseEntity.ok(progressService.getBodyMetrics(userId));
    }

    @GetMapping("/api/progress/me")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Get overall fitness progress, streak, workouts/month and Personal Records (PRs)")
    public ResponseEntity<ProgressResponse> getProgress(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = requireUserId(principal);
        return ResponseEntity.ok(progressService.getProgress(userId));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        return principal.getId();
    }
}
