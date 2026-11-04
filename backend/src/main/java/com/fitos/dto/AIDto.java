package com.fitos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class AIDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRequest {
        private Long customerId;
        @NotBlank(message = "Message must not be empty")
        private String message;
        private Long conversationId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatResponse {
        private Long conversationId;
        private String reply;
        private List<String> suggestions;
        private Boolean fallback;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendRequest {
        private Long customerId;
        private String goal;
        private String targetMuscleGroup;
        private String currentFitnessLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendResponse {
        private Long recommendationId;
        private String title;
        private String rawJson;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationItem {
        private Long id;
        private String service;
        private String title;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private List<MessageItem> messages;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageItem {
        private Long id;
        private String role;
        private String content;
        private LocalDateTime createdAt;
    }
}

