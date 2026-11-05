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
import java.time.ZoneId;
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

        // 2. Nạp Fitness Context
        Map<String, Object> fitnessContext = buildCustomerFitnessContext(customer);

        // 3. Chuẩn bị lịch sử hội thoại gần nhất
        List<AiMessage> pastMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<Map<String, String>> history = pastMessages.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());

        // 4. Chuẩn bị payload gửi sang ai-workout
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", req.getMessage());
        payload.put("fitness_profile", fitnessContext);
        payload.put("conversation_history", history);

        // 5. Gọi AI service
        Map<String, Object> aiResponse = workoutAIClient.chat(payload);

        String reply = (String) aiResponse.getOrDefault("reply", "Tôi chưa có phản hồi thích hợp vào lúc này.");
        List<String> suggestedActions = (List<String>) aiResponse.getOrDefault("suggested_actions", Collections.emptyList());

        // 6. Lưu câu trả lời từ AI
        AiMessage assistantMsg = AiMessage.builder()
                .conversation(conversation)
                .role("assistant")
                .content(reply)
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(assistantMsg);

        return AIDto.ChatResponse.builder()
                .conversationId(conversation.getId())
                .reply(reply)
                .suggestions(suggestedActions)
                .build();
    }

    /**
     * Lấy danh sách cuộc hội thoại của hội viên
     */
    public List<AIDto.ConversationItem> getCustomerConversations(Long customerId) {
        return conversationRepository.findByCustomerIdOrderByStartedAtDesc(customerId).stream()
                .map(c -> AIDto.ConversationItem.builder()
                        .id(c.getId())
                        .service(c.getService())
                        .title(c.getTitle())
                        .startedAt(c.getStartedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Task 70: Sinh gợi ý điều chỉnh giáo án từ AI (ai-workout service)
     * Kết quả được lưu vào bảng ai_recommendations (JSONB)
     */
    @Transactional
    public AIDto.RecommendResponse generateRecommendation(Long customerId, String goal, String targetMuscle, String fitnessLevel) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        Map<String, Object> fitnessContext = buildCustomerFitnessContext(customer);
        if (goal != null) fitnessContext.put("goal", goal);
        if (targetMuscle != null) fitnessContext.put("target_muscle_group", targetMuscle);
        if (fitnessLevel != null) fitnessContext.put("current_fitness_level", fitnessLevel);

        Map<String, Object> payload = new HashMap<>();
        payload.put("fitness_profile", fitnessContext);

        Map<String, Object> aiResult = workoutAIClient.recommend(payload);

        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(aiResult);
        } catch (Exception e) {
            log.error("Error serializing AI recommend result", e);
            jsonString = "{}";
        }

        AiRecommendation rec = AiRecommendation.builder()
                .customerId(customer.getId())
                .studioId(customer.getStudioId() != null ? customer.getStudioId() : 1L)
                .type("WORKOUT_CHANGE")
                .suggestedChanges(jsonString)
                .status("PENDING")
                .build();

        AiRecommendation saved = recommendationRepository.save(rec);

        return AIDto.RecommendResponse.builder()
                .recommendationId(saved.getId())
                .title((String) aiResult.getOrDefault("title", "Gợi ý cá nhân hóa từ FITOS AI"))
                .rawJson(jsonString)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt() != null ? LocalDateTime.ofInstant(saved.getCreatedAt(), ZoneId.systemDefault()) : LocalDateTime.now())
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

        bodyMetricRepository.findFirstByCustomerIdOrderByRecordedDateDesc(customer.getId())
                .ifPresent(m -> {
                    ctx.put("weight_kg", m.getWeightKg());
                    ctx.put("height_cm", m.getHeightCm());
                    ctx.put("body_fat_pct", m.getBodyFatPct());
                });

        return ctx;
    }
}
