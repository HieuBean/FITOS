package com.fitos.controller;

import com.fitos.repository.CustomerRepository;
import com.fitos.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/member")
public class MemberActivityController {
    private final JdbcTemplate jdbc;
    private final CustomerRepository customers;

    public MemberActivityController(JdbcTemplate jdbc, CustomerRepository customers) {
        this.jdbc = jdbc;
        this.customers = customers;
    }

    @GetMapping("/activity")
    @PreAuthorize("hasRole('MEMBER')")
    public List<MemberActivity> recentActivity(@AuthenticationPrincipal UserPrincipal principal) {
        var customer = customers.findByUserIdAndStudioId(principal.getId(), principal.getStudioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
        return jdbc.query("""
                SELECT kind, title, occurred_at FROM (
                    SELECT 'CHECK_IN' AS kind,
                           COALESCE('Điểm danh lớp ' || c.name, 'Điểm danh tại studio') AS title,
                           a.checkin_time AS occurred_at
                    FROM attendance a
                    LEFT JOIN class_sessions cs ON cs.id = a.class_session_id AND cs.studio_id = a.studio_id
                    LEFT JOIN classes c ON c.id = cs.class_id AND c.studio_id = a.studio_id
                    WHERE a.customer_id = ? AND a.studio_id = ?
                    UNION ALL
                    SELECT 'WORKOUT' AS kind, 'Hoàn thành buổi tập' AS title, ws.end_time AS occurred_at
                    FROM workout_sessions ws
                    WHERE ws.customer_id = ? AND ws.studio_id = ? AND ws.end_time IS NOT NULL
                ) events
                ORDER BY occurred_at DESC
                LIMIT 10
                """, (rs, row) -> new MemberActivity(
                rs.getString("kind"), rs.getString("title"), rs.getTimestamp("occurred_at").toInstant()),
                customer.getId(), principal.getStudioId(), customer.getId(), principal.getStudioId());
    }

    public record MemberActivity(String kind, String title, Instant occurredAt) {}
}
