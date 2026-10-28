package com.fitos.trainer;

import com.fitos.trainer.dto.TrainerScheduleDtos.ScheduleSlot;
import com.fitos.trainer.dto.TrainerScheduleDtos.SessionType;
import com.fitos.trainer.dto.TrainerScheduleDtos.TrainerScheduleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Task #46: GET /api/trainers/:id/schedule
 * Queries class_sessions and pt_sessions for trainer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainerScheduleService {

    private final JdbcTemplate jdbc;

    static final String SQL_TRAINER_NAME = "SELECT full_name FROM trainers WHERE id = ?";

    static final String SQL_CLASS_SESSIONS = """
            SELECT cs.id, c.name AS title, cs.start_time, cs.end_time, cs.status, cs.capacity, cs.booked_count
            FROM class_sessions cs
            JOIN classes c ON c.id = cs.class_id
            WHERE cs.trainer_id = ?
              AND cs.start_time >= ? AND cs.start_time < ?
            ORDER BY cs.start_time ASC
            """;

    static final String SQL_PT_SESSIONS = """
            SELECT ps.id, 'Personal Training' AS title, ps.start_time, ps.end_time, ps.status,
                   ps.customer_id, c.full_name AS customer_name
            FROM pt_sessions ps
            LEFT JOIN customers c ON c.id = ps.customer_id
            WHERE ps.trainer_id = ?
              AND ps.start_time >= ? AND ps.start_time < ?
            ORDER BY ps.start_time ASC
            """;

    public TrainerScheduleResponse getSchedule(Long trainerId, LocalDate from, LocalDate to) {
        String trainerName = "Trainer #" + trainerId;
        try {
            String name = jdbc.queryForObject(SQL_TRAINER_NAME, String.class, trainerId);
            if (name != null) trainerName = name;
        } catch (DataAccessException e) {
            log.debug("Could not read trainers table: {}", e.getMessage());
        }

        OffsetDateTime start = (from != null ? from : LocalDate.now()).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = (to != null ? to : (from != null ? from.plusDays(7) : LocalDate.now().plusDays(7)))
                .plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<ScheduleSlot> slots = new ArrayList<>();

        // Fetch class sessions
        try {
            List<ScheduleSlot> classSlots = jdbc.query(SQL_CLASS_SESSIONS, (rs, i) -> new ScheduleSlot(
                    rs.getLong("id"),
                    SessionType.CLASS,
                    rs.getString("title"),
                    ts(rs, "start_time"),
                    ts(rs, "end_time"),
                    rs.getString("status"),
                    rs.getInt("capacity"),
                    rs.getInt("booked_count"),
                    null,
                    null
            ), trainerId, Timestamp.from(start.toInstant()), Timestamp.from(end.toInstant()));
            slots.addAll(classSlots);
        } catch (DataAccessException e) {
            log.warn("Cannot read class_sessions for trainer {}: {}", trainerId, e.getMessage());
        }

        // Fetch PT sessions
        try {
            List<ScheduleSlot> ptSlots = jdbc.query(SQL_PT_SESSIONS, (rs, i) -> new ScheduleSlot(
                    rs.getLong("id"),
                    SessionType.PT,
                    rs.getString("title"),
                    ts(rs, "start_time"),
                    ts(rs, "end_time"),
                    rs.getString("status"),
                    1,
                    1,
                    rs.getLong("customer_id"),
                    rs.getString("customer_name")
            ), trainerId, Timestamp.from(start.toInstant()), Timestamp.from(end.toInstant()));
            slots.addAll(ptSlots);
        } catch (DataAccessException e) {
            log.warn("Cannot read pt_sessions for trainer {}: {}", trainerId, e.getMessage());
        }

        slots.sort(Comparator.comparing(ScheduleSlot::startTime));
        return new TrainerScheduleResponse(trainerId, trainerName, slots);
    }

    private static OffsetDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toInstant().atOffset(ZoneOffset.UTC);
    }
}
