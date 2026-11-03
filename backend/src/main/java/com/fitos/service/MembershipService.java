package com.fitos.service;

import com.fitos.dto.commerce.MembershipDtos.MembershipResponse;
import com.fitos.dto.commerce.MembershipDtos.RenewRequest;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.model.Membership;
import com.fitos.model.ServicePlan;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.MembershipRepository;
import com.fitos.repository.ServicePlanRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Task #42: danh sách / pause / resume / cancel membership.
 * Task #43: gia hạn + danh sách sắp hết hạn (nền cho widget Renewal Risk).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private static final String SELECT_BASE = """
            SELECT m.id, m.customer_id, c.full_name AS customer_name, m.plan_id, p.name AS plan_name,
                   m.start_date, m.end_date, m.status, m.paused_at, m.pause_days_used, m.auto_renew, m.created_at
            FROM memberships m
            JOIN customers c ON c.id = m.customer_id AND c.studio_id = m.studio_id
            JOIN service_plans p ON p.id = m.plan_id AND p.studio_id = m.studio_id
            WHERE m.studio_id = ?
            """;

    private final MembershipRepository membershipRepository;
    private final ServicePlanRepository planRepository;
    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbc;

    private final RowMapper<MembershipResponse> rowMapper = (rs, rowNum) -> {
        LocalDate endDate = rs.getDate("end_date").toLocalDate();
        Date pausedAt = rs.getDate("paused_at");
        String status = rs.getString("status");
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        String risk = riskLevel(status, daysRemaining);
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new MembershipResponse(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                rs.getLong("plan_id"),
                rs.getString("plan_name"),
                rs.getDate("start_date").toLocalDate(),
                endDate,
                status,
                daysRemaining,
                pausedAt == null ? null : pausedAt.toLocalDate(),
                rs.getInt("pause_days_used"),
                rs.getBoolean("auto_renew"),
                risk,
                riskReason(status, daysRemaining),
                createdAt == null ? null : createdAt.toInstant());
    };

    @Transactional
    public List<MembershipResponse> list(String status) {
        Long studioId = StudioContext.currentStudioId();
        expireOutdated(studioId);
        if (status == null || status.isBlank()) {
            return jdbc.query(SELECT_BASE + " ORDER BY m.end_date ASC", rowMapper, studioId);
        }
        return jdbc.query(SELECT_BASE + " AND m.status = ? ORDER BY m.end_date ASC", rowMapper, studioId, status);
    }

    @Transactional
    public List<MembershipResponse> expiring(int days) {
        Long studioId = StudioContext.currentStudioId();
        if (days <= 0 || days > 90) {
            throw new BadRequestException("Số ngày cảnh báo phải trong khoảng 1..90");
        }
        expireOutdated(studioId);
        LocalDate until = LocalDate.now().plusDays(days);
        return jdbc.query(SELECT_BASE + " AND m.status = 'ACTIVE' AND m.end_date BETWEEN ? AND ? " +
                "ORDER BY m.end_date ASC", rowMapper, studioId, Date.valueOf(LocalDate.now()), Date.valueOf(until));
    }

    @Transactional
    public List<MembershipResponse> mine(Long userId) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ hội viên"));
        expireOutdated(studioId);
        return jdbc.query(SELECT_BASE + " AND m.customer_id = ? ORDER BY m.end_date DESC",
                rowMapper, studioId, customer.getId());
    }

    @Transactional
    public MembershipResponse pause(Long id, boolean pause) {
        Long studioId = StudioContext.currentStudioId();
        Membership membership = membershipRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói hội viên: " + id));
        LocalDate today = LocalDate.now();

        if (pause) {
            if (!"ACTIVE".equals(membership.getStatus())) {
                throw new BadRequestException("Chỉ tạm dừng được gói đang ACTIVE (hiện tại: " + membership.getStatus() + ")");
            }
            membership.setStatus("PAUSED");
            membership.setPausedAt(today);
        } else {
            if (!"PAUSED".equals(membership.getStatus())) {
                throw new BadRequestException("Chỉ tiếp tục được gói đang PAUSED (hiện tại: " + membership.getStatus() + ")");
            }
            LocalDate pausedAt = membership.getPausedAt() == null ? today : membership.getPausedAt();
            long pausedDays = Math.max(0, ChronoUnit.DAYS.between(pausedAt, today));
            membership.setEndDate(membership.getEndDate().plusDays(pausedDays));
            membership.setPauseDaysUsed((membership.getPauseDaysUsed() == null ? 0 : membership.getPauseDaysUsed())
                    + (int) pausedDays);
            membership.setPausedAt(null);
            membership.setStatus("ACTIVE");
        }
        membershipRepository.save(membership);
        return findOne(studioId, membership.getId());
    }

    @Transactional
    public MembershipResponse cancel(Long id) {
        Long studioId = StudioContext.currentStudioId();
        Membership membership = membershipRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói hội viên: " + id));
        if ("CANCELLED".equals(membership.getStatus())) {
            return findOne(studioId, membership.getId());
        }
        membership.setStatus("CANCELLED");
        membership.setPausedAt(null);
        membershipRepository.save(membership);
        return findOne(studioId, membership.getId());
    }

    @Transactional
    @com.fitos.audit.Auditable(action = "RENEW_MEMBERSHIP", entity = "Membership")
    public MembershipResponse renew(Long id, RenewRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Membership membership = membershipRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói hội viên: " + id));

        Long planId = req != null && req.planId() != null ? req.planId() : membership.getPlanId();
        ServicePlan plan = planRepository.findByIdAndStudioId(planId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói dịch vụ: " + planId));

        int extraDays = req != null && req.extraDays() != null && req.extraDays() > 0
                ? req.extraDays() : plan.getDurationDays();
        LocalDate base = membership.getEndDate().isBefore(LocalDate.now()) ? LocalDate.now() : membership.getEndDate();
        membership.setEndDate(base.plusDays(extraDays));
        membership.setPlanId(plan.getId());
        if ("EXPIRED".equals(membership.getStatus()) || "CANCELLED".equals(membership.getStatus())) {
            membership.setStartDate(LocalDate.now());
        }
        membership.setStatus("ACTIVE");
        membership.setPausedAt(null);
        membershipRepository.save(membership);
        log.info("Gia hạn membership {} thêm {} ngày → {}", id, extraDays, membership.getEndDate());
        return findOne(studioId, membership.getId());
    }

    private MembershipResponse findOne(Long studioId, Long id) {
        return jdbc.query(SELECT_BASE + " AND m.id = ?", rowMapper, studioId, id).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói hội viên: " + id));
    }

    /** Đánh dấu EXPIRED cho các gói đã quá hạn nhưng còn ở trạng thái ACTIVE. */
    private void expireOutdated(Long studioId) {
        int updated = jdbc.update("UPDATE memberships SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP " +
                "WHERE studio_id = ? AND status = 'ACTIVE' AND end_date < ?", studioId, Date.valueOf(LocalDate.now()));
        if (updated > 0) {
            log.info("Đã chuyển {} membership sang EXPIRED", updated);
        }
    }

    static String riskLevel(String status, long daysRemaining) {
        if (!"ACTIVE".equals(status)) {
            return "HIGH";
        }
        if (daysRemaining <= 3) {
            return "HIGH";
        }
        if (daysRemaining <= 7) {
            return "MEDIUM";
        }
        return "LOW";
    }

    static String riskReason(String status, long daysRemaining) {
        if ("PAUSED".equals(status)) {
            return "Gói đang tạm dừng";
        }
        if ("EXPIRED".equals(status)) {
            return "Gói đã hết hạn";
        }
        if ("CANCELLED".equals(status)) {
            return "Gói đã huỷ";
        }
        if (daysRemaining <= 3) {
            return "Còn " + Math.max(daysRemaining, 0) + " ngày — nguy cơ rời bỏ cao";
        }
        if (daysRemaining <= 7) {
            return "Còn " + daysRemaining + " ngày — nên nhắc gia hạn";
        }
        return "Còn " + daysRemaining + " ngày";
    }

    /** Cho tầng đọc khác (vd. dashboard) dùng lại. */
    public static Instant now() {
        return Instant.now();
    }
}
