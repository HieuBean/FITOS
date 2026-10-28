package com.fitos.ai.chat;

import com.fitos.ai.chat.dto.AiChatDtos.ChatRequest;
import com.fitos.ai.chat.dto.AiChatDtos.ChatResponse;
import com.fitos.ai.chat.dto.AiChatDtos.ConversationSummary;
import com.fitos.ai.chat.dto.AiChatDtos.MessageDto;
import com.fitos.common.security.CurrentUserService;
import com.fitos.customer.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Tasks #64 and #65: AI Chat Proxy to ai-workout (port 8004) & Conversation history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatProxyService {

    private final AiConversationRepository conversationRepository;
    private final CurrentUserService currentUser;

    @Value("${ai.workout.url:http://localhost:8004}")
    private String aiWorkoutUrl;

    @Transactional
    public ChatResponse chat(ChatRequest req) {
        Customer customer = currentUser.requireCustomer();
        Long customerId = customer.getId();

        AiConversation conversation;
        if (req.conversationId() != null) {
            conversation = conversationRepository.findWithMessagesById(req.conversationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
            if (!conversation.getCustomerId().equals(customerId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
            }
        } else {
            conversation = new AiConversation();
            conversation.setCustomerId(customerId);
            String title = req.message().length() > 50 ? req.message().substring(0, 47) + "..." : req.message();
            conversation.setTitle(title);
            conversation = conversationRepository.save(conversation);
        }

        // 1. Save user message
        AiMessage userMsg = new AiMessage("user", req.message());
        conversation.addMessage(userMsg);

        // 2. Call ai-workout service (port 8004)
        String assistantReplyText = callAiWorkoutService(customerId, req.message());

        // 3. Save assistant reply
        AiMessage assistantMsg = new AiMessage("assistant", assistantReplyText);
        conversation.addMessage(assistantMsg);

        AiConversation saved = conversationRepository.save(conversation);

        return new ChatResponse(
                saved.getId(),
                saved.getTitle(),
                MessageDto.from(userMsg),
                MessageDto.from(assistantMsg)
        );
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getMyConversations() {
        Customer customer = currentUser.requireCustomer();
        List<AiConversation> list = conversationRepository.findByCustomerIdOrderByUpdatedAtDesc(customer.getId());
        return list.stream().map(ConversationSummary::from).toList();
    }

    private String callAiWorkoutService(Long customerId, String message) {
        try {
            RestClient restClient = RestClient.builder().baseUrl(aiWorkoutUrl).build();
            Map<?, ?> response = restClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("customer_id", customerId, "message", message))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }
            if (response != null && response.containsKey("answer")) {
                return (String) response.get("answer");
            }
        } catch (Exception e) {
            log.warn("ai-workout service unreachable at {}: {}", aiWorkoutUrl, e.getMessage());
        }

        // Friendly fallback when ai-workout is not running locally
        return "Hi " + currentUser.requireCustomer().getFullName() +
                "! I'm your FITOS AI Coach. I've noted your message: \"" + message +
                "\". Keep up your great consistency this week! Remember to stay hydrated and hit your target protein intake.";
    }
}
