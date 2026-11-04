package com.fitos.service;

import com.fitos.dto.WorkoutDto;
import com.fitos.model.Customer;
import com.fitos.model.Exercise;
import com.fitos.model.WorkoutLog;
import com.fitos.model.WorkoutSession;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.ExerciseRepository;
import com.fitos.repository.WorkoutLogRepository;
import com.fitos.repository.WorkoutPlanRepository;
import com.fitos.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Layer 6 - WorkoutLogService Deep Edge-Cases & Metric Test Suite")
public class WorkoutLogServiceDeepTest {

    @Mock
    private WorkoutSessionRepository sessionRepo;
    @Mock
    private WorkoutLogRepository logRepo;
    @Mock
    private WorkoutPlanRepository planRepo;
    @Mock
    private CustomerRepository customerRepo;
    @Mock
    private ExerciseRepository exerciseRepo;

    @InjectMocks
    private WorkoutLogService workoutLogService;

    private Customer customer;
    private Exercise benchPress;
    private WorkoutSession session;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Customer Edge").build();
        benchPress = Exercise.builder().id(10L).name("Bench Press").muscleGroup("CHEST").build();
        session = WorkoutSession.builder().id(100L).customer(customer).startedAt(LocalDateTime.now().minusHours(1)).status("IN_PROGRESS").build();
    }

    @Test
    @DisplayName("Create Log - Fails when WorkoutSession not found")
    void testCreateLog_missingSession_throwsException() {
        WorkoutDto.CreateLogRequest req = WorkoutDto.CreateLogRequest.builder()
                .workoutSessionId(999L)
                .exerciseId(10L)
                .setNumber(1)
                .repsActual(10)
                .build();
        when(sessionRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workoutLogService.createLog(req));
        verify(logRepo, never()).save(any());
    }

    @Test
    @DisplayName("Create Log - Fails when Exercise not found")
    void testCreateLog_missingExercise_throwsException() {
        WorkoutDto.CreateLogRequest req = WorkoutDto.CreateLogRequest.builder()
                .workoutSessionId(100L)
                .exerciseId(999L)
                .setNumber(1)
                .repsActual(10)
                .build();
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));
        when(exerciseRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workoutLogService.createLog(req));
        verify(logRepo, never()).save(any());
    }

    @Test
    @DisplayName("Calculate Streak - Empty session list returns 0")
    void testCalculateStreak_emptyList_returnsZero() {
        assertEquals(0, workoutLogService.calculateStreak(Collections.emptyList()));
        assertEquals(0, workoutLogService.calculateStreak(null));
    }

    @Test
    @DisplayName("Calculate Streak - Session today only returns 1")
    void testCalculateStreak_onlyToday_returnsOne() {
        List<WorkoutSession> list = List.of(
                WorkoutSession.builder().startedAt(LocalDate.now().atTime(10, 0)).build()
        );
        assertEquals(1, workoutLogService.calculateStreak(list));
    }

    @Test
    @DisplayName("Calculate Streak - Gap in attendance breaks streak")
    void testCalculateStreak_withGap_streakBreaks() {
        LocalDate today = LocalDate.now();
        // Today, Yesterday, and 4 days ago (gap at day 2 & 3 ago)
        List<WorkoutSession> list = List.of(
                WorkoutSession.builder().startedAt(today.atTime(8, 0)).build(),
                WorkoutSession.builder().startedAt(today.minusDays(1).atTime(8, 0)).build(),
                WorkoutSession.builder().startedAt(today.minusDays(4).atTime(8, 0)).build()
        );
        assertEquals(2, workoutLogService.calculateStreak(list));
    }

    @Test
    @DisplayName("Calculate Streak - Missed today and yesterday returns 0")
    void testCalculateStreak_missedRecentDays_returnsZero() {
        LocalDate today = LocalDate.now();
        List<WorkoutSession> list = List.of(
                WorkoutSession.builder().startedAt(today.minusDays(2).atTime(10, 0)).build(),
                WorkoutSession.builder().startedAt(today.minusDays(3).atTime(10, 0)).build()
        );
        assertEquals(0, workoutLogService.calculateStreak(list));
    }

    @Test
    @DisplayName("Personal Records - Computes max weight and max reps across all exercise logs")
    void testGetProgressSummary_personalRecordsComputed() {
        WorkoutLog log1 = WorkoutLog.builder()
                .exercise(benchPress)
                .weightKg(new BigDecimal("90.0"))
                .repsActual(5)
                .build();
        WorkoutLog log2 = WorkoutLog.builder()
                .exercise(benchPress)
                .weightKg(new BigDecimal("100.0"))
                .repsActual(3)
                .build();
        WorkoutLog log3 = WorkoutLog.builder()
                .exercise(benchPress)
                .weightKg(new BigDecimal("80.0"))
                .repsActual(12)
                .build();

        session.setLogs(List.of(log1, log2, log3));
        session.setStatus("COMPLETED");

        when(sessionRepo.findCompletedSessionsByCustomer(1L)).thenReturn(List.of(session));
        when(logRepo.findCustomerLogsByWeightDesc(1L)).thenReturn(List.of(log2, log1, log3));

        WorkoutDto.ProgressSummaryResponse progress = workoutLogService.getProgress(1L);

        assertNotNull(progress);
        assertEquals(1L, progress.getCustomerId());
        assertEquals(1, progress.getTotalSessionsCompleted());
        assertEquals(1, progress.getPersonalRecords().size());

        WorkoutDto.PersonalRecordItem pr = progress.getPersonalRecords().get(0);
        assertEquals(10L, pr.getExerciseId());
        assertEquals("Bench Press", pr.getExerciseName());
        assertEquals(new BigDecimal("100.0"), pr.getMaxWeightKg(), "Max weight should be 100.0 kg");
        assertEquals(3, pr.getMaxReps());
    }
}
