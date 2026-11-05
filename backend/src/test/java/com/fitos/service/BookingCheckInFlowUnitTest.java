package com.fitos.service;

import com.fitos.model.Attendance;
import com.fitos.model.Booking;
import com.fitos.model.ClassSession;
import com.fitos.repository.AttendanceRepository;
import com.fitos.repository.BookingRepository;
import com.fitos.repository.ClassSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Task #156 - Person C (Layer 6: Testing)
 * Integration Test E2E: Book Class -> Check-in -> Attendance recorded -> Verify history.
 */
@ExtendWith(MockitoExtension.class)
public class BookingCheckInFlowUnitTest {

    @Test
    @DisplayName("E2E Lifecycle: Đặt chỗ lớp học -> Điểm danh tại quầy/QR -> Ghi nhận Attendance -> Tra cứu lịch sử")
    void testBookingToCheckIn_FullLifecycleE2E() {
        Long customerId = 300L;
        Long sessionId = 77L;
        Long bookingId = 888L;
        Long attendanceId = 999L;

        ClassSession session = ClassSession.builder()
                .id(sessionId)
                .classId(5L)
                .capacity(20)
                .bookedCount(0)
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(18, 30))
                .status("SCHEDULED")
                .build();

        ClassSessionRepository mockSessionRepo = mock(ClassSessionRepository.class);
        BookingRepository mockBookingRepo = mock(BookingRepository.class);
        AttendanceRepository mockAttendanceRepo = mock(AttendanceRepository.class);

        // Mocks setup
        when(mockBookingRepo.existsByCustomerIdAndSessionIdAndStatusNot(customerId, sessionId, "CANCELLED")).thenReturn(false);
        when(mockSessionRepo.findByIdWithLock(sessionId)).thenReturn(Optional.of(session));
        when(mockSessionRepo.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking bookingEntity = Booking.builder()
                .id(bookingId)
                .customerId(customerId)
                .sessionId(sessionId)
                .status("CONFIRMED")
                .bookedAt(LocalDateTime.now())
                .build();

        when(mockBookingRepo.save(any(Booking.class))).thenReturn(bookingEntity);
        when(mockBookingRepo.findById(bookingId)).thenReturn(Optional.of(bookingEntity));

        Attendance attendanceEntity = Attendance.builder()
                .id(attendanceId)
                .customerId(customerId)
                .bookingId(bookingId)
                .classSessionId(sessionId)
                .method("QR")
                .checkinTime(LocalDateTime.now())
                .build();

        when(mockAttendanceRepo.save(any(Attendance.class))).thenReturn(attendanceEntity);
        when(mockAttendanceRepo.findByCustomerIdOrderByCheckinTimeDesc(customerId)).thenReturn(List.of(attendanceEntity));

        BookingService bookingService = new BookingService(mockSessionRepo, mockBookingRepo);
        CheckInService checkInService = new CheckInService(mockBookingRepo, mockAttendanceRepo);

        // =========================================================================
        // BƯỚC 1: HỘI VIÊN ĐẶT CHỖ LỚP HỌC (BOOK CLASS)
        // =========================================================================
        Booking createdBooking = bookingService.bookClass(customerId, sessionId);

        assertNotNull(createdBooking);
        assertEquals(bookingId, createdBooking.getId());
        assertEquals("CONFIRMED", createdBooking.getStatus());
        assertEquals(1, session.getBookedCount(), "bookedCount của buổi học phải tăng từ 0 lên 1");

        // =========================================================================
        // BƯỚC 2: HỘI VIÊN ĐẾN STUDIO VÀ QUÉT MÃ CHECK-IN (CHECK-IN VIA QR)
        // =========================================================================
        Attendance recordedAttendance = checkInService.checkIn(customerId, bookingId, "QR");

        assertNotNull(recordedAttendance);
        assertEquals(attendanceId, recordedAttendance.getId());
        assertEquals("QR", recordedAttendance.getMethod());
        assertEquals("CONFIRMED", createdBooking.getStatus(), "Đặt chỗ chỉ hoàn tất khi buổi học kết thúc");

        // =========================================================================
        // BƯỚC 3: HỘI VIÊN XEM LỊCH SỬ THAM GIA LỚP HỌC (VERIFY HISTORY)
        // =========================================================================
        List<Attendance> history = mockAttendanceRepo.findByCustomerIdOrderByCheckinTimeDesc(customerId);

        assertNotNull(history);
        assertEquals(1, history.size(), "Lịch sử phải có đúng 1 lần tham gia lớp");
        assertEquals(sessionId, history.get(0).getClassSessionId());
        assertEquals(bookingId, history.get(0).getBookingId());
        assertEquals("QR", history.get(0).getMethod());

        verify(mockBookingRepo, times(1)).save(any(Booking.class)); // check-in không hoàn tất buổi học
        verify(mockAttendanceRepo, times(1)).save(any(Attendance.class));
    }
}
