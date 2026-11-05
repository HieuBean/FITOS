package com.fitos.service;

import com.fitos.model.PtPackage;
import com.fitos.model.PtSession;
import com.fitos.repository.PtPackageRepository;
import com.fitos.repository.PtSessionRepository;
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
 * Task #155 - Person C (Layer 6: Testing) — Pair-testing với Người D (Cross-testing)
 * Unit test PT complete idempotent:
 * Gọi complete 2 lần với cùng 1 completionRef duy nhất -> chỉ trừ đúng 1 buổi tập, không bị trừ đúp.
 */
@ExtendWith(MockitoExtension.class)
public class PtSessionIdempotencyTest {

    @Mock
    private PtSessionRepository ptSessionRepository;

    @Mock
    private PtPackageRepository ptPackageRepository;

    @InjectMocks
    private PtSessionService ptSessionService;

    private PtSession mockPtSession;
    private PtPackage mockPtPackage;
    private final String TEST_COMPLETION_REF = "COMP-TX-2026-X889";

    @BeforeEach
    void setUp() {
        mockPtPackage = PtPackage.builder()
                .id(1L)
                .customerId(50L)
                .totalSessions(10)
                .remainingSessions(10)
                .status("ACTIVE")
                .build();

        mockPtSession = PtSession.builder()
                .id(88L)
                .trainerId(2L)
                .customerId(50L)
                .ptPackageId(1L)
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .status("SCHEDULED")
                .build();
    }

    @Test
    @DisplayName("Idempotency: Gọi hoàn thành buổi tập PT 2 lần với cùng completionRef -> Chỉ trừ 1 lần")
    void testCompletePtSession_IdempotentBehavior() {
        Long sessionId = 88L;

        // Lần gọi 1: completionRef chưa tồn tại trong DB
        when(ptSessionRepository.findByCompletionRef(TEST_COMPLETION_REF))
                .thenReturn(Optional.empty()) // Lần 1: chưa có
                .thenReturn(Optional.of(mockPtSession)); // Lần 2: đã có

        when(ptSessionRepository.findByIdWithLock(sessionId)).thenReturn(Optional.of(mockPtSession));
        when(ptSessionRepository.save(any(PtSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ptPackageRepository.findById(1L)).thenReturn(Optional.of(mockPtPackage));
        when(ptPackageRepository.save(any(PtPackage.class))).thenAnswer(inv -> inv.getArgument(0));

        // ==========================================
        // 1. GỌI LẦN THỨ NHẤT (FIRST INVOCATION)
        // ==========================================
        PtSession firstCall = ptSessionService.completePtSession(sessionId, TEST_COMPLETION_REF);

        assertNotNull(firstCall);
        assertEquals("COMPLETED", firstCall.getStatus());
        assertEquals(TEST_COMPLETION_REF, firstCall.getCompletionRef());
        assertEquals(9, mockPtPackage.getRemainingSessions(), "Lần gọi 1: số buổi tập còn lại phải giảm từ 10 xuống 9");

        // ==========================================
        // 2. GỌI LẦN THỨ HAI (IDEMPOTENT RETRY)
        // ==========================================
        PtSession secondCall = ptSessionService.completePtSession(sessionId, TEST_COMPLETION_REF);

        assertNotNull(secondCall);
        assertEquals("COMPLETED", secondCall.getStatus());
        assertEquals(TEST_COMPLETION_REF, secondCall.getCompletionRef());
        
        // KIỂM CHỨNG: Số buổi còn lại VẪN LÀ 9 (Tuyệt đối không bị trừ lần thứ 2)
        assertEquals(9, mockPtPackage.getRemainingSessions(), "Lần gọi 2: số buổi còn lại vẫn là 9, không bị trừ trùng");

        // ptPackageRepository.save chỉ được gọi DUY NHẤT 1 LẦN
        verify(ptPackageRepository, times(1)).save(any(PtPackage.class));
    }

}
