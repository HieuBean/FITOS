package com.fitos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.client.AiWorkoutClient;
import com.fitos.dto.ai.AiDtos.RecommendationResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.AiRecommendation;
import com.fitos.model.BodyMetric;
import com.fitos.model.Customer;
import com.fitos.repository.AiRecommendationRepository;
import com.fitos.repository.BodyMetricRepository;
import com.fitos.repository.CustomerRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task #66: sinh đề xuất từ AI và lưu vào `ai_recommendations` (JSONB) để hội viên
 * ACCEPT / DISMISS về sau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final AiRecommendationRepository recommendationRepository;
    private final CustomerRepository customerRepository;
    private final BodyMetricRepository bodyMetricRepository;
    private final AiWorkoutClient workoutClient;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Transactional
    public RecommendationResponse generate(Long userId, String feedbackOrPainNote) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ hội viên"));

        Map<String, Object> body = new HashMap<>();
        body.put("feedback_or_pain_note", feedbackOrPainNote);
        body.put("fitness_profile", profile(customer, studioId));
        List<Map<String, Object>> plans = jdbc.queryForList(
                "SELECT id, title FROM workout_plans WHERE customer_id = ? AND studio_id = ? AND status = 'ACTIVE' " +
                        "ORDER BY start_date DESC, id DESC LIMIT 1", customer.getId(), studioId);
        if (!plans.isEmpty()) {
            Map<String, Object> plan = plans.get(0);
            body.put("current_plan_name", plan.get("title"));
            body.put("current_exercises", jdbc.queryForList(
                    "SELECT e.name, pe.target_sets, pe.target_reps_min, pe.target_reps_max, pe.target_weight_kg " +
                            "FROM workout_plan_exercises pe JOIN exercises e ON e.id = pe.exercise_id " +
                            "WHERE pe.plan_id = ? AND pe.studio_id = ? ORDER BY pe.day_of_week, pe.order_index",
                    plan.get("id"), studioId));
        } else {
            body.put("current_exercises", List.of());
        }

        Map<String, Object> response = workoutClient.post("/recommend", body);
        String title;
        String explanation;
        Object suggestedChanges;

        if (response != null && response.get("analysis") != null) {
            title = response.get("title") == null ? "Điều chỉnh giáo án" : String.valueOf(response.get("title"));
            explanation = String.valueOf(response.get("analysis"));
            suggestedChanges = response.getOrDefault("suggested_changes", List.of());
        } else {
            title = "Gợi ý phục hồi";
            explanation = "Chưa kết nối được AI coach. Gợi ý tạm thời: giảm 10% khối lượng ở buổi tiếp theo, "
                    + "ưu tiên ngủ đủ 7-8 giờ và ghi lại cảm nhận sau buổi tập.";
            suggestedChanges = List.of();
        }

        AiRecommendation recommendation = AiRecommendation.builder()
                .studioId(studioId)
                .customerId(customer.getId())
                .type(response == null ? "RECOVERY" : "WORKOUT_CHANGE")
                .title(title.length() > 150 ? title.substring(0, 150) : title)
                .explanation(explanation)
                .suggestedChanges(toJson(suggestedChanges))
                .status("PENDING")
                .build();
        return toResponse(recommendationRepository.save(recommendation));
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> listForMember(Long userId, String status) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ hội viên"));
        return listForCustomer(customer.getId(), status);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> listForCustomer(Long customerId, String status) {
        Long studioId = StudioContext.currentStudioId();
        List<AiRecommendation> recommendations = (status == null || status.isBlank())
                ? recommendationRepository.findByCustomerIdAndStudioIdOrderByCreatedAtDesc(customerId, studioId)
                : recommendationRepository.findByCustomerIdAndStudioIdAndStatusOrderByCreatedAtDesc(
                        customerId, studioId, status);
        return recommendations.stream().map(this::toResponse).toList();
    }

    @Transactional
    public RecommendationResponse respond(Long id, boolean accept, Long actorUserId) {
        Long studioId = StudioContext.currentStudioId();
        AiRecommendation recommendation = recommendationRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề xuất: " + id));

        Customer customer = customerRepository.findByUserIdAndStudioId(actorUserId, studioId).orElse(null);
        boolean isOwner = customer != null && customer.getId().equals(recommendation.getCustomerId());
        if (!isOwner && !hasStaffRole()) {
            throw new BadRequestException("Bạn không có quyền phản hồi đề xuất này");
        }
        recommendation.setStatus(accept ? "ACCEPTED" : "DISMISSED");
        recommendation.setRespondedAt(Instant.now());
        return toResponse(recommendationRepository.save(recommendation));
    }

    private boolean hasStaffRole() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(authority ->
                List.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_RECEPTIONIST", "ROLE_TRAINER")
                        .contains(authority.getAuthority()));
    }

    private Map<String, Object> profile(Customer customer, Long studioId) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("full_name", customer.getFullName());
        if (customer.getDob() != null) {
            profile.put("age", Period.between(customer.getDob(), LocalDate.now()).getYears());
        }
        if (customer.getGender() != null) {
            profile.put("gender", customer.getGender());
        }
        if (customer.getFitnessGoal() != null) {
            profile.put("fitness_goal", customer.getFitnessGoal());
        }
        bodyMetricRepository.findFirstByCustomerIdOrderByRecordedDateDesc(customer.getId())
                .ifPresent(metric -> profile.put("body_metrics", metricMap(metric)));
        return profile;
    }

    private Map<String, Object> metricMap(BodyMetric metric) {
        Map<String, Object> result = new HashMap<>();
        result.put("weight_kg", metric.getWeightKg());
        result.put("height_cm", metric.getHeightCm());
        result.put("body_fat_pct", metric.getBodyFatPercent());
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private RecommendationResponse toResponse(AiRecommendation recommendation) {
        Object suggestedChanges;
        try {
            suggestedChanges = objectMapper.readValue(recommendation.getSuggestedChanges(), Object.class);
        } catch (Exception exception) {
            suggestedChanges = recommendation.getSuggestedChanges();
        }
        return new RecommendationResponse(recommendation.getId(), recommendation.getCustomerId(),
                recommendation.getType(), recommendation.getTitle(), recommendation.getExplanation(),
                suggestedChanges, recommendation.getStatus(), recommendation.getCreatedAt());
    }
}
