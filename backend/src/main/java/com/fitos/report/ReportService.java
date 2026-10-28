package com.fitos.report;

import com.fitos.report.dto.ReportDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Task #68: Reports service for Sales, Memberships, and Classes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final JdbcTemplate jdbc;

    public SalesReportResponse getSalesReport(LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0;

        String sqlTotals = """
                SELECT COALESCE(SUM(total_amount), 0) AS total_rev, COUNT(*) AS count
                FROM orders
                WHERE status = 'PAID'
                  AND paid_at >= ?::date AND paid_at < (?::date + INTERVAL '1 day')
                """;

        try {
            var row = jdbc.queryForMap(sqlTotals, startDate, endDate);
            Object revObj = row.get("total_rev");
            if (revObj instanceof BigDecimal bd) {
                totalRevenue = bd;
            } else if (revObj instanceof Number n) {
                totalRevenue = BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
            }
            Object countObj = row.get("count");
            if (countObj instanceof Number n) {
                totalOrders = n.longValue();
            }
        } catch (DataAccessException e) {
            log.warn("Error reading orders for sales report: {}", e.getMessage());
        }

        BigDecimal aov = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String sqlTimeline = """
                SELECT TO_CHAR(paid_at, 'YYYY-MM-DD') AS day, COALESCE(SUM(total_amount), 0) AS amount, COUNT(*) AS cnt
                FROM orders
                WHERE status = 'PAID'
                  AND paid_at >= ?::date AND paid_at < (?::date + INTERVAL '1 day')
                GROUP BY TO_CHAR(paid_at, 'YYYY-MM-DD')
                ORDER BY day ASC
                """;

        List<DailyRevenue> timeline = new ArrayList<>();
        try {
            timeline = jdbc.query(sqlTimeline, (rs, i) -> new DailyRevenue(
                    rs.getString("day"),
                    rs.getBigDecimal("amount"),
                    rs.getLong("cnt")
            ), startDate, endDate);
        } catch (DataAccessException e) {
            log.warn("Error reading daily sales: {}", e.getMessage());
        }

        String sqlBreakdown = """
                SELECT oi.plan_name, COUNT(*) AS cnt, SUM(oi.line_total) AS total
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.status = 'PAID'
                  AND o.paid_at >= ?::date AND o.paid_at < (?::date + INTERVAL '1 day')
                GROUP BY oi.plan_name
                ORDER BY total DESC
                """;

        List<PlanSalesBreakdown> breakdown = new ArrayList<>();
        try {
            breakdown = jdbc.query(sqlBreakdown, (rs, i) -> new PlanSalesBreakdown(
                    rs.getString("plan_name"),
                    rs.getLong("cnt"),
                    rs.getBigDecimal("total")
            ), startDate, endDate);
        } catch (DataAccessException e) {
            log.warn("Error reading plan breakdown: {}", e.getMessage());
        }

        return new SalesReportResponse(totalRevenue, totalOrders, aov, timeline, breakdown);
    }

    public MembershipReportResponse getMembershipsReport() {
        String sql = "SELECT status, COUNT(*) AS cnt FROM memberships GROUP BY status";
        List<MembershipStatusCount> counts = new ArrayList<>();
        long active = 0, paused = 0, expired = 0, total = 0;

        try {
            counts = jdbc.query(sql, (rs, i) -> new MembershipStatusCount(
                    rs.getString("status"),
                    rs.getLong("cnt")
            ));
            for (MembershipStatusCount c : counts) {
                total += c.count();
                if ("ACTIVE".equalsIgnoreCase(c.status())) active = c.count();
                else if ("PAUSED".equalsIgnoreCase(c.status())) paused = c.count();
                else if ("EXPIRED".equalsIgnoreCase(c.status())) expired = c.count();
            }
        } catch (DataAccessException e) {
            log.warn("Error querying memberships report: {}", e.getMessage());
        }

        double retentionRate = total > 0 ? ((double) (active + paused) / total) * 100 : 100.0;
        return new MembershipReportResponse(active, paused, expired, total, counts, Math.round(retentionRate * 10.0) / 10.0);
    }

    public ClassesReportResponse getClassesReport() {
        String sql = """
                SELECT c.name, COUNT(cs.id) AS sessions_cnt, COALESCE(SUM(cs.booked_count), 0) AS bookings_cnt,
                       COALESCE(AVG(NULLIF(cs.capacity, 0)), 20) AS avg_cap
                FROM classes c
                LEFT JOIN class_sessions cs ON cs.class_id = c.id
                GROUP BY c.id, c.name
                ORDER BY bookings_cnt DESC
                """;

        List<ClassUtilization> topClasses = new ArrayList<>();
        long totalSessions = 0;
        long totalBookings = 0;

        try {
            topClasses = jdbc.query(sql, (rs, i) -> {
                long sessions = rs.getLong("sessions_cnt");
                long bookings = rs.getLong("bookings_cnt");
                double avgCap = rs.getDouble("avg_cap");
                double occ = (sessions > 0 && avgCap > 0) ? (bookings / (sessions * avgCap)) * 100.0 : 0.0;
                return new ClassUtilization(
                        rs.getString("name"),
                        sessions,
                        bookings,
                        Math.round(occ * 10.0) / 10.0
                );
            });

            for (ClassUtilization u : topClasses) {
                totalSessions += u.totalSessions();
                totalBookings += u.totalBookings();
            }
        } catch (DataAccessException e) {
            log.warn("Error querying class report: {}", e.getMessage());
        }

        double overallOccupancy = totalSessions > 0 ? Math.min(100.0, (double) totalBookings / (totalSessions * 20.0) * 100.0) : 0.0;
        return new ClassesReportResponse(totalSessions, totalBookings, Math.round(overallOccupancy * 10.0) / 10.0, topClasses);
    }
}
