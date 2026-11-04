package com.fitos.controller;

import com.fitos.dto.AIDto;
import com.fitos.service.WorkoutAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Workout Coach API", description = "Trợ lý AI thể lực & gợi ý điều chỉnh giáo án (Task 69-70)")
public class WorkoutAIController {

    private final WorkoutAIService workoutAIService;

    @PostMapping("/chat")
    @Operation(summary = "Chat với AI Workout Coach (Context Injection từ DB -> ai-workout:8004)")
    public ResponseEntity<AIDto.ChatResponse> chat(@Valid @RequestBody AIDto.ChatRequest req) {
        if (req.getCustomerId() == null) {
            req.setCustomerId(1L);
        }
        return ResponseEntity.ok(workoutAIService.chatProxy(req));
    }

    @GetMapping("/conversations/me")
    @Operation(summary = "Lấy lịch sử các phiên chat với AI của hội viên")
    public ResponseEntity<List<AIDto.ConversationItem>> getMyConversations(
            @RequestParam(defaultValue = "1") Long customerId) {
        return ResponseEntity.ok(workoutAIService.getCustomerConversations(customerId));
    }

    @PostMapping("/recommend")
    @Operation(summary = "Yêu cầu AI sinh gợi ý điều chỉnh giáo án (Structured JSON output)")
    public ResponseEntity<AIDto.RecommendResponse> recommend(@RequestBody AIDto.RecommendRequest req) {
        if (req.getCustomerId() == null) {
            req.setCustomerId(1L);
        }
        return ResponseEntity.ok(workoutAIService.createRecommendation(req));
    }

    @RequestMapping(value = {"/recommend/{id}/accept", "/recommendations/{id}/accept"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(summary = "Chấp nhận áp dụng gợi ý từ AI")
    public ResponseEntity<Void> acceptRecommendation(@PathVariable Long id) {
        workoutAIService.updateRecommendationStatus(id, "ACCEPTED");
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = {"/recommend/{id}/dismiss", "/recommendations/{id}/dismiss"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(summary = "Bỏ qua / từ chối gợi ý từ AI")
    public ResponseEntity<Void> dismissRecommendation(@PathVariable Long id) {
        workoutAIService.updateRecommendationStatus(id, "DISMISSED");
        return ResponseEntity.ok().build();
    }
}

