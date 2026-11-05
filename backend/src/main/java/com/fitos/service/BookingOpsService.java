package com.fitos.service;

import com.fitos.dto.common.PageResponse;
import com.fitos.dto.ops.OpsDtos.BookingResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Booking;
import com.fitos.model.Customer;
import com.fitos.repository.BookingRepository;
import com.fitos.repository.CustomerRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/**
 * Task #50: danh sách đặt chỗ (quầy lễ tân + hội viên).
 * Task #51: huỷ / đánh dấu no-show.
 * Việc tạo booking vẫn đi qua {@link BookingService} để giữ nguyên khóa bi quan.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingOpsService {

    private static final String SELECT_BASE = """
            SELECT b.id, b.customer_id, cu.full_name AS customer_name, b.session_id, cl.name AS class_name,
                   cs.session_date, cs.start_time, b.status, b.booked_at,
                   EXISTS (SELECT 1 FROM attendance a WHERE a.booking_id = b.id) AS checked_in
            FROM bookings b
            JOIN customers cu ON cu.id = b.customer_id AND cu.studio_id = b.studio_id
            JOIN class_sessions cs ON cs.id = b.session_id AND cs.studio_id = b.studio_id
            JOIN classes cl ON cl.id = cs.class_id AND cl.studio_id = b.studio_id
            WHERE b.studio_id = ?
            """;

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbc;

    private final RowMapper<BookingResponse> rowMapper = (rs, rowNum) -> {
        java.sql.Date sessionDate = rs.getDate("session_date");
        java.sql.Time start = rs.getTime("start_time");
        Timestamp bookedAt = rs.getTimestamp("booked_at");
        return new BookingResponse(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                rs.getLong("session_id"),
                rs.getString("class_name"),
                sessionDate.toLocalDate(),
                start.toLocalTime(),
                rs.getString("status"),
                bookedAt == null ? null : bookedAt.toInstant(),
                rs.getBoolean("checked_in"));
    };

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> list(String status, int page, int size) {
        Long studioId = StudioContext.currentStudioId();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String where = (status == null || status.isBlank()) ? "" : " AND b.status = ?";
        String countSql = "SELECT COUNT(*) FROM bookings b WHERE b.studio_id = ?" +
                ((status == null || status.isBlank()) ? "" : " AND b.status = ?");

        Long counted = (status == null || status.isBlank())
                ? jdbc.queryForObject(countSql, Long.class, studioId)
                : jdbc.queryForObject(countSql, Long.class, studioId, status);
        long total = counted == null ? 0L : counted;

        String sql = SELECT_BASE + where + " ORDER BY b.booked_at DESC LIMIT ? OFFSET ?";
        List<BookingResponse> rows = (status == null || status.isBlank())
                ? jdbc.query(sql, rowMapper, studioId, safeSize, safePage * safeSize)
                : jdbc.query(sql, rowMapper, studioId, status, safeSize, safePage * safeSize);
        return PageResponse.of(rows, safePage, safeSize, total);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(Long userId) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ hội viên"));
        return jdbc.query(SELECT_BASE + " AND b.customer_id = ? ORDER BY b.booked_at DESC",
                rowMapper, studioId, customer.getId());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> bySession(Long sessionId) {
        return jdbc.query(SELECT_BASE + " AND b.session_id = ? ORDER BY b.booked_at ASC",
                rowMapper, StudioContext.currentStudioId(), sessionId);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long bookingId) {
        return findOne(StudioContext.currentStudioId(), bookingId);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, String reason) {
        Long studioId = StudioContext.currentStudioId();
        Booking booking = bookingRepository.findByIdAndStudioId(bookingId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt chỗ: " + bookingId));
        if ("NO_SHOW".equals(booking.getStatus()) || "COMPLETED".equals(booking.getStatus())) {
            throw new BadRequestException("Không huỷ được lượt đặt chỗ ở trạng thái " + booking.getStatus());
        }
        bookingService.cancelBooking(bookingId, reason);
        return findOne(studioId, bookingId);
    }

    @Transactional
    public BookingResponse markNoShow(Long bookingId) {
        Long studioId = StudioContext.currentStudioId();
        Booking booking = bookingRepository.findByIdAndStudioId(bookingId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt chỗ: " + bookingId));
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new BadRequestException("Chỉ đánh dấu no-show cho lượt đặt đang CONFIRMED");
        }
        booking.setStatus("NO_SHOW");
        bookingRepository.save(booking);
        log.info("Đánh dấu no-show cho booking {}", bookingId);
        return findOne(studioId, bookingId);
    }

    @Transactional
    public BookingResponse checkInBooking(Long bookingId) {
        Long studioId = StudioContext.currentStudioId();
        Booking booking = bookingRepository.findByIdAndStudioId(bookingId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt chỗ: " + bookingId));
        return findOne(studioId, booking.getId());
    }

    /** Số lượt đặt theo ngày — dùng cho banner thống kê nhanh ở trang bookings. */
    @Transactional(readOnly = true)
    public long countForDate(LocalDate date) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM bookings b " +
                        "JOIN class_sessions cs ON cs.id = b.session_id AND cs.studio_id = b.studio_id " +
                        "WHERE b.studio_id = ? AND cs.session_date = ?",
                Long.class, StudioContext.currentStudioId(), java.sql.Date.valueOf(date));
        return count == null ? 0 : count;
    }

    private BookingResponse findOne(Long studioId, Long bookingId) {
        return jdbc.query(SELECT_BASE + " AND b.id = ?", rowMapper, studioId, bookingId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt chỗ: " + bookingId));
    }
}
