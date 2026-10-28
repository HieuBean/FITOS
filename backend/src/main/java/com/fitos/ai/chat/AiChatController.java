package com.fitos.ai.chat;

import com.fitos.ai.chat.dto.AiChatDtos.ChatRequest;
import com.fitos.ai.chat.dto.AiChatDtos.ChatResponse;
import com.fitos.ai.chat.dto.AiChatDtos.ConversationSummary;
import com.fitos.common.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tasks #64 and #65: AI Chat Endpoints.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatProxyService chatService;

    @PostMapping("/chat")
    @PreAuthorize(Roles.MEMBER)
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        return chatService.chat(req);
    }

    @GetMapping("/conversations/me")
    @PreAuthorize(Roles.MEMBER)
    public List<ConversationSummary> getMyConversations() {
        return chatService.getMyConversations();
    }
}
