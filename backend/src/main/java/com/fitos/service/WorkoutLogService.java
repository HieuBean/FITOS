package com.fitos.service;

import com.fitos.dto.WorkoutDto;
import com.fitos.exception.BusinessException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.*;
import com.fitos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutLogService {

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutLogRepository logRepository;
    private final WorkoutPlanRepository planRepository;
    private final CustomerRepository customerRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public WorkoutDto.SessionResponse startSession(WorkoutDto.StartSessionRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + req.getCustomerId()));

        WorkoutPlan plan = null;
        if (req.getWorkoutPlanId() != null) {
            plan = planRepository.findById(req.getWorkoutPlanId()).orElse(null);
        } else {
            plan = planRepository.findActiveByCustomerId(customer.getId()).orElse(null);
        }

        WorkoutSession session = WorkoutSession.builder()
                .customer(customer)
                .workoutPlan(plan)
                .startedAt(LocalDateTime.now())
                .status("IN_PROGRESS")
                .notes(req.getNotes())
                .logs(new ArrayList<>())
                .build();

        return mapToSessionResponse(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutDto.SessionResponse completeSession(Long sessionId, WorkoutDto.CompleteSessionRequest req) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found: " + sessionId));

        if ("COMPLETED".equalsIgnoreCase(session.getStatus())) {
            return mapToSessionResponse(session);
        }

        LocalDateTime now = LocalDateTime.now();
        session.setCompletedAt(now);
        session.setStatus("COMPLETED");

        int durationSecs = (int) Duration.between(session.getStartedAt(), now).getSeconds();
        session.setTotalDurationSecs(durationSecs);

        if (req != null) {
            if (req.getCaloriesBurned() != null) {
                session.setCaloriesBurned(req.getCaloriesBurned());
            }
            if (req.getNotes() != null) {
                session.setNotes(req.getNotes());
            }
        }

        log.info("Completed Workout Session {}. Duration: {} seconds", sessionId, durationSecs);
        return mapToSessionResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto.SessionResponse> getCustomerSessions(Long customerId) {
        return sessionRepository.findByCustomerIdOrderByStartedAtDesc(customerId).stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkoutDto.SessionResponse getSessionById(Long sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found: " + sessionId));
        return mapToSessionResponse(session);
    }

    @Transactional
    public WorkoutDto.LogResponse createLog(WorkoutDto.CreateLogRequest req) {
        WorkoutSession session = sessionRepository.findById(req.getWorkoutSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Workout Session not found: " + req.getWorkoutSessionId()));

        Exercise exercise = exerciseRepository.findById(req.getExerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found: " + req.getExerciseId()));

        WorkoutLog logEntry = WorkoutLog.builder()
                .workoutSession(session)
                .exercise(exercise)
                .setNumber(req.getSetNumber())
                .repsActual(req.getRepsActual())
                .weightKg(req.getWeightKg())
                .durationSecs(req.getDurationSecs())
                .rpe(req.getRpe())
                .notes(req.getNotes())
                .loggedAt(LocalDateTime.now())
                .build();

        return mapToLogResponse(logRepository.save(logEntry));
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto.LogResponse> getLogsBySession(Long sessionId) {
        return logRepository.findByWorkoutSessionIdOrderBySetNumberAsc(sessionId).stream()
                .map(this::mapToLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tính toán Streak và thống kê tiến độ cá nhân (PRs)
     */
    @Transactional(readOnly = true)
    public WorkoutDto.ProgressSummaryResponse getProgress(Long customerId) {
        List<WorkoutSession> completedSessions = sessionRepository.findCompletedSessionsByCustomer(customerId);
        
        int streak = calculateStreak(completedSessions);
        
        LocalDateTime firstDayOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long sessionsThisMonth = completedSessions.stream()
                .filter(s -> s.getStartedAt().isAfter(firstDayOfMonth))
                .count();

        // Tính PR (Personal Record) theo từng bài tập
        List<WorkoutLog> allLogs = logRepository.findCustomerLogsByWeightDesc(customerId);
        Map<Long, WorkoutDto.PersonalRecordItem> prMap = new HashMap<>();

        for (WorkoutLog l : allLogs) {
            Long exId = l.getExercise().getId();
            if (!prMap.containsKey(exId) && l.getWeightKg() != null) {
                prMap.put(exId, WorkoutDto.PersonalRecordItem.builder()
                        .exerciseId(exId)
                        .exerciseName(l.getExercise().getName())
                        .maxWeightKg(l.getWeightKg())
                        .maxReps(l.getRepsActual())
                        .build());
            }
        }

        return WorkoutDto.ProgressSummaryResponse.builder()
                .customerId(customerId)
                .streak(streak)
                .totalSessionsCompleted(completedSessions.size())
                .sessionsThisMonth((int) sessionsThisMonth)
                .personalRecords(new ArrayList<>(prMap.values()))
                .build();
    }

    public int calculateStreak(List<WorkoutSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;

        Set<LocalDate> workoutDates = sessions.stream()
                .map(s -> s.getStartedAt().toLocalDate())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate checkDate = today;

        // Nếu hôm nay chưa đi tập, kiểm tra xem hôm qua có đi không
        if (!workoutDates.contains(checkDate)) {
            checkDate = today.minusDays(1);
        }

        while (workoutDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    private WorkoutDto.SessionResponse mapToSessionResponse(WorkoutSession s) {
        List<WorkoutDto.LogResponse> logList = s.getLogs() != null ?
                s.getLogs().stream().map(this::mapToLogResponse).collect(Collectors.toList()) : List.of();

        return WorkoutDto.SessionResponse.builder()
                .id(s.getId())
                .customerId(s.getCustomer().getId())
                .workoutPlanId(s.getWorkoutPlan() != null ? s.getWorkoutPlan().getId() : null)
                .startedAt(s.getStartedAt())
                .completedAt(s.getCompletedAt())
                .status(s.getStatus())
                .totalDurationSecs(s.getTotalDurationSecs())
                .caloriesBurned(s.getCaloriesBurned())
                .notes(s.getNotes())
                .logs(logList)
                .build();
    }

    private WorkoutDto.LogResponse mapToLogResponse(WorkoutLog l) {
        return WorkoutDto.LogResponse.builder()
                .id(l.getId())
                .exerciseId(l.getExercise().getId())
                .exerciseName(l.getExercise().getName())
                .muscleGroup(l.getExercise().getMuscleGroup())
                .setNumber(l.getSetNumber())
                .repsActual(l.getRepsActual())
                .weightKg(l.getWeightKg())
                .durationSecs(l.getDurationSecs())
                .rpe(l.getRpe())
                .notes(l.getNotes())
                .loggedAt(l.getLoggedAt())
                .build();
    }
}

