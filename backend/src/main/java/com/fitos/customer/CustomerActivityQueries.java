package com.fitos.customer;

import com.fitos.customer.dto.CustomerDtos.AttendanceItem;
import com.fitos.customer.dto.CustomerDtos.BookingItem;
import com.fitos.customer.dto.CustomerDtos.WorkoutItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;

/**
 * Truy vấn đọc sang module Booking (C/D) và Workout (D) cho Customer 360 (#31).
 * Dùng SQL thuần để KHÔNG phụ thuộc entity của người khác.
 * Tên bảng/cột giả định — xem docs/B_CONTRACTS.md. Nếu module kia chưa migrate,
 * phần đó trả rỗng (log WARN) thay vì làm hỏng cả trang hồ sơ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerActivityQueries {

    static final String SQL_BOOKINGS = """
            SELECT b.id, c.name AS class_name, s.start_time, b.status, b.created_at
            FROM bookings b
            JOIN class_sessions s ON s.id = b.session_id
            JOIN classes c        ON c.id = s.class_id
            WHERE b.customer_id = ?
            ORDER BY s.start_time DESC
            LIMIT 20
            """;

    static final String SQL_WORKOUTS = """
            SELECT ws.id, COALESCE(wp.name, 'Workout') AS title, ws.started_at, ws.completed_at, ws.status
            FROM workout_sessions ws
            LEFT JOIN workout_plans wp ON wp.id = ws.workout_plan_id
            WHERE ws.customer_id = ?
            ORDER BY ws.started_at DESC
            LIMIT 20
            """;

    static final String SQL_ATTENDANCE = """
            SELECT a.id, a.checked_in_at, a.source
            FROM attendance a
            WHERE a.customer_id = ?
            ORDER BY a.checked_in_at DESC
            LIMIT 20
            """;

    static final String SQL_COUNT_BOOKINGS = "SELECT COUNT(*) FROM bookings WHERE customer_id = ?";
    static final String SQL_VISITS_30D =
            "SELECT COUNT(*) FROM attendance WHERE customer_id = ? AND checked_in_at >= NOW() - INTERVAL '30 days'";
    static final String SQL_WORKOUTS_30D =
            "SELECT COUNT(*) FROM workout_sessions WHERE customer_id = ? AND started_at >= NOW() - INTERVAL '30 days'";
    static final String SQL_LAST_VISIT = "SELECT MAX(checked_in_at) FROM attendance WHERE customer_id = ?";

    private final JdbcTemplate jdbc;

    public List<BookingItem> recentBookings(Long customerId) {
        return safe("bookings", List.of(), () -> jdbc.query(SQL_BOOKINGS, (rs, i) -> new BookingItem(
                rs.getLong("id"), rs.getString("class_name"), ts(rs, "start_time"), rs.getString("status"),
                ts(rs, "created_at"), null), customerId));
    }

    public List<WorkoutItem> recentWorkouts(Long customerId) {
        return safe("workout_sessions", List.of(), () -> jdbc.query(SQL_WORKOUTS, (rs, i) -> new WorkoutItem(
                rs.getLong("id"), rs.getString("title"), ts(rs, "started_at"), ts(rs, "completed_at"),
                rs.getString("status")), customerId));
    }

    public List<AttendanceItem> recentAttendance(Long customerId) {
        return safe("attendance", List.of(), () -> jdbc.query(SQL_ATTENDANCE, (rs, i) -> new AttendanceItem(
                rs.getLong("id"), ts(rs, "checked_in_at"), rs.getString("source")), customerId));
    }

    public long countBookings(Long customerId) {
        return safe("bookings", 0L, () -> count(SQL_COUNT_BOOKINGS, customerId));
    }

    public long visitsLast30Days(Long customerId) {
        return safe("attendance", 0L, () -> count(SQL_VISITS_30D, customerId));
    }

    public long workoutsLast30Days(Long customerId) {
        return safe("workout_sessions", 0L, () -> count(SQL_WORKOUTS_30D, customerId));
    }

    public OffsetDateTime lastVisit(Long customerId) {
        return safe("attendance", null, () -> {
            Timestamp t = jdbc.queryForObject(SQL_LAST_VISIT, Timestamp.class, customerId);
            return t == null ? null : t.toInstant().atOffset(ZoneOffset.UTC);
        });
    }

    private long count(String sql, Long customerId) {
        Long n = jdbc.queryForObject(sql, Long.class, customerId);
        return n == null ? 0 : n;
    }

    private static OffsetDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toInstant().atOffset(ZoneOffset.UTC);
    }

    private <T> T safe(String module, T fallback, Supplier<T> query) {
        try {
            return query.get();
        } catch (DataAccessException e) {
            log.warn("Customer 360: cannot read {} ({}). Returning empty.", module, e.getMostSpecificCause().getMessage());
            return fallback;
        }
    }
}
