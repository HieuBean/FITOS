package com.fitos.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WorkoutAIClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WorkoutAIClient(
            @Value("${ai.workout.base-url:http://localhost:8004}") String baseUrl,
            @Value("${ai.internal.token:fitos-ai-token}") String internalToken,
            ObjectMapper objectMapper) {
        
        this.objectMapper = objectMapper;

        // Spec Chuyên đề 5: Connection Timeout: 3s, Read Timeout: 15s (cho LLM)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Gọi endpoint POST /chat trên ai-workout microservice (port 8004)
     */
    public Map<String, Object> chat(Map<String, Object> payload) {
        try {
            log.info("Sending chat request to AI Workout service: {}", payload.get("message"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/chat")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("AI Workout Service call failed (will fallback safely): {}", e.getMessage());
        }

        // Fallback response khi Python microservice chưa sẵn sàng
        Map<String, Object> fallback = new HashMap<>();
        String userMsg = payload.getOrDefault("message", "").toString();
        fallback.put("reply", "Chào bạn! Tôi là Trợ lý Luyện tập FITOS AI. " +
                "Hiện tôi đã ghi nhận yêu cầu: \"" + userMsg + "\". " +
                "Lời khuyên chung: Hãy khởi động kỹ 5-10 phút trước buổi tập và duy trì nhịp thở đều đặn!");
        fallback.put("suggestions", List.of(
                "Lập kế hoạch tập ngực và tay sau",
                "Chế độ dinh dưỡng tăng cơ giảm mỡ",
                "Cách tránh chấn thương khi Squat"
        ));
        fallback.put("fallback", true);
        return fallback;
    }

    /**
     * Gọi endpoint POST /recommend trên ai-workout microservice (port 8004)
     */
    public Map<String, Object> recommend(Map<String, Object> payload) {
        try {
            log.info("Sending recommendation request to AI Workout service for customer: {}", payload.get("customer_id"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/recommend")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("AI Workout Recommendation call failed (fallback): {}", e.getMessage());
        }

        // Fallback response
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("title", "Gợi ý lộ trình thích ứng theo thể trạng");
        fallback.put("suggested_changes", List.of(
                "Tăng 1 set bài Bench Press",
                "Nghỉ 90s giữa các hiệp nặng",
                "Bổ sung 15 phút cardio sau buổi tập"
        ));
        fallback.put("status", "PENDING");
        fallback.put("fallback", true);
        return fallback;
    }
}

