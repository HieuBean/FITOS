package com.fitos.service;

import com.fitos.model.Booking;
import com.fitos.model.ClassSession;
import com.fitos.repository.BookingRepository;
import com.fitos.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final ClassSessionRepository classSessionRepository;
    private final BookingRepository bookingRepository;

    /**
     * Task #51 & Task #153, #154:
     * Đặt chỗ lớp học với cơ chế Concurrency Locking (SELECT FOR UPDATE) và kiểm tra Duplicate / Capacity.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking bookClass(Long customerId, Long sessionId) {
        log.info("Processing class booking: customerId={}, sessionId={}", customerId, sessionId);

        // 1. Spec 7.6: Ngăn chặn hội viên book trùng 2 lần trên cùng 1 session
        if (bookingRepository.existsByCustomerIdAndSessionIdAndStatusNot(customerId, sessionId, "CANCELLED")) {
            throw new IllegalStateException("Hội viên đã đặt chỗ trong lớp học này (Duplicate booking not allowed)");
        }

        // 2. Task #51: Đọc session với PESSIMISTIC_WRITE lock (SELECT FOR UPDATE)
        ClassSession session = classSessionRepository.findByIdWithLock(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học với ID: " + sessionId));

        // 3. Spec 7.5: Kiểm tra sức chứa lớp học (Capacity check)
        if (session.getBookedCount() >= session.getCapacity()) {
            throw new IllegalStateException("Lớp học đã hết chỗ (Full capacity: " + session.getCapacity() + ")");
        }

        // 3b. Không cho đặt lớp đã huỷ hoặc đã bắt đầu/qua giờ.
        //     Trước đây hệ thống vẫn nhận đặt cho buổi 06:00 khi đã 12:30, và job
        //     no-show lập tức đánh dấu NO_SHOW — hội viên mất lượt mà không tập được.
        if (!"SCHEDULED".equalsIgnoreCase(session.getStatus())) {
            throw new IllegalStateException("Buổi học này không còn nhận đặt chỗ (trạng thái: " + session.getStatus() + ")");
        }
        LocalDateTime sessionStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        if (!sessionStart.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Buổi học này đã bắt đầu hoặc đã kết thúc, không thể đặt chỗ");
        }

        // 4. Tăng số lượng đã đặt (Atomic increment)
        session.setBookedCount(session.getBookedCount() + 1);
        classSessionRepository.save(session);

        // 5. Tạo bản ghi Booking
        Booking booking = Booking.builder()
                .studioId(session.getStudioId())
                .customerId(customerId)
                .sessionId(sessionId)
                .status("CONFIRMED")
                .bookedAt(LocalDateTime.now())
                .build();

        return bookingRepository.save(booking);
    }

    /**
     * Hủy đặt chỗ lớp học và hoàn trả slot (Decrement bookedCount)
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt đặt chỗ: " + bookingId));

        if ("CANCELLED".equals(booking.getStatus())) {
            return booking;
        }

        booking.setStatus("CANCELLED");
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelReason(reason);

        // Hoàn trả slot buổi học
        classSessionRepository.findByIdWithLock(booking.getSessionId()).ifPresent(session -> {
            if (session.getBookedCount() > 0) {
                session.setBookedCount(session.getBookedCount() - 1);
                classSessionRepository.save(session);
            }
        });

        return bookingRepository.save(booking);
    }
}
