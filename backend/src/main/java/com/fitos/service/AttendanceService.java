package com.fitos.service;

import com.fitos.dto.common.PageResponse;
import com.fitos.dto.ops.OpsDtos.AttendanceResponse;
import com.fitos.dto.ops.OpsDtos.CheckInRequest;
import com.fitos.dto.ops.OpsDtos.CheckInResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Attendance;
import com.fitos.model.Booking;
import com.fitos.model.Customer;
import com.fitos.repository.AttendanceRepository;
import com.fitos.repository.BookingRepository;
import com.fitos.repository.CustomerRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Task #52 và #53: điểm danh tại quầy (có kiểm tra gói còn hiệu lực) + tra cứu lịch sử điểm danh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final CheckInService checkInService;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final AttendanceRepository attendanceRepository;
    private final MembershipValidator membershipValidator;

    @Transactional
    public CheckInResponse checkIn(CheckInRequest req, Long actorUserId) {
        Long studioId = StudioContext.currentStudioId();
        Booking booking = null;
        Long customerId = req.customerId();

        if (req.bookingId() != null) {
            booking = bookingRepository.findByIdAndStudioId(req.bookingId(), studioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt chỗ: " + req.bookingId()));
            customerId = booking.getCustomerId();
        }
        if (customerId == null) {
            throw new BadRequestException("Cần cung cấp bookingId hoặc customerId để điểm danh");
        }
        final Long resolvedCustomerId = customerId;
        Customer customer = customerRepository.findByIdAndStudioId(resolvedCustomerId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội viên: " + resolvedCustomerId));

        // Task #52: chỉ cho vào phòng khi gói còn hiệu lực
        String membershipStatus = membershipValidator.statusOf(resolvedCustomerId, studioId);
        membershipValidator.requireActive(resolvedCustomerId, studioId);

        String method = req.method() == null ? "DESK" : req.method();
        Attendance attendance;
        if (booking != null) {
            attendance = checkInService.checkIn(customerId, booking.getId(), method);
            if (req.notes() != null && !req.notes().isBlank()) {
                attendance.setNotes(req.notes());
                attendance = attendanceRepository.save(attendance);
            }
        } else {
            attendance = attendanceRepository.save(Attendance.builder()
                    .studioId(studioId)
                    .customerId(customerId)
                    .method(method)
                    .checkinTime(LocalDateTime.now())
                    .checkedInBy(actorUserId)
                    .notes(req.notes())
                    .build());
        }
        log.info("Check-in {} cho hội viên {} ({}), method={}", attendance.getId(), customerId,
                membershipStatus, method);

        return new CheckInResponse(attendance.getId(), customerId, customer.getFullName(),
                attendance.getBookingId(), attendance.getCheckinTime(), attendance.getMethod(), membershipStatus);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> recent(int page, int size) {
        Long studioId = StudioContext.currentStudioId();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Attendance> result = attendanceRepository.findByStudioIdOrderByCheckinTimeDesc(
                studioId, PageRequest.of(safePage, safeSize));

        List<AttendanceResponse> content = result.getContent().stream()
                .map(attendance -> new AttendanceResponse(
                        attendance.getId(),
                        attendance.getCustomerId(),
                        customerRepository.findByIdAndStudioId(attendance.getCustomerId(), studioId)
                                .map(Customer::getFullName).orElse(null),
                        attendance.getBookingId(),
                        attendance.getClassSessionId(),
                        attendance.getCheckinTime(),
                        attendance.getMethod(),
                        attendance.getNotes()))
                .toList();
        return PageResponse.of(content, safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> mine(Long userId) {
        Long studioId = StudioContext.currentStudioId();
        Customer customer = customerRepository.findByUserIdAndStudioId(userId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ hội viên"));
        return attendanceRepository.findByCustomerIdOrderByCheckinTimeDesc(customer.getId()).stream()
                .map(attendance -> new AttendanceResponse(attendance.getId(), attendance.getCustomerId(),
                        customer.getFullName(), attendance.getBookingId(), attendance.getClassSessionId(),
                        attendance.getCheckinTime(), attendance.getMethod(), attendance.getNotes()))
                .toList();
    }

    static Timestamp toTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value);
    }
}
