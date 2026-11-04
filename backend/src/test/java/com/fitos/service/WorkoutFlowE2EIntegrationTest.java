package com.fitos.service;

import com.fitos.dto.WorkoutDto;
import com.fitos.model.Customer;
import com.fitos.model.Exercise;
import com.fitos.model.WorkoutLog;
import com.fitos.model.WorkoutPlan;
import com.fitos.model.WorkoutSession;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.ExerciseRepository;
import com.fitos.repository.WorkoutLogRepository;
import com.fitos.repository.WorkoutPlanRepository;
import com.fitos.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Task 160: Integration test E2E flow:
 * Create Workout Plan -> Start Session -> Log Exercise -> Progress computed.
 */
@ExtendWith(MockitoExtension.class)
public class WorkoutFlowE2EIntegrationTest {

    @Mock
    private WorkoutPlanRepository workoutPlanRepo;
    @Mock
    private WorkoutSessionRepository sessionRepo;
    @Mock
    private WorkoutLogRepository logRepo;
    @Mock
    private ExerciseRepository exerciseRepo;
    @Mock
    private CustomerRepository customerRepo;

    @InjectMocks
    private WorkoutPlanService workoutPlanService;

    @InjectMocks
    private WorkoutLogService workoutLogService;

    private Customer customer;
    private Exercise benchPress;
    private Exercise squat;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(100L).name("Le Hoang Nam").email("nam@fitos.vn").build();
        benchPress = Exercise.builder().id(1L).name("Barbell Bench Press").muscleGroup("CHEST").difficulty("INTERMEDIATE").build();
        squat = Exercise.builder().id(2L).name("Barbell Squat").muscleGroup("LEGS").difficulty("INTERMEDIATE").build();
    }

    @Test
    void testFullWorkoutE2EFlow_CreatePlan_StartSession_LogSets_ComputeProgress() {
        // --- STEP 1: Create Workout Plan ---
        WorkoutDto.CreatePlanRequest planReq = WorkoutDto.CreatePlanRequest.builder()
                .customerId(100L)
                .name("8-Week Strength & Hypertrophy")
                .goal("STRENGTH")
                .description("Chương trình tập tăng sức mạnh cơ bản")
                .exercises(List.of(
                        WorkoutDto.PlanExerciseItem.builder()
                                .exerciseId(1L)
                                .dayOfWeek(1)
                                .sets(4)
                                .reps(8)
                                .restSecs(90)
                                .build(),
                        WorkoutDto.PlanExerciseItem.builder()
                                .exerciseId(2L)
                                .dayOfWeek(3)
                                .sets(5)
                                .reps(5)
                                .restSecs(120)
                                .build()
                ))
                .build();

        when(customerRepo.findById(100L)).thenReturn(Optional.of(customer));
        when(exerciseRepo.findById(1L)).thenReturn(Optional.of(benchPress));
        when(exerciseRepo.findById(2L)).thenReturn(Optional.of(squat));
        when(workoutPlanRepo.save(any(WorkoutPlan.class))).thenAnswer(inv -> {
            WorkoutPlan p = inv.getArgument(0);
            p.setId(777L);
            return p;
        });

        WorkoutDto.PlanResponse createdPlan = workoutPlanService.create(planReq);
        assertNotNull(createdPlan);
        assertEquals(777L, createdPlan.getId());
        assertEquals("DRAFT", createdPlan.getStatus());
        assertEquals(2, createdPlan.getExercises().size());

        // --- STEP 2: Activate Plan ---
        WorkoutPlan draftPlanEntity = WorkoutPlan.builder()
                .id(777L)
                .customer(customer)
                .status("DRAFT")
                .exercises(new ArrayList<>())
                .build();
        when(workoutPlanRepo.findById(777L)).thenReturn(Optional.of(draftPlanEntity));
        when(workoutPlanRepo.findActiveByCustomerId(100L)).thenReturn(Optional.empty());

        WorkoutDto.PlanResponse activatedPlan = workoutPlanService.activate(777L);
        assertEquals("ACTIVE", activatedPlan.getStatus());

        // --- STEP 3: Start Session ---
        WorkoutSession newSession = WorkoutSession.builder()
                .id(888L)
                .customer(customer)
                .workoutPlan(draftPlanEntity)
                .startedAt(LocalDateTime.now())
                .status("IN_PROGRESS")
                .logs(new ArrayList<>())
                .build();

        when(sessionRepo.findById(888L)).thenReturn(Optional.of(newSession));

        // --- STEP 4: Log Exercise Sets ---
        WorkoutDto.CreateLogRequest logSet1 = WorkoutDto.CreateLogRequest.builder()
                .workoutSessionId(888L)
                .exerciseId(1L)
                .setNumber(1)
                .repsActual(8)
                .weightKg(new BigDecimal("80.0"))
                .rpe(8)
                .build();

        WorkoutLog logEntity1 = WorkoutLog.builder()
                .id(901L)
                .workoutSession(newSession)
                .exercise(benchPress)
                .setNumber(1)
                .repsActual(8)
                .weightKg(new BigDecimal("80.0"))
                .rpe(8)
                .build();

        when(logRepo.save(any(WorkoutLog.class))).thenReturn(logEntity1);
        WorkoutDto.LogResponse logRes1 = workoutLogService.createLog(logSet1);
        assertNotNull(logRes1);
        assertEquals(1L, logRes1.getExerciseId());
        assertEquals("Barbell Bench Press", logRes1.getExerciseName());
        assertEquals(8, logRes1.getRepsActual());

        // --- STEP 5: Complete Session & Calculate Progress ---
        newSession.setStatus("COMPLETED");
        newSession.setCompletedAt(LocalDateTime.now().plusMinutes(50));
        newSession.setTotalDurationSecs(3000);
        newSession.setCaloriesBurned(380);
        newSession.getLogs().add(logEntity1);

        // Verify Streak Calculation (e.g., today session)
        int streak = workoutLogService.calculateStreak(List.of(newSession));
        assertEquals(1, streak, "Active session today should result in a streak of 1 day");

        // Verify E2E integrity
        verify(workoutPlanRepo, atLeastOnce()).save(any(WorkoutPlan.class));
        verify(logRepo, times(1)).save(any(WorkoutLog.class));
    }
}
