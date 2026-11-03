package com.fitos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.client.AiOperationsClient;
import com.fitos.dto.ai.AiDtos.NoShowRiskResponse;
import com.fitos.dto.ai.AiDtos.ScheduleSuggestionResponse;
import com.fitos.dto.ai.AiDtos.SlotSuggestion;
import com.fitos.dto.ai.AiDtos.TrainerTipsResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.repository.CustomerRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task #111, #112, #113 phía backend: gom dữ liệu vận hành rồi gọi service `ai-operations`.
 * Nếu service Python không chạy được, mọi endpoint vẫn trả kết quả rule-based để demo
 * không bị chết (trường `source` cho biết dữ liệu đến từ đâu).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiOperationsService {

    private final AiOperationsClient client;
    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    // --------------------------------------------------------------- no-show
    @Transactional(readOnly = true)
    public NoShowRiskResponse noShowRisk(Long sessionId, Long customerId) {
        Long studioId = StudioContext.currentStudioId();
        Map<String, Object> session = jdbc.query(
                "SELECT cs.session_date, cs.start_time, cs.capacity, cs.booked_count, c.name AS class_name " +
                        "FROM class_sessions cs JOIN classes c ON c.id = cs.class_id AND c.studio_id = cs.studio_id " +
                        "WHERE cs.studio_id = ? AND cs.id = ?",
                rs -> rs.next() ? Map.of(
                        "session_date", rs.getDate("session_date").toLocalDate(),
                        "start_time", rs.getTime("start_time").toLocalTime(),
                        "capacity", rs.getInt("capacity"),
                        "booked_count", rs.getInt("booked_count"),
                        "class_name", rs.getString("class_name")) : null,
                studioId, sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Không tìm thấy buổi học: " + sessionId);
        }
        LocalDate sessionDate = (LocalDate) session.get("session_date");
        long daysAhead = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), sessionDate));

        long priorBookings = 0;
        long priorNoShows = 0;
        if (customerId != null) {
            Long total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM bookings WHERE studio_id = ? AND customer_id = ?",
                    Long.class, studioId, customerId);
            Long noShow = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM bookings WHERE studio_id = ? AND customer_id = ? AND status = 'NO_SHOW'",
                    Long.class, studioId, customerId);
            priorBookings = total == null ? 0 : total;
            priorNoShows = noShow == null ? 0 : noShow;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("session_id", sessionId);
        request.put("customer_id", customerId);
        request.put("days_ahead", daysAhead);
        request.put("hour_of_day", ((java.time.LocalTime) session.get("start_time")).getHour());
        request.put("weekday", sessionDate.getDayOfWeek().getValue());
        request.put("prior_bookings", priorBookings);
        request.put("prior_no_shows", priorNoShows);
        request.put("capacity", session.get("capacity"));
        request.put("booked_count", session.get("booked_count"));

        Map<String, Object> response = client.post("/predict-noshow", request);
        if (response != null && response.get("probability") instanceof Number probability) {
            return new NoShowRiskResponse(sessionId, customerId, clamp(probability.doubleValue()),
                    String.valueOf(response.getOrDefault("risk_level", riskLevel(probability.doubleValue()))),
                    String.valueOf(response.getOrDefault("reason", "Dự đoán từ ai-operations")),
                    "ai-operations");
        }
        double fallback = fallbackProbability(daysAhead, priorBookings, priorNoShows,
                ((java.time.LocalTime) session.get("start_time")).getHour());
        return new NoShowRiskResponse(sessionId, customerId, fallback, riskLevel(fallback),
                fallbackReason(fallback, priorNoShows, daysAhead), "fallback");
    }

    // --------------------------------------------------------------- schedule
    @Transactional(readOnly = true)
    public ScheduleSuggestionResponse scheduleSuggestions(LocalDate date) {
        Long studioId = StudioContext.currentStudioId();
        LocalDate target = date == null ? LocalDate.now() : date;

        List<Map<String, Object>> sessions = jdbc.query(
                "SELECT cs.id AS session_id, cs.class_id, c.name AS class_name, cs.start_time, cs.capacity, " +
                        "cs.booked_count FROM class_sessions cs " +
                        "JOIN classes c ON c.id = cs.class_id AND c.studio_id = cs.studio_id " +
                        "WHERE cs.studio_id = ? AND cs.session_date = ? AND cs.status = 'SCHEDULED' " +
                        "ORDER BY cs.start_time",
                (rs, rowNum) -> Map.of(
                        "session_id", rs.getLong("session_id"),
                        "class_id", rs.getLong("class_id"),
                        "class_name", rs.getString("class_name"),
                        "start_time", rs.getTime("start_time").toLocalTime().toString(),
                        "capacity", rs.getInt("capacity"),
                        "booked_count", rs.getInt("booked_count")),
                studioId, java.sql.Date.valueOf(target));

        Map<String, Object> request = new HashMap<>();
        request.put("date", target.toString());
        request.put("sessions", sessions);

        Map<String, Object> response = client.post("/suggest-schedule", request);
        List<SlotSuggestion> suggestions = parseSuggestions(response);
        if (!suggestions.isEmpty()) {
            return new ScheduleSuggestionResponse(target, suggestions, "ai-operations");
        }
        return new ScheduleSuggestionResponse(target, fallbackSuggestions(sessions), "fallback");
    }

    @SuppressWarnings("unchecked")
    private List<SlotSuggestion> parseSuggestions(Map<String, Object> response) {
        if (response == null || !(response.get("suggestions") instanceof List<?> raw)) {
            return List.of();
        }
        List<SlotSuggestion> result = new ArrayList<>();
        for (Object item : raw) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> suggestion = (Map<String, Object>) map;
            result.add(new SlotSuggestion(
                    String.valueOf(suggestion.getOrDefault("slot", "")),
                    suggestion.get("class_id") instanceof Number number ? number.longValue() : null,
                    String.valueOf(suggestion.getOrDefault("class_name", "")),
                    String.valueOf(suggestion.getOrDefault("reason", "")),
                    suggestion.get("priority") instanceof Number priority ? priority.doubleValue() : 0.5));
        }
        return result;
    }

    private List<SlotSuggestion> fallbackSuggestions(List<Map<String, Object>> sessions) {
        return sessions.stream()
                .filter(session -> ((int) session.get("capacity")) > 0)
                .map(session -> {
                    int capacity = (int) session.get("capacity");
                    int booked = (int) session.get("booked_count");
                    double fill = (double) booked / capacity;
                    return new SlotSuggestion(
                            String.valueOf(session.get("start_time")),
                            (Long) session.get("class_id"),
                            String.valueOf(session.get("class_name")),
                            fill < 0.4
                                    ? "Lớp còn " + (capacity - booked) + " chỗ — nên đẩy thông báo lấp chỗ"
                                    : "Lớp đã đạt " + Math.round(fill * 100) + "% sức chứa",
                            1 - fill);
                })
                .sorted(Comparator.comparingDouble(SlotSuggestion::priority).reversed())
                .limit(3)
                .toList();
    }

    // ---------------------------------------------------------- trainer tips
    @Transactional(readOnly = true)
    public TrainerTipsResponse trainerTips(Long customerId) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByIdAndStudioId(customerId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội viên: " + customerId));

        Long attendance30 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE studio_id = ? AND customer_id = ? AND checkin_time >= ?",
                Long.class, studioId, customerId, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(30)));
        Long noShows30 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings b JOIN class_sessions cs ON cs.id = b.session_id " +
                        "AND cs.studio_id = b.studio_id WHERE b.studio_id = ? AND b.customer_id = ? " +
                        "AND b.status = 'NO_SHOW' AND cs.session_date >= ?",
                Long.class, studioId, customerId, java.sql.Date.valueOf(LocalDate.now().minusDays(30)));

        Map<String, Object> profile = new HashMap<>();
        profile.put("customer_id", customerId);
        profile.put("full_name", customer.getFullName());
        profile.put("fitness_goal", customer.getFitnessGoal());
        profile.put("attendance_last_30_days", attendance30 == null ? 0 : attendance30);
        profile.put("no_shows_last_30_days", noShows30 == null ? 0 : noShows30);

        Map<String, Object> request = new HashMap<>();
        request.put("customer", profile);

        Map<String, Object> response = client.post("/trainer-tips", request);
        if (response != null && response.get("tips") instanceof List<?> tips && !tips.isEmpty()) {
            return new TrainerTipsResponse(customerId, customer.getFullName(),
                    tips.stream().map(String::valueOf).toList(), "ai-operations");
        }
        return new TrainerTipsResponse(customerId, customer.getFullName(),
                fallbackTips(customer, attendance30 == null ? 0 : attendance30, noShows30 == null ? 0 : noShows30),
                "fallback");
    }

    private List<String> fallbackTips(Customer customer, long attendance30, long noShows30) {
        List<String> tips = new ArrayList<>();
        if (attendance30 == 0) {
            tips.add("Hội viên chưa điểm danh trong 30 ngày — gọi lại để xếp 1 buổi làm quen.");
        } else if (attendance30 >= 12) {
            tips.add("Tần suất rất tốt (" + attendance30 + " buổi/30 ngày) — cân nhắc tăng khối lượng 5-10%.");
        } else {
            tips.add("Duy trì " + attendance30 + " buổi/30 ngày — đặt mục tiêu thêm 1 buổi mỗi tuần.");
        }
        if (noShows30 >= 2) {
            tips.add("Có " + noShows30 + " lượt no-show — nhắc lịch trước 24h và xác nhận qua Zalo.");
        }
        if (customer.getFitnessGoal() != null) {
            tips.add("Bám mục tiêu " + customer.getFitnessGoal() + ": kiểm tra lại bài tập mỗi 4 tuần.");
        }
        return tips;
    }

    private double fallbackProbability(long daysAhead, long priorBookings, long priorNoShows, int hour) {
        double probability = 0.18;
        if (priorBookings > 0) {
            probability += Math.min(0.4, 0.45 * ((double) priorNoShows / priorBookings));
        }
        if (daysAhead > 7) {
            probability += 0.12;
        } else if (daysAhead > 3) {
            probability += 0.06;
        }
        if (hour < 7 || hour >= 20) {
            probability += 0.05;
        }
        return clamp(probability);
    }

    private String fallbackReason(double probability, long priorNoShows, long daysAhead) {
        return String.format("Xác suất %.0f%% — %d lượt no-show trước đây, đặt trước %d ngày",
                probability * 100, priorNoShows, daysAhead);
    }

    private String riskLevel(double probability) {
        if (probability >= 0.6) {
            return "HIGH";
        }
        if (probability >= 0.35) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, Math.round(value * 1000) / 1000.0));
    }

    /** Cho phép tầng khác serialize nhanh response AI thô (giữ nguyên JSON). */
    public String raw(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    public JsonNode read(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new BadRequestException("JSON không hợp lệ");
        }
    }
}
