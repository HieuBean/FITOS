package com.fitos.service;

import com.fitos.dto.WorkoutDto;
import com.fitos.model.*;
import com.fitos.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkoutLogServiceTest {

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
    private Exercise exercise;
    private WorkoutSession session;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Tran Van C").build();
        exercise = Exercise.builder().id(10L).name("Bench Press").muscleGroup("CHEST").build();
        session = WorkoutSession.builder().id(100L).customer(customer).startedAt(LocalDateTime.now().minusHours(1)).status("IN_PROGRESS").build();
    }

    @Test
    void testCreateLog_success() {
        WorkoutDto.CreateLogRequest req = WorkoutDto.CreateLogRequest.builder()
                .workoutSessionId(100L)
                .exerciseId(10L)
                .setNumber(1)
                .repsActual(12)
                .weightKg(new BigDecimal("60.0"))
                .rpe(8)
                .notes("Good form")
                .build();

        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));
        when(exerciseRepo.findById(10L)).thenReturn(Optional.of(exercise));
        when(logRepo.save(any(WorkoutLog.class))).thenAnswer(invocation -> {
            WorkoutLog l = invocation.getArgument(0);
            l.setId(500L);
            return l;
        });

        WorkoutDto.LogResponse res = workoutLogService.createLog(req);

        assertNotNull(res.getId());
        assertEquals(10L, res.getExerciseId());
        assertEquals("Bench Press", res.getExerciseName());
        assertEquals(12, res.getRepsActual());
        assertEquals(new BigDecimal("60.0"), res.getWeightKg());
        verify(logRepo, times(1)).save(any(WorkoutLog.class));
    }

    @Test
    void testCalculateStreak_consecutiveDays() {
        LocalDate today = LocalDate.now();
        List<WorkoutSession> sessions = List.of(
                WorkoutSession.builder().startedAt(today.atTime(8, 0)).build(),
                WorkoutSession.builder().startedAt(today.minusDays(1).atTime(9, 0)).build(),
                WorkoutSession.builder().startedAt(today.minusDays(2).atTime(18, 0)).build(),
                WorkoutSession.builder().startedAt(today.minusDays(3).atTime(17, 0)).build()
        );

        int streak = workoutLogService.calculateStreak(sessions);
        assertEquals(4, streak);
    }

    @Test
    void testCalculateStreak_emptySessions() {
        int streak = workoutLogService.calculateStreak(new ArrayList<>());
        assertEquals(0, streak);
    }
}

