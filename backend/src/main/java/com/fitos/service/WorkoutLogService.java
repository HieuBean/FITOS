package com.fitos.service;

import com.fitos.dto.WorkoutDto;
import com.fitos.exception.BusinessException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.*;
import com.fitos.repository.*;
import com.fitos.security.UserPrincipal;
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
    private final CurrentMemberService currentMember;

    /**
     * Bắt đầu buổi tập cho một khách hàng đã được xác thực từ danh tính đăng nhập
     * (không nhận {@code customerId} từ client).
     */
    @Transactional
    public WorkoutDto.SessionResponse startSession(Customer customer, WorkoutDto.StartSessionRequest req) {
        WorkoutPlan plan = null;
        if (req != null && req.getWorkoutPlanId() != null) {
            plan = planRepository.findById(req.getWorkoutPlanId())
                    .filter(candidate -> candidate.getCustomer() != null
                            && customer.getId().equals(candidate.getCustomer().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy giáo án thuộc hội viên này: " + req.getWorkoutPlanId()));
        } else {
            plan = planRepository.findActiveByCustomerId(customer.getId()).orElse(null);
        }

        WorkoutSession session = WorkoutSession.builder()
                .customer(customer)
                .workoutPlan(plan)
                .startedAt(LocalDateTime.now())
                .status("IN_PROGRESS")
                .notes(req != null ? req.getNotes() : null)
                .logs(new ArrayList<>())
                .build();

        return mapToSessionResponse(sessionRepository.save(session));
    }

    /** Kiểm tra buổi tập có thuộc về người đang đăng nhập (hoặc người đó là nhân viên). */
    private WorkoutSession requireOwnedSession(Long sessionId, UserPrincipal principal) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found: " + sessionId));
        if (currentMember.isStaff(principal)) {
            return session;
        }
        Customer owner = currentMember.require(principal);
        if (session.getCustomer() == null || !owner.getId().equals(session.getCustomer().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền truy cập buổi tập này");
        }
        return session;
    }

    @Transactional
    public WorkoutDto.SessionResponse completeSession(Long sessionId, WorkoutDto.CompleteSessionRequest req,
                                                      UserPrincipal principal) {
        WorkoutSession session = requireOwnedSession(sessionId, principal);

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
    public WorkoutDto.SessionResponse getSessionById(Long sessionId, UserPrincipal principal) {
        return mapToSessionResponse(requireOwnedSession(sessionId, principal));
    }

    @Transactional
    public WorkoutDto.LogResponse createLog(WorkoutDto.CreateLogRequest req, UserPrincipal principal) {
        WorkoutSession session = requireOwnedSession(req.getWorkoutSessionId(), principal);

        Exercise exercise = exerciseRepository.findById(req.getExerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found: " + req.getExerciseId()));

        // Chặn ghi set trùng số hiệu trong cùng buổi (sửa tay ở client rất dễ sinh trùng).
        if (req.getSetNumber() == null || req.getSetNumber() < 1) {
            throw new com.fitos.exception.BadRequestException("Số hiệu set phải lớn hơn 0");
        }
        if (logRepository.existsByWorkoutSessionIdAndExerciseIdAndSetNumber(
                session.getId(), exercise.getId(), req.getSetNumber())) {
            throw new com.fitos.exception.BadRequestException(
                    "Set #" + req.getSetNumber() + " của bài " + exercise.getName() + " đã được ghi trước đó");
        }
        if (req.getRpe() != null && (req.getRpe() < 1 || req.getRpe() > 10)) {
            throw new com.fitos.exception.BadRequestException("RPE phải trong khoảng 1 đến 10");
        }
        if (req.getWeightKg() != null && req.getWeightKg().signum() < 0) {
            throw new com.fitos.exception.BadRequestException("Mức tạ không được âm");
        }

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
    public List<WorkoutDto.LogResponse> getLogsBySession(Long sessionId, UserPrincipal principal) {
        requireOwnedSession(sessionId, principal);
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

