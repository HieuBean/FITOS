package com.fitos.ai.chat.dto;

import com.fitos.ai.chat.AiConversation;
import com.fitos.ai.chat.AiMessage;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.List;

public final class AiChatDtos {

    private AiChatDtos() {
    }

    public record ChatRequest(
            Long conversationId,
            @NotBlank String message
    ) {
    }

    public record MessageDto(
            Long id,
            String role,
            String content,
            OffsetDateTime createdAt
    ) {
        public static MessageDto from(AiMessage m) {
            return new MessageDto(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt());
        }
    }

    public record ChatResponse(
            Long conversationId,
            String title,
            MessageDto userMessage,
            MessageDto reply
    ) {
    }

    public record ConversationSummary(
            Long id,
            String title,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<MessageDto> messages
    ) {
        public static ConversationSummary from(AiConversation c) {
            return new ConversationSummary(
                    c.getId(),
                    c.getTitle(),
                    c.getCreatedAt(),
                    c.getUpdatedAt(),
                    c.getMessages().stream().map(MessageDto::from).toList()
            );
        }
    }
}
