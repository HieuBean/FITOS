package com.fitos.service;

import com.fitos.dto.WorkoutDto;
import com.fitos.model.Customer;
import com.fitos.model.Exercise;
import com.fitos.model.Trainer;
import com.fitos.model.WorkoutPlan;
import com.fitos.model.WorkoutPlanExercise;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.ExerciseRepository;
import com.fitos.repository.TrainerRepository;
import com.fitos.repository.WorkoutPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Layer 6 - WorkoutPlanService Comprehensive Deep Test Suite")
public class WorkoutPlanServiceDeepTest {

    @Mock
    private WorkoutPlanRepository workoutPlanRepo;
    @Mock
    private CustomerRepository customerRepo;
    @Mock
    private TrainerRepository trainerRepo;
    @Mock
    private ExerciseRepository exerciseRepo;

    @InjectMocks
    private WorkoutPlanService workoutPlanService;

    private Customer customer;
    private Trainer trainer;
    private Exercise benchPress;
    private Exercise squat;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Customer Test").email("cust@fitos.vn").build();
        trainer = Trainer.builder().id(2L).name("PT Hoang").email("hoang@fitos.vn").specialty("HYPERTROPHY").build();
        benchPress = Exercise.builder().id(10L).name("Bench Press").muscleGroup("CHEST").build();
        squat = Exercise.builder().id(20L).name("Squat").muscleGroup("LEGS").build();
    }

    @Test
    @DisplayName("Create Plan - Fails when Customer not found")
    void testCreate_customerNotFound_shouldThrow() {
        WorkoutDto.CreatePlanRequest req = WorkoutDto.CreatePlanRequest.builder()
                .customerId(999L)
                .name("Missing Cust Plan")
                .build();
        when(customerRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workoutPlanService.create(req));
        verify(workoutPlanRepo, never()).save(any());
    }

    @Test
    @DisplayName("Create Plan - Successfully maps Trainer and multiple exercise items")
    void testCreate_withTrainerAndExercises_success() {
        WorkoutDto.CreatePlanRequest req = WorkoutDto.CreatePlanRequest.builder()
                .customerId(1L)
                .trainerId(2L)
                .name("Push Pull Legs")
                .goal("HYPERTROPHY")
                .description("Target chest & legs")
                .startDate(LocalDate.now())
                .exercises(List.of(
                        WorkoutDto.PlanExerciseItem.builder().exerciseId(10L).dayOfWeek(1).sets(4).reps(10).build(),
                        WorkoutDto.PlanExerciseItem.builder().exerciseId(20L).dayOfWeek(3).sets(5).reps(8).build()
                ))
                .build();

        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(trainerRepo.findById(2L)).thenReturn(Optional.of(trainer));
        when(exerciseRepo.findById(10L)).thenReturn(Optional.of(benchPress));
        when(exerciseRepo.findById(20L)).thenReturn(Optional.of(squat));
        when(workoutPlanRepo.save(any(WorkoutPlan.class))).thenAnswer(inv -> {
            WorkoutPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        WorkoutDto.PlanResponse res = workoutPlanService.create(req);
        assertNotNull(res);
        assertEquals(100L, res.getId());
        assertEquals("DRAFT", res.getStatus());
        assertEquals(2, res.getExercises().size());
        assertEquals(1L, res.getCustomerId());
        assertEquals(2L, res.getTrainerId());
    }

    @Test
    @DisplayName("Activate Plan - Fails when Plan ID not found")
    void testActivate_planNotFound_shouldThrow() {
        when(workoutPlanRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> workoutPlanService.activate(999L));
    }

    @Test
    @DisplayName("Activate Plan - Deactivates prior active plan and sets start date")
    void testActivate_priorActivePlan_deactivated() {
        WorkoutPlan currentActive = WorkoutPlan.builder().id(50L).customer(customer).status("ACTIVE").build();
        WorkoutPlan targetPlan = WorkoutPlan.builder().id(51L).customer(customer).status("DRAFT").exercises(new ArrayList<>()).build();

        when(workoutPlanRepo.findById(51L)).thenReturn(Optional.of(targetPlan));
        when(workoutPlanRepo.findActiveByCustomerId(1L)).thenReturn(Optional.of(currentActive));
        when(workoutPlanRepo.save(any(WorkoutPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkoutDto.PlanResponse res = workoutPlanService.activate(51L);

        assertEquals("ACTIVE", res.getStatus());
        assertEquals("CANCELLED", currentActive.getStatus());
        assertNotNull(targetPlan.getStartDate());
    }

    @Test
    @DisplayName("Update Plan - Successfully updates plan metadata and modifies exercise list")
    void testUpdate_planMetadataAndExercises() {
        WorkoutPlan existing = WorkoutPlan.builder()
                .id(70L)
                .customer(customer)
                .name("Old Plan")
                .goal("FAT_LOSS")
                .exercises(new ArrayList<>())
                .build();

        WorkoutDto.UpdatePlanRequest updateReq = WorkoutDto.UpdatePlanRequest.builder()
                .name("Updated Plan Name")
                .goal("MUSCLE_GAIN")
                .description("New Description")
                .exercises(List.of(
                        WorkoutDto.PlanExerciseItem.builder().exerciseId(10L).dayOfWeek(2).sets(3).reps(12).build()
                ))
                .build();

        when(workoutPlanRepo.findById(70L)).thenReturn(Optional.of(existing));
        when(exerciseRepo.findById(10L)).thenReturn(Optional.of(benchPress));
        when(workoutPlanRepo.save(any(WorkoutPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkoutDto.PlanResponse updated = workoutPlanService.update(70L, updateReq);
        assertEquals("Updated Plan Name", updated.getName());
        assertEquals("MUSCLE_GAIN", updated.getGoal());
        assertEquals(1, updated.getExercises().size());
        assertEquals("Bench Press", updated.getExercises().get(0).getExerciseName());
    }
}
