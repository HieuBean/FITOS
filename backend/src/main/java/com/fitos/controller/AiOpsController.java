package com.fitos.controller;

import com.fitos.client.AiOperationsClient;
import com.fitos.dto.ai.AiDtos.NoShowRiskRequest;
import com.fitos.dto.ai.AiDtos.NoShowRiskResponse;
import com.fitos.dto.ai.AiDtos.ScheduleSuggestionResponse;
import com.fitos.dto.ai.AiDtos.TrainerTipsRequest;
import com.fitos.dto.ai.AiDtos.TrainerTipsResponse;
import com.fitos.service.AiOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** Task #111, #112, #113 (phía backend): cầu nối tới service `ai-operations`. */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Operations", description = "Dự đoán no-show, gợi ý lịch và coaching tips")
public class AiOpsController {

    private final AiOperationsService aiOperationsService;
    private final AiOperationsClient aiOperationsClient;

    @PostMapping("/noshow-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    @Operation(summary = "Xác suất hội viên không đến buổi học")
    public ResponseEntity<NoShowRiskResponse> noShowRisk(@Valid @RequestBody NoShowRiskRequest req) {
        return ResponseEntity.ok(aiOperationsService.noShowRisk(req.sessionId(), req.customerId()));
    }

    @GetMapping("/schedule-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Gợi ý lấp chỗ trống cho lịch trong ngày")
    public ResponseEntity<ScheduleSuggestionResponse> scheduleSuggestions(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(aiOperationsService.scheduleSuggestions(date));
    }

    @PostMapping("/trainer-tips")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAINER')")
    @Operation(summary = "Gợi ý coaching cho huấn luyện viên theo tiến độ hội viên")
    public ResponseEntity<TrainerTipsResponse> trainerTips(@Valid @RequestBody TrainerTipsRequest req) {
        return ResponseEntity.ok(aiOperationsService.trainerTips(req.customerId()));
    }

    @GetMapping("/operations-health")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Kiểm tra service ai-operations có sẵn sàng không")
    public ResponseEntity<Map<String, Object>> health() {
        boolean reachable = aiOperationsClient.isReachable();
        return ResponseEntity.ok(Map.of("available", reachable,
                "note", reachable ? "ai-operations đang chạy" : "ai-operations không phản hồi — dùng fallback rule-based"));
    }
}
