package com.fitos.service;

import com.fitos.dto.progress.BodyMetricRequest;
import com.fitos.dto.progress.BodyMetricResponse;
import com.fitos.dto.progress.ProgressResponse;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.BodyMetric;
import com.fitos.model.Customer;
import com.fitos.repository.BodyMetricRepository;
import com.fitos.repository.CustomerRepository;
import com.fitos.security.StudioContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final CustomerRepository customerRepository;
    private final BodyMetricRepository bodyMetricRepository;
    private final EntityManager entityManager;

    @Transactional
    public BodyMetricResponse recordBodyMetric(Long userId, BodyMetricRequest req) {
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + userId));

        LocalDate date = req.getRecordedDate() != null ? req.getRecordedDate() : LocalDate.now();

        // Upsert per day pattern
        BodyMetric metric = bodyMetricRepository.findByCustomerIdAndRecordedDate(customer.getId(), date)
                .orElseGet(() -> BodyMetric.builder()
                        .studioId(customer.getStudioId())
                        .customer(customer)
                        .recordedDate(date)
                        .build());

        metric.setWeightKg(req.getWeightKg());
        if (req.getHeightCm() != null) metric.setHeightCm(req.getHeightCm());
        if (req.getBodyFatPercent() != null) metric.setBodyFatPercent(req.getBodyFatPercent());
        if (req.getMuscleMassKg() != null) metric.setMuscleMassKg(req.getMuscleMassKg());
        if (req.getNotes() != null) metric.setNotes(req.getNotes());

        metric = bodyMetricRepository.save(metric);
        return mapToResponse(metric);
    }

    @Transactional(readOnly = true)
    public List<BodyMetricResponse> getBodyMetrics(Long userId) {
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + userId));

        return bodyMetricRepository.findByCustomerIdOrderByRecordedDateDesc(customer.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(Long userId) {
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + userId));

        Long customerId = customer.getId();

        // 1. Body metrics stats
        List<BodyMetric> metrics = bodyMetricRepository.findByCustomerIdOrderByRecordedDateDesc(customerId);
        BigDecimal currentWeight = null;
        BigDecimal startingWeight = null;
        BigDecimal weightChange = null;

        if (!metrics.isEmpty()) {
            currentWeight = metrics.get(0).getWeightKg();
            startingWeight = metrics.get(metrics.size() - 1).getWeightKg();
            weightChange = currentWeight.subtract(startingWeight);
        }

        // 2. Query workouts completed this month
        int workoutsThisMonth = 0;
        try {
            Query query = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM workout_sessions " +
                    "WHERE customer_id = :customerId AND studio_id = :studioId AND end_time IS NOT NULL " +
                    "AND session_date >= DATE_TRUNC('month', CURRENT_DATE) AND session_date <= CURRENT_DATE"
            );
            query.setParameter("customerId", customerId);
            query.setParameter("studioId", customer.getStudioId());
            workoutsThisMonth = ((Number) query.getSingleResult()).intValue();
        } catch (Exception ex) {
            log.warn("Could not query workout_sessions count: {}", ex.getMessage());
            workoutsThisMonth = 0;
        }

        // 3. Query Personal Records (PRs) from workout_logs and exercises
        List<ProgressResponse.PersonalRecord> prs = new ArrayList<>();
        try {
            Query prQuery = entityManager.createNativeQuery(
                    "SELECT exercise_name, weight_kg, reps_completed, session_date FROM (" +
                    " SELECT e.name AS exercise_name, wl.weight_kg, wl.reps_completed, ws.session_date, " +
                    " ROW_NUMBER() OVER (PARTITION BY e.id ORDER BY wl.weight_kg DESC, wl.reps_completed DESC, " +
                    " ws.session_date DESC, wl.id DESC) AS rank_no" +
                    " FROM workout_logs wl JOIN workout_sessions ws ON wl.session_id = ws.id AND wl.studio_id = ws.studio_id" +
                    " JOIN exercises e ON wl.exercise_id = e.id" +
                    " WHERE ws.customer_id = :customerId AND ws.studio_id = :studioId" +
                    ") ranked WHERE rank_no = 1 ORDER BY weight_kg DESC LIMIT 5"
            );
            prQuery.setParameter("customerId", customerId);
            prQuery.setParameter("studioId", customer.getStudioId());
            List<?> resultList = prQuery.getResultList();
            for (Object obj : resultList) {
                Object[] row = (Object[]) obj;
                String exName = (String) row[0];
                BigDecimal maxWeight = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                int reps = row[2] != null ? ((Number) row[2]).intValue() : 0;
                LocalDate date = row[3] instanceof Date ? ((Date) row[3]).toLocalDate()
                        : LocalDate.parse(row[3].toString());
                prs.add(new ProgressResponse.PersonalRecord(exName, maxWeight, reps, date));
            }
        } catch (Exception ex) {
            log.warn("Could not query personal records: {}", ex.getMessage());
        }

        return ProgressResponse.builder()
                .workoutsThisMonth(workoutsThisMonth)
                .currentStreakDays(calculateCurrentStreak(customerId))
                .longestStreakDays(calculateLongestStreak(customerId))
                .currentWeightKg(currentWeight)
                .startingWeightKg(startingWeight)
                .weightChangeKg(weightChange)
                .personalRecords(prs)
                .build();
    }

    private int calculateCurrentStreak(Long customerId) {
        List<?> rows = entityManager.createNativeQuery("SELECT DISTINCT session_date FROM workout_sessions WHERE customer_id = :customerId AND end_time IS NOT NULL ORDER BY session_date DESC")
                .setParameter("customerId", customerId).getResultList();
        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (Object row : rows) {
            LocalDate date = row instanceof Date ? ((Date) row).toLocalDate() : LocalDate.parse(row.toString());
            if (date.equals(expected) || (streak == 0 && date.equals(expected.minusDays(1)))) {
                streak++;
                expected = date.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    private int calculateLongestStreak(Long customerId) {
        List<?> rows = entityManager.createNativeQuery("SELECT DISTINCT session_date FROM workout_sessions WHERE customer_id = :customerId AND end_time IS NOT NULL ORDER BY session_date ASC")
                .setParameter("customerId", customerId).getResultList();
        int longest = 0;
        int current = 0;
        LocalDate previous = null;
        for (Object row : rows) {
            LocalDate date = row instanceof Date ? ((Date) row).toLocalDate() : LocalDate.parse(row.toString());
            current = previous != null && date.equals(previous.plusDays(1)) ? current + 1 : 1;
            longest = Math.max(longest, current);
            previous = date;
        }
        return longest;
    }
    private BodyMetricResponse mapToResponse(BodyMetric m) {
        return BodyMetricResponse.builder()
                .id(m.getId())
                .customerId(m.getCustomer().getId())
                .recordedDate(m.getRecordedDate())
                .weightKg(m.getWeightKg())
                .heightCm(m.getHeightCm())
                .bodyFatPercent(m.getBodyFatPercent())
                .muscleMassKg(m.getMuscleMassKg())
                .notes(m.getNotes())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
