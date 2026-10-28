package com.fitos.service;

import com.fitos.dto.dashboard.CrmFunnelResponse;
import com.fitos.dto.dashboard.DashboardStatsResponse;
import com.fitos.dto.dashboard.RevenueChartResponse;
import com.fitos.repository.LeadRepository;
import com.fitos.security.StudioContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LeadRepository leadRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        Long studioId = StudioContext.currentStudioId();
        long activeMembers = number("SELECT COUNT(DISTINCT customer_id) FROM memberships " +
                "WHERE studio_id = :studioId AND status = 'ACTIVE' " +
                "AND start_date <= CURRENT_DATE AND end_date >= CURRENT_DATE", studioId);
        long newLeads = number("SELECT COUNT(*) FROM leads WHERE studio_id = :studioId AND created_at >= DATE_TRUNC('month', CURRENT_DATE)", studioId);
        long convertedLeads = number("SELECT COUNT(*) FROM leads WHERE studio_id = :studioId AND status = 'CONVERTED' AND created_at >= DATE_TRUNC('month', CURRENT_DATE)", studioId);
        long totalLeads = newLeads;
        long classesToday = number("SELECT COUNT(*) FROM class_sessions WHERE studio_id = :studioId AND session_date = CURRENT_DATE AND status <> 'CANCELLED'", studioId);
        long bookedToday = number("SELECT COALESCE(SUM(booked_count), 0) FROM class_sessions WHERE studio_id = :studioId AND session_date = CURRENT_DATE AND status <> 'CANCELLED'", studioId);
        long capacityToday = number("SELECT COALESCE(SUM(capacity), 0) FROM class_sessions WHERE studio_id = :studioId AND session_date = CURRENT_DATE AND status <> 'CANCELLED'", studioId);
        BigDecimal revenue = decimal("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE studio_id = :studioId AND status = 'SUCCESS' AND paid_at >= DATE_TRUNC('month', CURRENT_DATE) AND paid_at < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month'", studioId);
        double conversionRate = totalLeads > 0 ? Math.round(((double) convertedLeads / totalLeads) * 1000.0) / 10.0 : 0.0;
        double fillRate = capacityToday > 0 ? Math.round((bookedToday * 100.0 / capacityToday) * 10.0) / 10.0 : 0.0;
        return DashboardStatsResponse.builder()
                .revenueThisMonth(revenue)
                .activeMembers(activeMembers)
                .classesToday((int) classesToday)
                .bookedSpotsToday((int) bookedToday)
                .totalCapacityToday((int) capacityToday)
                .classFillRate(fillRate)
                .newLeadsThisMonth((int) newLeads)
                .leadConversionRate(conversionRate)
                .build();
    }

    private Query query(String sql, Long studioId) {
        return entityManager.createNativeQuery(sql).setParameter("studioId", studioId);
    }

    private long number(String sql, Long studioId) {
        return ((Number) query(sql, studioId).getSingleResult()).longValue();
    }

    private BigDecimal decimal(String sql, Long studioId) {
        Object value = query(sql, studioId).getSingleResult();
        return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
    }

    @Transactional(readOnly = true)
    public RevenueChartResponse getRevenueChart() {
        List<?> rows = query("SELECT to_char(month_start, 'YYYY-MM') AS month, COALESCE(SUM(p.amount), 0) AS amount " +
                "FROM generate_series(date_trunc('month', CURRENT_DATE) - INTERVAL '5 months', date_trunc('month', CURRENT_DATE), INTERVAL '1 month') AS months(month_start) " +
                "LEFT JOIN payments p ON p.studio_id = :studioId AND p.status = 'SUCCESS' AND p.paid_at >= month_start AND p.paid_at < month_start + INTERVAL '1 month' " +
                "GROUP BY month_start ORDER BY month_start", StudioContext.currentStudioId()).getResultList();
        List<RevenueChartResponse.MonthlyData> data = new ArrayList<>();
        for (Object row : rows) {
            Object[] values = (Object[]) row;
            data.add(new RevenueChartResponse.MonthlyData(values[0].toString(), new BigDecimal(values[1].toString())));
        }
        return RevenueChartResponse.builder().points(data).build();
    }

    @Transactional(readOnly = true)
    public CrmFunnelResponse getCrmFunnel() {
        Long studioId = StudioContext.currentStudioId();
        List<Object[]> grouped = leadRepository.countGroupedByStatus(studioId);
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : grouped) {
            statusMap.put((String) row[0], (Long) row[1]);
        }

        long total = leadRepository.countByStudioId(studioId);
        long newCount = statusMap.getOrDefault("NEW", 0L);
        long contactedCount = statusMap.getOrDefault("CONTACTED", 0L);
        long interestedCount = statusMap.getOrDefault("INTERESTED", 0L);
        long trialCount = statusMap.getOrDefault("TRIAL", 0L);
        long negotiatingCount = statusMap.getOrDefault("NEGOTIATING", 0L);
        long convertedCount = statusMap.getOrDefault("CONVERTED", 0L);
        long lostCount = statusMap.getOrDefault("LOST", 0L);

        List<CrmFunnelResponse.FunnelStage> stages = List.of(
                new CrmFunnelResponse.FunnelStage("NEW", newCount, total > 0 ? Math.round((newCount * 100.0 / total) * 10.0) / 10.0 : 0.0),
                new CrmFunnelResponse.FunnelStage("CONTACTED", contactedCount, total > 0 ? Math.round((contactedCount * 100.0 / total) * 10.0) / 10.0 : 0.0),
                new CrmFunnelResponse.FunnelStage("INTERESTED", interestedCount, total > 0 ? Math.round((interestedCount * 100.0 / total) * 10.0) / 10.0 : 0.0),
                new CrmFunnelResponse.FunnelStage("TRIAL", trialCount, total > 0 ? Math.round((trialCount * 100.0 / total) * 10.0) / 10.0 : 0.0),
                new CrmFunnelResponse.FunnelStage("NEGOTIATING", negotiatingCount, total > 0 ? Math.round((negotiatingCount * 100.0 / total) * 10.0) / 10.0 : 0.0),
                new CrmFunnelResponse.FunnelStage("CONVERTED", convertedCount, total > 0 ? Math.round((convertedCount * 100.0 / total) * 10.0) / 10.0 : 0.0),
                new CrmFunnelResponse.FunnelStage("LOST", lostCount, total > 0 ? Math.round((lostCount * 100.0 / total) * 10.0) / 10.0 : 0.0)
        );

        return CrmFunnelResponse.builder().stages(stages).build();
    }
}
