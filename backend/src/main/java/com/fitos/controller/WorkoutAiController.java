package com.fitos.controller;

import com.fitos.model.BodyMetric;
import com.fitos.model.Customer;
import com.fitos.repository.BodyMetricRepository;
import com.fitos.repository.CustomerRepository;
import com.fitos.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasAnyAuthority('ROLE_MEMBER','ROLE_TRAINER','ROLE_MANAGER','ROLE_ADMIN')")
public class WorkoutAiController {
    private final CustomerRepository customers;
    private final BodyMetricRepository metrics;
    private final JdbcTemplate jdbc;
    private final RestTemplate http = createHttpClient();
    private final String serviceUrl;
    private final String serviceToken;

    public WorkoutAiController(CustomerRepository customers, BodyMetricRepository metrics, JdbcTemplate jdbc,
                               @Value("${fitos.ai.workout-url}") String serviceUrl,
                               @Value("${fitos.ai.token}") String serviceToken) {
        this.customers = customers;
        this.metrics = metrics;
        this.jdbc = jdbc;
        this.serviceUrl = serviceUrl;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/chat")
    public Map<?, ?> chat(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ChatInput input) {
        if (input.message() == null || input.message().isBlank() || input.message().length() > 2000) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Message must be 1-2000 characters");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("message", input.message());
        body.put("conversation_history", input.conversationHistory() == null ? List.of() :
                input.conversationHistory().stream()
                        .filter(message -> message != null
                                && ("user".equals(message.get("role")) || "assistant".equals(message.get("role")))
                                && message.get("content") != null && !message.get("content").isBlank())
                        .limit(10)
                        .map(message -> Map.of("role", message.get("role"),
                                "content", message.get("content").substring(0, Math.min(2000, message.get("content").length()))))
                        .toList());
        body.put("fitness_profile", profile(principal));
        return forward("/chat", body);
    }

    @PostMapping("/recommend")
    public Map<?, ?> recommend(@AuthenticationPrincipal UserPrincipal principal, @RequestBody RecommendInput input) {
        if (input.feedbackOrPainNote() != null && input.feedbackOrPainNote().length() > 2000) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Feedback is too long");
        }
        Customer customer = customerOrNull(principal);
        Map<String, Object> body = new HashMap<>();
        if (customer == null) {
            body.put("current_exercises", List.of());
        } else {
            List<Map<String, Object>> plans = jdbc.queryForList(
                    "SELECT id, name FROM workout_plans WHERE customer_id = ? AND studio_id = ? AND status = 'ACTIVE' " +
                    "ORDER BY start_date DESC, id DESC LIMIT 1", customer.getId(), principal.getStudioId());
            if (!plans.isEmpty()) {
                Map<String, Object> plan = plans.get(0);
                body.put("current_plan_name", plan.get("name"));
                body.put("current_exercises", jdbc.queryForList(
                        "SELECT e.name, pe.sets, pe.reps, pe.weight_suggest " +
                        "FROM workout_plan_exercises pe JOIN exercises e ON e.id = pe.exercise_id " +
                        "WHERE pe.workout_plan_id = ? AND pe.studio_id = ? ORDER BY pe.day_of_week, pe.sort_order",
                        plan.get("id"), principal.getStudioId()));
            } else {
                body.put("current_exercises", List.of());
            }
        }
        body.put("feedback_or_pain_note", input.feedbackOrPainNote());
        body.put("fitness_profile", profile(principal));
        return forward("/recommend", body);
    }

    /**
     * Hồ sơ thể lực gửi kèm câu hỏi.
     *
     * <p>Nhân viên (ADMIN/MANAGER/TRAINER) không có hồ sơ khách hàng nên trước đây
     * hàm {@code customer()} ném 404 và toàn bộ trợ lý AI hỏng với các vai trò này.
     * Nay trả hồ sơ tối thiểu (chỉ tên) thay vì lỗi — AI vẫn tư vấn được, chỉ là
     * không có dữ liệu cân nặng/giới tính/lịch sử tập.
     */
    private Map<String, Object> profile(UserPrincipal principal) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("user_id", principal.getId());

        Customer customer = customerOrNull(principal);
        if (customer == null) {
            profile.put("full_name", principal.getFullName());
            profile.put("recent_workouts", List.of());
            profile.put("has_member_profile", false);
            return profile;
        }

        profile.put("full_name", customer.getFullName());
        profile.put("has_member_profile", true);
        if (customer.getDob() != null) profile.put("age", Period.between(customer.getDob(), LocalDate.now()).getYears());
        if (customer.getGender() != null) profile.put("gender", customer.getGender());
        if (customer.getFitnessGoal() != null) profile.put("fitness_goal", customer.getFitnessGoal());
        metrics.findFirstByCustomerIdOrderByRecordedDateDesc(customer.getId()).ifPresent(metric -> profile.put("body_metrics", metricMap(metric)));
        profile.put("recent_workouts", jdbc.query(
                "SELECT ws.session_date, e.name AS exercise_name, wl.set_number, wl.weight_kg, " +
                "wl.reps_actual, wl.rpe FROM workout_logs wl " +
                "JOIN workout_sessions ws ON ws.id = wl.workout_session_id AND ws.studio_id = wl.studio_id " +
                "JOIN exercises e ON e.id = wl.exercise_id " +
                "WHERE ws.customer_id = ? AND ws.studio_id = ? " +
                "ORDER BY ws.session_date DESC, wl.id DESC LIMIT 20",
                (rs, row) -> Map.of(
                        "session_date", rs.getDate("session_date").toLocalDate().toString(),
                        "exercise_name", rs.getString("exercise_name"),
                        "set_number", rs.getInt("set_number"),
                        "weight_kg", rs.getBigDecimal("weight_kg"),
                        "reps_completed", rs.getInt("reps_actual")),
                customer.getId(), principal.getStudioId()));
        return profile;
    }

    /** Trả về null thay vì ném 404 khi tài khoản không có hồ sơ hội viên. */
    private Customer customerOrNull(UserPrincipal principal) {
        return customers.findByUserIdAndStudioId(principal.getId(), principal.getStudioId()).orElse(null);
    }

    private Map<String, Object> metricMap(BodyMetric metric) {
        Map<String, Object> result = new HashMap<>();
        result.put("weight_kg", metric.getWeightKg());
        result.put("height_cm", metric.getHeightCm());
        result.put("body_fat_pct", metric.getBodyFatPercent());
        result.put("recorded_at", metric.getRecordedDate().toString());
        return result;
    }

    private static RestTemplate createHttpClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(25000);
        return new RestTemplate(factory);
    }

    private Map<?, ?> forward(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(serviceToken);
        try {
            Map<?, ?> response = http.postForObject(serviceUrl + path, new HttpEntity<>(body, headers), Map.class);
            if (response == null) throw new ResponseStatusException(BAD_GATEWAY, "AI service returned no response");
            return response;
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI service rejected the request");
        } catch (org.springframework.web.client.ResourceAccessException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI service unavailable");
        }
    }

    public record ChatInput(String message, @com.fasterxml.jackson.annotation.JsonProperty("conversation_history") List<Map<String, String>> conversationHistory) {}
    public record RecommendInput(@com.fasterxml.jackson.annotation.JsonProperty("current_plan_name") String currentPlanName,
                                 @com.fasterxml.jackson.annotation.JsonProperty("target_muscle_group") String targetMuscleGroup,
                                 @com.fasterxml.jackson.annotation.JsonProperty("feedback_or_pain_note") String feedbackOrPainNote) {}
}
