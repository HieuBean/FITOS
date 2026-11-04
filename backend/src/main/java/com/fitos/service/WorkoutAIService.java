package com.fitos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.client.WorkoutAIClient;
import com.fitos.dto.AIDto;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.*;
import com.fitos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutAIService {

    private final WorkoutAIClient workoutAIClient;
    private final CustomerRepository customerRepository;
    private final WorkoutPlanRepository planRepository;
    private final BodyMetricRepository bodyMetricRepository;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Task 69: Xử lý chat AI proxy.
     * Tự động nạp context hội viên (thông số cơ thể, mục tiêu tập luyện) đưa vào request sang Python AI service.
     */
    @Transactional
    public AIDto.ChatResponse chatProxy(AIDto.ChatRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + req.getCustomerId()));

        AiConversation conversation;
        if (req.getConversationId() != null) {
            conversation = conversationRepository.findById(req.getConversationId())
                    .orElseGet(() -> createNewConversation(customer, "WORKOUT"));
        } else {
            conversation = createNewConversation(customer, "WORKOUT");
        }

        // 1. Lưu tin nhắn người dùng
        AiMessage userMsg = AiMessage.builder()
                .conversation(conversation)
                .role("user")
                .content(req.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(userMsg);

        // 2. Nạp context thể lực từ DB
        Map<String, Object> fitnessContext = buildCustomerFitnessContext(customer);

        Map<String, Object> aiPayload = new HashMap<>();
        aiPayload.put("customer_id", customer.getId());
        aiPayload.put("message", req.getMessage());
        aiPayload.put("fitness_context", fitnessContext);

        // 3. Gọi microservice Python ai-workout (port 8004) qua WebClient/RestClient
        Map<String, Object> aiResponse = workoutAIClient.chat(aiPayload);

        String replyText = (String) aiResponse.getOrDefault("reply", "Cảm ơn bạn đã liên hệ!");
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) aiResponse.getOrDefault("suggestions", List.of());
        Boolean isFallback = (Boolean) aiResponse.getOrDefault("fallback", false);

        // 4. Lưu phản hồi trợ lý AI vào DB
        AiMessage botMsg = AiMessage.builder()
                .conversation(conversation)
                .role("assistant")
                .content(replyText)
                .modelUsed("gpt-4o-mini")
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(botMsg);

        return AIDto.ChatResponse.builder()
                .conversationId(conversation.getId())
                .reply(replyText)
                .suggestions(suggestions)
                .fallback(isFallback)
                .build();
    }

    /**
     * Task 70: Lấy danh sách hội thoại của người dùng
     */
    @Transactional(readOnly = true)
    public List<AIDto.ConversationItem> getCustomerConversations(Long customerId) {
        return conversationRepository.findByCustomerIdOrderByStartedAtDesc(customerId).stream()
                .map(c -> AIDto.ConversationItem.builder()
                        .id(c.getId())
                        .service(c.getService())
                        .title(c.getTitle())
                        .startedAt(c.getStartedAt())
                        .endedAt(c.getEndedAt())
                        .messages(c.getMessages() != null ?
                                c.getMessages().stream().map(m -> AIDto.MessageItem.builder()
                                        .id(m.getId())
                                        .role(m.getRole())
                                        .content(m.getContent())
                                        .createdAt(m.getCreatedAt())
                                        .build()).collect(Collectors.toList()) : List.of())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Task 70: Tạo gợi ý kế hoạch từ AI và lưu bản ghi
     */
    @Transactional
    public AIDto.RecommendResponse createRecommendation(AIDto.RecommendRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + req.getCustomerId()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("customer_id", customer.getId());
        payload.put("goal", req.getGoal());
        payload.put("target_muscle", req.getTargetMuscleGroup());
        payload.put("fitness_level", req.getCurrentFitnessLevel());

        Map<String, Object> aiResult = workoutAIClient.recommend(payload);

        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(aiResult);
        } catch (Exception e) {
            jsonString = "{}";
        }

        AiRecommendation rec = AiRecommendation.builder()
                .customer(customer)
                .service("WORKOUT")
                .recType("WORKOUT_PLAN_OPTIMIZE")
                .recommendation(jsonString)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        AiRecommendation saved = recommendationRepository.save(rec);

        return AIDto.RecommendResponse.builder()
                .recommendationId(saved.getId())
                .title((String) aiResult.getOrDefault("title", "Gợi ý cá nhân hóa từ FITOS AI"))
                .rawJson(jsonString)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Task 70: Cập nhật trạng thái gợi ý AI (ACCEPT hoặc DISMISS)
     */
    @Transactional
    public void updateRecommendationStatus(Long recId, String status) {
        AiRecommendation rec = recommendationRepository.findById(recId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + recId));

        rec.setStatus(status.toUpperCase());
        rec.setActedAt(LocalDateTime.now());
        recommendationRepository.save(rec);
        log.info("Updated AI recommendation {} status to {}", recId, status);
    }

    private AiConversation createNewConversation(Customer customer, String service) {
        AiConversation conversation = AiConversation.builder()
                .customer(customer)
                .service(service)
                .title("Trợ lý thể lực & kế hoạch tập luyện")
                .startedAt(LocalDateTime.now())
                .messages(new ArrayList<>())
                .build();
        return conversationRepository.save(conversation);
    }

    private Map<String, Object> buildCustomerFitnessContext(Customer customer) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("customer_name", customer.getName());

        planRepository.findActiveByCustomerId(customer.getId())
                .ifPresent(p -> {
                    ctx.put("active_plan_name", p.getName());
                    ctx.put("goal", p.getGoal());
                });

        bodyMetricRepository.findFirstByCustomerIdOrderByRecordedAtDesc(customer.getId())
                .ifPresent(m -> {
                    ctx.put("weight_kg", m.getWeightKg());
                    ctx.put("height_cm", m.getHeightCm());
                    ctx.put("body_fat_pct", m.getBodyFatPct());
                });

        return ctx;
    }
}

