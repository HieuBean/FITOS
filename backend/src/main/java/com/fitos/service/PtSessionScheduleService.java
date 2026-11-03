package com.fitos.service;

import com.fitos.dto.ops.OpsDtos.PtSessionRequest;
import com.fitos.dto.ops.OpsDtos.PtSessionResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.PtSession;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.PtSessionRepository;
import com.fitos.repository.TrainerRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Task #54, #55, #56: đặt lịch PT, kiểm tra trùng lịch của HLV, dời/huỷ và hoàn thành
 * (hoàn thành uỷ quyền cho {@link PtSessionService} để giữ cơ chế idempotent theo completion_ref).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PtSessionScheduleService {

    private static final String SELECT_BASE = """
            SELECT ps.id, ps.trainer_id, t.full_name AS trainer_name, ps.customer_id, c.full_name AS customer_name,
                   ps.pt_package_id, ps.session_date, ps.start_time, ps.end_time, ps.status,
                   ps.completion_ref, ps.training_notes
            FROM pt_sessions ps
            JOIN trainers t ON t.id = ps.trainer_id AND t.studio_id = ps.studio_id
            JOIN customers c ON c.id = ps.customer_id AND c.studio_id = ps.studio_id
            WHERE ps.studio_id = ?
            """;

    private final PtSessionRepository ptSessionRepository;
    private final PtSessionService ptSessionService;
    private final TrainerRepository trainerRepository;
    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbc;

    private final RowMapper<PtSessionResponse> rowMapper = (rs, rowNum) -> {
        Date sessionDate = rs.getDate("session_date");
        Time start = rs.getTime("start_time");
        Time end = rs.getTime("end_time");
        Long ptPackageId = rs.getObject("pt_package_id", Long.class);
        return new PtSessionResponse(
                rs.getLong("id"),
                rs.getLong("trainer_id"),
                rs.getString("trainer_name"),
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                ptPackageId,
                sessionDate.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                rs.getString("status"),
                rs.getString("completion_ref"),
                rs.getString("training_notes"));
    };

    @Transactional(readOnly = true)
    public List<PtSessionResponse> list(LocalDate from, LocalDate to, Long trainerId, Long customerId) {
        Long studioId = StudioContext.currentStudioId();
        LocalDate start = from == null ? LocalDate.now().minusDays(7) : from;
        LocalDate end = to == null ? LocalDate.now().plusDays(14) : to;
        StringBuilder sql = new StringBuilder(SELECT_BASE)
                .append(" AND ps.session_date BETWEEN ? AND ?");
        List<Object> args = new java.util.ArrayList<>(List.of(studioId, Date.valueOf(start), Date.valueOf(end)));
        if (trainerId != null) {
            sql.append(" AND ps.trainer_id = ?");
            args.add(trainerId);
        }
        if (customerId != null) {
            sql.append(" AND ps.customer_id = ?");
            args.add(customerId);
        }
        sql.append(" ORDER BY ps.session_date, ps.start_time");
        return jdbc.query(sql.toString(), rowMapper, args.toArray());
    }

    @Transactional
    public PtSessionResponse create(PtSessionRequest req) {
        Long studioId = StudioContext.currentStudioId();
        trainerRepository.findByIdAndStudioId(req.trainerId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy huấn luyện viên: " + req.trainerId()));
        customerRepository.findByIdAndStudioId(req.customerId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội viên: " + req.customerId()));
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BadRequestException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        boolean trainerBusy = countOverlaps(studioId, req.trainerId(), req.sessionDate(),
                req.startTime(), req.endTime(), null) > 0;
        if (trainerBusy) {
            throw new BadRequestException("Huấn luyện viên đã có lịch trong khung giờ này");
        }
        PtSession session = PtSession.builder()
                .trainerId(req.trainerId())
                .customerId(req.customerId())
                .ptPackageId(req.ptPackageId())
                .sessionDate(req.sessionDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .status("SCHEDULED")
                .trainingNotes(req.trainingNotes())
                .build();
        session = ptSessionRepository.save(session);
        return findOne(studioId, session.getId());
    }

    @Transactional
    public PtSessionResponse reschedule(Long id, PtSessionRequest req) {
        Long studioId = StudioContext.currentStudioId();
        PtSession session = ptSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi PT: " + id));
        if (!List.of("SCHEDULED", "IN_PROGRESS").contains(session.getStatus())) {
            throw new BadRequestException("Không dời được buổi PT ở trạng thái " + session.getStatus());
        }
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BadRequestException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (countOverlaps(studioId, session.getTrainerId(), req.sessionDate(), req.startTime(), req.endTime(), id) > 0) {
            throw new BadRequestException("Huấn luyện viên đã có lịch trong khung giờ này");
        }
        session.setSessionDate(req.sessionDate());
        session.setStartTime(req.startTime());
        session.setEndTime(req.endTime());
        if (req.trainingNotes() != null) {
            session.setTrainingNotes(req.trainingNotes());
        }
        ptSessionRepository.save(session);
        return findOne(studioId, id);
    }

    @Transactional
    public PtSessionResponse cancel(Long id) {
        Long studioId = StudioContext.currentStudioId();
        PtSession session = ptSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi PT: " + id));
        if ("COMPLETED".equals(session.getStatus())) {
            throw new BadRequestException("Không huỷ được buổi PT đã hoàn thành");
        }
        session.setStatus("CANCELLED");
        session.setUpdatedAt(java.time.LocalDateTime.now());
        ptSessionRepository.save(session);
        return findOne(studioId, id);
    }

    @Transactional
    public PtSessionResponse complete(Long id, String completionRef, String trainingNotes) {
        Long studioId = StudioContext.currentStudioId();
        ptSessionService.completePtSession(id, completionRef);
        if (trainingNotes != null && !trainingNotes.isBlank()) {
            ptSessionRepository.findById(id).ifPresent(session -> {
                session.setTrainingNotes(trainingNotes);
                ptSessionRepository.save(session);
            });
        }
        return findOne(studioId, id);
    }

    private int countOverlaps(Long studioId, Long trainerId, LocalDate date, LocalTime start, LocalTime end,
                              Long excludeId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pt_sessions WHERE studio_id = ? AND trainer_id = ? AND session_date = ? " +
                        "AND status IN ('SCHEDULED', 'IN_PROGRESS') AND start_time < ? AND end_time > ? " +
                        "AND (? IS NULL OR id <> ?)",
                Integer.class, studioId, trainerId, Date.valueOf(date), Time.valueOf(end), Time.valueOf(start),
                excludeId, excludeId);
        return count == null ? 0 : count;
    }

    private PtSessionResponse findOne(Long studioId, Long id) {
        return jdbc.query(SELECT_BASE + " AND ps.id = ?", rowMapper, studioId, id).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi PT: " + id));
    }
}
