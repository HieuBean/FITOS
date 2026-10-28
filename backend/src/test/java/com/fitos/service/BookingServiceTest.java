package com.fitos.service;

import com.fitos.model.Booking;
import com.fitos.model.ClassSession;
import com.fitos.repository.BookingRepository;
import com.fitos.repository.ClassSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Task #153 - Person C (Layer 6: Testing)
 * Unit test BookingService: Capacity check, Duplicate booking prevention, Cancellation.
 */
@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private ClassSession mockSession;

    @BeforeEach
    void setUp() {
        mockSession = ClassSession.builder()
                .id(10L)
                .classId(1L)
                .capacity(15)
                .bookedCount(5)
                .sessionDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .status("SCHEDULED")
                .build();
    }

    @Test
    @DisplayName("Đặt chỗ thành công khi lớp còn slot và hội viên chưa đặt trước đó")
    void testBookClass_Success() {
        Long customerId = 100L;
        Long sessionId = 10L;

        when(bookingRepository.existsByCustomerIdAndSessionIdAndStatusNot(customerId, sessionId, "CANCELLED")).thenReturn(false);
        when(classSessionRepository.findByIdWithLock(sessionId)).thenReturn(Optional.of(mockSession));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        Booking result = bookingService.bookClass(customerId, sessionId);

        assertNotNull(result);
        assertEquals(500L, result.getId());
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(6, mockSession.getBookedCount(), "bookedCount phải tăng thêm 1");

        verify(classSessionRepository, times(1)).save(mockSession);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ IllegalStateException khi lớp học đã đầy (Capacity Full)")
    void testBookClass_CapacityFull_ThrowsException() {
        Long customerId = 101L;
        Long sessionId = 10L;

        // Giả lập lớp học đã đạt sức chứa tối đa (15/15)
        mockSession.setBookedCount(15);
        mockSession.setCapacity(15);

        when(bookingRepository.existsByCustomerIdAndSessionIdAndStatusNot(customerId, sessionId, "CANCELLED")).thenReturn(false);
        when(classSessionRepository.findByIdWithLock(sessionId)).thenReturn(Optional.of(mockSession));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            bookingService.bookClass(customerId, sessionId);
        });

        assertTrue(ex.getMessage().contains("Lớp học đã hết chỗ"));
        assertEquals(15, mockSession.getBookedCount(), "bookedCount không được tăng");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ IllegalStateException khi hội viên cố tình đặt trùng lần 2 (Duplicate Booking)")
    void testBookClass_DuplicateBooking_ThrowsException() {
        Long customerId = 100L;
        Long sessionId = 10L;

        // Hội viên đã có lượt đặt trước đó
        when(bookingRepository.existsByCustomerIdAndSessionIdAndStatusNot(customerId, sessionId, "CANCELLED")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            bookingService.bookClass(customerId, sessionId);
        });

        assertTrue(ex.getMessage().contains("Hội viên đã đặt chỗ trong lớp học này"));
        verify(classSessionRepository, never()).findByIdWithLock(anyLong());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Hủy đặt chỗ thành công và hoàn trả lại slot (Decrement bookedCount)")
    void testCancelBooking_Success() {
        Long bookingId = 500L;
        Booking existingBooking = Booking.builder()
                .id(bookingId)
                .customerId(100L)
                .sessionId(10L)
                .status("CONFIRMED")
                .build();

        mockSession.setBookedCount(8);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existingBooking));
        when(classSessionRepository.findByIdWithLock(10L)).thenReturn(Optional.of(mockSession));
        when(bookingRepository.save(any(Booking.class))).thenReturn(existingBooking);

        Booking cancelled = bookingService.cancelBooking(bookingId, "Bận việc đột xuất");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("Bận việc đột xuất", cancelled.getCancelReason());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals(7, mockSession.getBookedCount(), "Số lượng slot phải giảm 1 sau khi hủy");

        verify(classSessionRepository, times(1)).save(mockSession);
    }

}
