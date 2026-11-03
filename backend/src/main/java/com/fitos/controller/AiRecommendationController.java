package com.fitos.controller;

import com.fitos.dto.ai.AiDtos.RecommendationResponse;
import com.fitos.security.UserPrincipal;
import com.fitos.service.AiRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Task #66: đề xuất giáo án do AI sinh ra, hội viên ACCEPT hoặc DISMISS. */
@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Recommendations", description = "Lưu và phản hồi đề xuất của AI")
public class AiRecommendationController {

    private final AiRecommendationService recommendationService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Đề xuất của tôi")
    public ResponseEntity<List<RecommendationResponse>> mine(
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(recommendationService.listForMember(principal.getId(), status));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Đề xuất của một hội viên")
    public ResponseEntity<List<RecommendationResponse>> byCustomer(
            @RequestParam("customerId") Long customerId,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(recommendationService.listForCustomer(customerId, status));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Sinh đề xuất mới từ ai-workout và lưu lại")
    public ResponseEntity<RecommendationResponse> generate(
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String note = body == null || body.get("feedbackOrPainNote") == null
                ? null : String.valueOf(body.get("feedbackOrPainNote"));
        return new ResponseEntity<>(recommendationService.generate(principal.getId(), note), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chấp nhận đề xuất")
    public ResponseEntity<RecommendationResponse> accept(@PathVariable("id") Long id,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(recommendationService.respond(id, true, principal.getId()));
    }

    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bỏ qua đề xuất")
    public ResponseEntity<RecommendationResponse> dismiss(@PathVariable("id") Long id,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(recommendationService.respond(id, false, principal.getId()));
    }
}
