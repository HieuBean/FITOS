package com.fitos.service;

import com.fitos.dto.WorkoutDto;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.*;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.ExerciseRepository;
import com.fitos.repository.TrainerRepository;
import com.fitos.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutPlanService {

    private final WorkoutPlanRepository workoutPlanRepository;
    private final CustomerRepository customerRepository;
    private final TrainerRepository trainerRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional(readOnly = true)
    public List<WorkoutDto.PlanResponse> findAll(Long customerId) {
        if (customerId != null) {
            return getCustomerPlans(customerId);
        }
        return workoutPlanRepository.findAll().stream()
                .map(this::mapToPlanResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto.PlanResponse> getCustomerPlans(Long customerId) {
        return workoutPlanRepository.findByCustomerId(customerId).stream()
                .map(this::mapToPlanResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkoutDto.PlanResponse getById(Long planId) {
        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout Plan not found: " + planId));
        return mapToPlanResponse(plan);
    }

    @Transactional
    public WorkoutDto.PlanResponse create(WorkoutDto.CreatePlanRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + req.getCustomerId()));

        Trainer trainer = null;
        if (req.getTrainerId() != null) {
            trainer = trainerRepository.findById(req.getTrainerId()).orElse(null);
        }

        WorkoutPlan plan = WorkoutPlan.builder()
                .customer(customer)
                .trainer(trainer)
                .name(req.getName())
                .description(req.getDescription())
                .goal(req.getGoal())
                .status("DRAFT")
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .exercises(new ArrayList<>())
                .build();

        if (req.getExercises() != null) {
            for (WorkoutDto.PlanExerciseItem item : req.getExercises()) {
                Exercise exercise = exerciseRepository.findById(item.getExerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Exercise not found: " + item.getExerciseId()));

                WorkoutPlanExercise wpe = WorkoutPlanExercise.builder()
                        .workoutPlan(plan)
                        .exercise(exercise)
                        .dayOfWeek(item.getDayOfWeek())
                        .sets(item.getSets())
                        .reps(item.getReps())
                        .durationSecs(item.getDurationSecs())
                        .restSecs(item.getRestSecs() != null ? item.getRestSecs() : 60)
                        .weightSuggest(item.getWeightSuggest())
                        .notes(item.getNotes())
                        .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0)
                        .build();

                plan.getExercises().add(wpe);
            }
        }

        WorkoutPlan saved = workoutPlanRepository.save(plan);
        log.info("Created Workout Plan {} for customer {}", saved.getId(), customer.getId());
        return mapToPlanResponse(saved);
    }

    @Transactional
    public WorkoutDto.PlanResponse update(Long planId, WorkoutDto.UpdatePlanRequest req) {
        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout Plan not found: " + planId));

        if (req.getName() != null) plan.setName(req.getName());
        if (req.getDescription() != null) plan.setDescription(req.getDescription());
        if (req.getGoal() != null) plan.setGoal(req.getGoal());
        if (req.getStatus() != null) plan.setStatus(req.getStatus());
        if (req.getStartDate() != null) plan.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) plan.setEndDate(req.getEndDate());

        if (req.getExercises() != null) {
            plan.getExercises().clear();
            for (WorkoutDto.PlanExerciseItem item : req.getExercises()) {
                Exercise exercise = exerciseRepository.findById(item.getExerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Exercise not found: " + item.getExerciseId()));

                WorkoutPlanExercise wpe = WorkoutPlanExercise.builder()
                        .workoutPlan(plan)
                        .exercise(exercise)
                        .dayOfWeek(item.getDayOfWeek())
                        .sets(item.getSets())
                        .reps(item.getReps())
                        .durationSecs(item.getDurationSecs())
                        .restSecs(item.getRestSecs() != null ? item.getRestSecs() : 60)
                        .weightSuggest(item.getWeightSuggest())
                        .notes(item.getNotes())
                        .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0)
                        .build();

                plan.getExercises().add(wpe);
            }
        }

        plan.setUpdatedAt(LocalDateTime.now());
        return mapToPlanResponse(workoutPlanRepository.save(plan));
    }

    /**
     * Task 66: Kích hoạt kế hoạch tập luyện
     * Deactivates previous ACTIVE plan of this customer
     */
    @Transactional
    public WorkoutDto.PlanResponse activate(Long planId) {
        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout Plan not found: " + planId));

        // Hủy kích hoạt plan đang ACTIVE trước đó của hội viên này
        workoutPlanRepository.findActiveByCustomerId(plan.getCustomer().getId())
                .ifPresent(activePlan -> {
                    if (!activePlan.getId().equals(planId)) {
                        activePlan.setStatus("CANCELLED");
                        activePlan.setUpdatedAt(LocalDateTime.now());
                        workoutPlanRepository.save(activePlan);
                        log.info("Deactivated previous plan {}", activePlan.getId());
                    }
                });

        plan.setStatus("ACTIVE");
        plan.setStartDate(LocalDate.now());
        plan.setUpdatedAt(LocalDateTime.now());
        return mapToPlanResponse(workoutPlanRepository.save(plan));
    }

    @Transactional
    public void complete(Long planId) {
        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout Plan not found: " + planId));
        plan.setStatus("COMPLETED");
        plan.setEndDate(LocalDate.now());
        plan.setUpdatedAt(LocalDateTime.now());
        workoutPlanRepository.save(plan);
    }

    @Transactional
    public void cancel(Long planId) {
        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout Plan not found: " + planId));
        plan.setStatus("CANCELLED");
        plan.setUpdatedAt(LocalDateTime.now());
        workoutPlanRepository.save(plan);
    }

    public WorkoutDto.PlanResponse mapToPlanResponse(WorkoutPlan p) {
        List<WorkoutDto.PlanExerciseDetail> exerciseDetails = p.getExercises() != null ?
                p.getExercises().stream().map(e -> WorkoutDto.PlanExerciseDetail.builder()
                        .id(e.getId())
                        .exerciseId(e.getExercise().getId())
                        .exerciseName(e.getExercise().getName())
                        .muscleGroup(e.getExercise().getMuscleGroup())
                        .dayOfWeek(e.getDayOfWeek())
                        .sets(e.getSets())
                        .reps(e.getReps())
                        .durationSecs(e.getDurationSecs())
                        .restSecs(e.getRestSecs())
                        .weightSuggest(e.getWeightSuggest())
                        .notes(e.getNotes())
                        .build()).collect(Collectors.toList()) : List.of();

        return WorkoutDto.PlanResponse.builder()
                .id(p.getId())
                .customerId(p.getCustomer().getId())
                .trainerId(p.getTrainer() != null ? p.getTrainer().getId() : null)
                .name(p.getName())
                .description(p.getDescription())
                .goal(p.getGoal())
                .status(p.getStatus())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .exercises(exerciseDetails)
                .build();
    }
}

