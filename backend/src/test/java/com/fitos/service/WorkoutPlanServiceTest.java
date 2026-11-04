package com.fitos.service;

import com.fitos.dto.WorkoutDto;
import com.fitos.model.Customer;
import com.fitos.model.WorkoutPlan;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.ExerciseRepository;
import com.fitos.repository.TrainerRepository;
import com.fitos.repository.WorkoutPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkoutPlanServiceTest {

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
    private WorkoutPlan plan;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Nguyen Van A").email("a@test.com").build();
        plan = WorkoutPlan.builder()
                .id(10L)
                .customer(customer)
                .name("Hypertrophy 4 Days")
                .goal("MUSCLE_GAIN")
                .status("DRAFT")
                .exercises(new ArrayList<>())
                .build();
    }

    @Test
    void testActivate_shouldDeactivatePreviousActivePlan() {
        WorkoutPlan activePlan = WorkoutPlan.builder()
                .id(9L)
                .customer(customer)
                .status("ACTIVE")
                .build();

        when(workoutPlanRepo.findById(10L)).thenReturn(Optional.of(plan));
        when(workoutPlanRepo.findActiveByCustomerId(1L)).thenReturn(Optional.of(activePlan));
        when(workoutPlanRepo.save(any(WorkoutPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutDto.PlanResponse response = workoutPlanService.activate(10L);

        assertEquals("ACTIVE", response.getStatus());
        assertEquals(LocalDate.now(), plan.getStartDate());
        assertEquals("CANCELLED", activePlan.getStatus());
        verify(workoutPlanRepo, times(2)).save(any(WorkoutPlan.class));
    }

    @Test
    void testComplete_shouldSetCompletedStatusAndEndDate() {
        when(workoutPlanRepo.findById(10L)).thenReturn(Optional.of(plan));
        when(workoutPlanRepo.save(any(WorkoutPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workoutPlanService.complete(10L);

        assertEquals("COMPLETED", plan.getStatus());
        assertEquals(LocalDate.now(), plan.getEndDate());
        verify(workoutPlanRepo).save(plan);
    }

    @Test
    void testCancel_shouldSetCancelledStatus() {
        when(workoutPlanRepo.findById(10L)).thenReturn(Optional.of(plan));
        when(workoutPlanRepo.save(any(WorkoutPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workoutPlanService.cancel(10L);

        assertEquals("CANCELLED", plan.getStatus());
        verify(workoutPlanRepo).save(plan);
    }
}

