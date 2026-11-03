package com.fitos.dto.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record NoShowRiskRequest(@NotNull Long sessionId, Long customerId) {
    }

    public record NoShowRiskResponse(
            Long sessionId,
            Long customerId,
            double probability,
            String riskLevel,
            String reason,
            String source
    ) {
    }

    public record ScheduleSuggestionRequest(LocalDate date) {
    }

    public record SlotSuggestion(
            String slot,
            Long classId,
            String className,
            String reason,
            double priority
    ) {
    }

    public record ScheduleSuggestionResponse(
            LocalDate date,
            List<SlotSuggestion> suggestions,
            String source
    ) {
    }

    public record TrainerTipsRequest(@Positive Long customerId) {
    }

    public record TrainerTipsResponse(
            Long customerId,
            String customerName,
            List<String> tips,
            String source
    ) {
    }

    public record RecommendationResponse(
            Long id,
            Long customerId,
            String type,
            String title,
            String explanation,
            Object suggestedChanges,
            String status,
            java.time.Instant createdAt
    ) {
    }
}
