package com.fitos.service;

import com.fitos.model.Attendance;
import com.fitos.model.Booking;
import com.fitos.repository.AttendanceRepository;
import com.fitos.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {

    private final BookingRepository bookingRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * Task #54 & Task #156: Điểm danh vào lớp học (Check-in via Desk/QR)
     */
    @Transactional
    public Attendance checkIn(Long customerId, Long bookingId, String method) {
        log.info("Processing check-in: customerId={}, bookingId={}, method={}", customerId, bookingId, method);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đặt chỗ: " + bookingId));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Thông tin đặt chỗ không khớp với hội viên");
        }

        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new IllegalStateException("Chỉ có thể điểm danh lượt đặt chỗ đã xác nhận");
        }
        if (attendanceRepository.existsByBookingId(bookingId)) {
            throw new IllegalStateException("Lượt đặt chỗ đã được điểm danh");
        }

        // Tạo bản ghi điểm danh (Attendance Record)
        Attendance attendance = Attendance.builder()
                .studioId(booking.getStudioId())
                .customerId(customerId)
                .bookingId(bookingId)
                .classSessionId(booking.getSessionId())
                .method(method != null ? method : "DESK")
                .checkinTime(LocalDateTime.now())
                .build();

        return attendanceRepository.save(attendance);
    }
}
