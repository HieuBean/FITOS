package com.fitos.service;

import com.fitos.model.Booking;
import com.fitos.model.ClassSession;
import com.fitos.repository.BookingRepository;
import com.fitos.repository.ClassSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Task #154 - Person C (Layer 6: Testing)
 * Concurrency Test: 2 CompletableFuture (hoặc 2 threads đồng thời) cùng tranh chấp
 * slot cuối cùng (Last slot: capacity = 1) -> ĐÚNG 1 thành công, 1 thất bại.
 * Đảm bảo tính toàn vẹn (No Overbooking / Chống đặt vượt sức chứa).
 */
@ExtendWith(MockitoExtension.class)
public class BookingConcurrencyTest {

    @Test
    @DisplayName("Concurrency: 2 luồng đồng thời đặt slot cuối cùng -> Đúng 1 thành công, 1 thất bại")
    void testConcurrentBooking_LastSlot_ExactlyOneSucceeds() throws Exception {
        Long sessionId = 99L;
        Long customer1Id = 201L;
        Long customer2Id = 202L;

        // Buổi học chỉ còn đúng 1 slot duy nhất (capacity = 1, bookedCount = 0)
        ClassSession session = ClassSession.builder()
                .id(sessionId)
                .classId(1L)
                .capacity(1)
                .bookedCount(0)
                .sessionDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .status("SCHEDULED")
                .build();

        ClassSessionRepository mockSessionRepo = mock(ClassSessionRepository.class);
        BookingRepository mockBookingRepo = mock(BookingRepository.class);

        // Giả lập cơ chế PESSIMISTIC_WRITE lock (SELECT FOR UPDATE) bằng synchronized block
        final Object dbLock = new Object();
        when(mockSessionRepo.findByIdWithLock(sessionId)).thenAnswer(inv -> {
            synchronized (dbLock) {
                // Trả về cùng một đối tượng session được đồng bộ hóa
                return Optional.of(session);
            }
        });

        when(mockSessionRepo.save(any(ClassSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockBookingRepo.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(ThreadLocalRandom.current().nextLong(1000, 9999));
            return b;
        });

        BookingService bookingService = new BookingService(mockSessionRepo, mockBookingRepo);

        // CyclicBarrier để 2 luồng cùng xuất phát tại đúng 1 thời điểm
        CyclicBarrier gate = new CyclicBarrier(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                gate.await(); // Chờ luồng 2 sẵn sàng
                bookingService.bookClass(customer1Id, sessionId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e instanceof IllegalStateException || (e.getCause() instanceof IllegalStateException)) {
                    failureCount.incrementAndGet();
                }
            }
        });

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                gate.await(); // Chờ luồng 1 sẵn sàng
                bookingService.bookClass(customer2Id, sessionId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e instanceof IllegalStateException || (e.getCause() instanceof IllegalStateException)) {
                    failureCount.incrementAndGet();
                }
            }
        });

        // Chờ cả 2 luồng hoàn thành (tối đa 5 giây)
        CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

        // KIỂM CHỨNG TÍNH TOÀN VẸN CƠ CHẾ CONCURRENCY:
        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 luồng được phép đặt thành công slot cuối cùng");
        assertEquals(1, failureCount.get(), "Luồng còn lại phải bị từ chối do hết chỗ (Full capacity)");
        assertEquals(1, session.getBookedCount(), "bookedCount cuối cùng phải là 1, tuyệt đối không được vượt quá capacity = 1");
    }
}
