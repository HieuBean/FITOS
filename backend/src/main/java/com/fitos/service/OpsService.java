package com.fitos.service;

import com.fitos.dto.ops.OpsDtos.ClassRequest;
import com.fitos.dto.ops.OpsDtos.ClassResponse;
import com.fitos.dto.ops.OpsDtos.ExerciseRequest;
import com.fitos.dto.ops.OpsDtos.ExerciseResponse;
import com.fitos.dto.ops.OpsDtos.SessionRequest;
import com.fitos.dto.ops.OpsDtos.SessionResponse;
import com.fitos.dto.ops.OpsDtos.TrainerRequest;
import com.fitos.dto.ops.OpsDtos.TrainerResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.ClassSession;
import com.fitos.model.Exercise;
import com.fitos.model.StudioClass;
import com.fitos.model.Trainer;
import com.fitos.model.TrainerAssignment;
import com.fitos.repository.ClassSessionRepository;
import com.fitos.repository.ExerciseRepository;
import com.fitos.repository.StudioClassRepository;
import com.fitos.repository.TrainerAssignmentRepository;
import com.fitos.repository.TrainerRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;

/**
 * Task #47, #48, #58, #45: CRUD lớp học, buổi học, huấn luyện viên và bài tập.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpsService {

    private static final String SESSION_SELECT = """
            SELECT cs.id, cs.class_id, c.name AS class_name, c.category, c.color_code,
                   cs.trainer_id, t.full_name AS trainer_name, cs.session_date, cs.start_time, cs.end_time,
                   cs.room, cs.capacity, cs.booked_count, cs.status
            FROM class_sessions cs
            JOIN classes c ON c.id = cs.class_id AND c.studio_id = cs.studio_id
            LEFT JOIN trainers t ON t.id = cs.trainer_id AND t.studio_id = cs.studio_id
            WHERE cs.studio_id = ?
            """;

    private final StudioClassRepository classRepository;
    private final ClassSessionRepository sessionRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerAssignmentRepository assignmentRepository;
    private final ExerciseRepository exerciseRepository;
    private final JdbcTemplate jdbc;

    private final RowMapper<SessionResponse> sessionMapper = (rs, rowNum) -> {
        Date sessionDate = rs.getDate("session_date");
        Time start = rs.getTime("start_time");
        Time end = rs.getTime("end_time");
        int capacity = rs.getInt("capacity");
        int booked = rs.getInt("booked_count");
        Long trainerId = rs.getObject("trainer_id", Long.class);
        return new SessionResponse(
                rs.getLong("id"),
                rs.getLong("class_id"),
                rs.getString("class_name"),
                rs.getString("category"),
                rs.getString("color_code"),
                trainerId,
                rs.getString("trainer_name"),
                sessionDate.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                rs.getString("room"),
                capacity,
                booked,
                Math.max(0, capacity - booked),
                rs.getString("status"));
    };

    // ------------------------------------------------------------------ classes
    @Transactional(readOnly = true)
    public List<ClassResponse> listClasses(boolean activeOnly) {
        Long studioId = StudioContext.currentStudioId();
        return (activeOnly ? classRepository.findByStudioIdAndIsActiveTrueOrderByNameAsc(studioId)
                : classRepository.findByStudioIdOrderByNameAsc(studioId))
                .stream().map(this::toClassResponse).toList();
    }

    @Transactional
    public ClassResponse createClass(ClassRequest req) {
        StudioClass studioClass = StudioClass.builder()
                .studioId(StudioContext.currentStudioId())
                .name(req.name().trim())
                .category(req.category())
                .description(req.description())
                .defaultCapacity(req.defaultCapacity() == null ? 20 : req.defaultCapacity())
                .colorCode(req.colorCode() == null ? "#D7FF3F" : req.colorCode())
                .isActive(req.isActive() == null || req.isActive())
                .build();
        return toClassResponse(classRepository.save(studioClass));
    }

    @Transactional
    public ClassResponse updateClass(Long id, ClassRequest req) {
        StudioClass studioClass = classRepository.findByIdAndStudioId(id, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học: " + id));
        studioClass.setName(req.name().trim());
        studioClass.setCategory(req.category());
        studioClass.setDescription(req.description());
        if (req.defaultCapacity() != null) {
            studioClass.setDefaultCapacity(req.defaultCapacity());
        }
        if (req.colorCode() != null) {
            studioClass.setColorCode(req.colorCode());
        }
        if (req.isActive() != null) {
            studioClass.setIsActive(req.isActive());
        }
        return toClassResponse(classRepository.save(studioClass));
    }

    // ----------------------------------------------------------------- sessions
    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(LocalDate from, LocalDate to) {
        Long studioId = StudioContext.currentStudioId();
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusDays(6) : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("Khoảng ngày không hợp lệ");
        }
        return jdbc.query(SESSION_SELECT + " AND cs.session_date BETWEEN ? AND ? " +
                "ORDER BY cs.session_date, cs.start_time", sessionMapper, studioId, Date.valueOf(start), Date.valueOf(end));
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> sessionsOfClass(Long classId) {
        Long studioId = StudioContext.currentStudioId();
        return jdbc.query(SESSION_SELECT + " AND cs.class_id = ? ORDER BY cs.session_date DESC, cs.start_time DESC",
                sessionMapper, studioId, classId);
    }

    @Transactional
    public SessionResponse createSession(SessionRequest req) {
        Long studioId = StudioContext.currentStudioId();
        StudioClass studioClass = classRepository.findByIdAndStudioId(req.classId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học: " + req.classId()));
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BadRequestException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (req.trainerId() != null) {
            trainerRepository.findByIdAndStudioId(req.trainerId(), studioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy huấn luyện viên: " + req.trainerId()));
        }
        ClassSession session = ClassSession.builder()
                .studioId(studioId)
                .classId(studioClass.getId())
                .trainerId(req.trainerId())
                .sessionDate(req.sessionDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .room(req.room() == null ? "Studio 1" : req.room())
                .capacity(req.capacity() == null ? studioClass.getDefaultCapacity() : req.capacity())
                .bookedCount(0)
                .status(req.status() == null ? "SCHEDULED" : req.status())
                .build();
        session = sessionRepository.save(session);
        return jdbc.query(SESSION_SELECT + " AND cs.id = ?", sessionMapper, studioId, session.getId()).get(0);
    }

    @Transactional
    public SessionResponse updateSession(Long id, SessionRequest req) {
        Long studioId = StudioContext.currentStudioId();
        ClassSession session = sessionRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học: " + id));
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BadRequestException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (req.capacity() != null && req.capacity() < session.getBookedCount()) {
            throw new BadRequestException("Sức chứa không được nhỏ hơn số chỗ đã đặt (" + session.getBookedCount() + ")");
        }
        session.setClassId(req.classId());
        session.setTrainerId(req.trainerId());
        session.setSessionDate(req.sessionDate());
        session.setStartTime(req.startTime());
        session.setEndTime(req.endTime());
        if (req.room() != null) {
            session.setRoom(req.room());
        }
        if (req.capacity() != null) {
            session.setCapacity(req.capacity());
        }
        if (req.status() != null) {
            session.setStatus(req.status());
        }
        sessionRepository.save(session);
        return jdbc.query(SESSION_SELECT + " AND cs.id = ?", sessionMapper, studioId, id).get(0);
    }

    @Transactional
    public SessionResponse cancelSession(Long id) {
        Long studioId = StudioContext.currentStudioId();
        ClassSession session = sessionRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học: " + id));
        if ("COMPLETED".equals(session.getStatus())) {
            throw new BadRequestException("Không huỷ được buổi học đã hoàn thành");
        }
        session.setStatus("CANCELLED");
        sessionRepository.save(session);
        return jdbc.query(SESSION_SELECT + " AND cs.id = ?", sessionMapper, studioId, id).get(0);
    }

    // ----------------------------------------------------------------- trainers
    @Transactional(readOnly = true)
    public List<TrainerResponse> listTrainers(boolean activeOnly) {
        Long studioId = StudioContext.currentStudioId();
        return (activeOnly ? trainerRepository.findByStudioIdAndIsActiveTrueOrderByFullNameAsc(studioId)
                : trainerRepository.findByStudioIdOrderByFullNameAsc(studioId))
                .stream().map(trainer -> toTrainerResponse(studioId, trainer)).toList();
    }

    @Transactional
    public TrainerResponse createTrainer(TrainerRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Trainer trainer = Trainer.builder()
                .studioId(studioId)
                .fullName(req.fullName().trim())
                .phone(req.phone().trim())
                .email(req.email())
                .specialization(req.specialization())
                .bio(req.bio())
                .experienceYears(req.experienceYears() == null ? 1 : req.experienceYears())
                .rating(req.rating() == null ? new java.math.BigDecimal("5.00") : req.rating())
                .isActive(req.isActive() == null || req.isActive())
                .build();
        return toTrainerResponse(studioId, trainerRepository.save(trainer));
    }

    @Transactional
    public TrainerResponse updateTrainer(Long id, TrainerRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Trainer trainer = trainerRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy huấn luyện viên: " + id));
        trainer.setFullName(req.fullName().trim());
        trainer.setPhone(req.phone().trim());
        trainer.setEmail(req.email());
        trainer.setSpecialization(req.specialization());
        trainer.setBio(req.bio());
        if (req.experienceYears() != null) {
            trainer.setExperienceYears(req.experienceYears());
        }
        if (req.rating() != null) {
            trainer.setRating(req.rating());
        }
        if (req.isActive() != null) {
            trainer.setIsActive(req.isActive());
        }
        return toTrainerResponse(studioId, trainerRepository.save(trainer));
    }

    @Transactional
    public void assignCustomer(Long trainerId, Long customerId) {
        Long studioId = StudioContext.currentStudioId();
        trainerRepository.findByIdAndStudioId(trainerId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy huấn luyện viên: " + trainerId));
        if (assignmentRepository.existsByTrainerIdAndCustomerIdAndStatus(trainerId, customerId, "ACTIVE")) {
            throw new BadRequestException("Hội viên đã được gán cho huấn luyện viên này");
        }
        assignmentRepository.save(TrainerAssignment.builder()
                .studioId(studioId)
                .trainerId(trainerId)
                .customerId(customerId)
                .status("ACTIVE")
                .build());
    }

    // ---------------------------------------------------------------- exercises
    @Transactional(readOnly = true)
    public List<ExerciseResponse> listExercises(String category) {
        List<Exercise> exercises = (category == null || category.isBlank())
                ? exerciseRepository.findAllByOrderByNameAsc()
                : exerciseRepository.findByCategoryIgnoreCaseOrderByNameAsc(category);
        return exercises.stream().map(this::toExerciseResponse).toList();
    }

    @Transactional
    public ExerciseResponse createExercise(ExerciseRequest req) {
        if (exerciseRepository.existsByNameIgnoreCase(req.name())) {
            throw new BadRequestException("Bài tập đã tồn tại: " + req.name());
        }
        Exercise exercise = Exercise.builder()
                .name(req.name().trim())
                .category(req.category())
                .muscleGroup(req.muscleGroup())
                .equipment(req.equipment())
                .difficulty(req.difficulty() == null ? "INTERMEDIATE" : req.difficulty())
                .instruction(req.instruction())
                .videoUrl(req.videoUrl())
                .build();
        return toExerciseResponse(exerciseRepository.save(exercise));
    }

    @Transactional
    public ExerciseResponse updateExercise(Long id, ExerciseRequest req) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập: " + id));
        exercise.setName(req.name().trim());
        exercise.setCategory(req.category());
        exercise.setMuscleGroup(req.muscleGroup());
        exercise.setEquipment(req.equipment());
        if (req.difficulty() != null) {
            exercise.setDifficulty(req.difficulty());
        }
        exercise.setInstruction(req.instruction());
        exercise.setVideoUrl(req.videoUrl());
        return toExerciseResponse(exerciseRepository.save(exercise));
    }

    // ------------------------------------------------------------------ mapping
    private ClassResponse toClassResponse(StudioClass studioClass) {
        return new ClassResponse(studioClass.getId(), studioClass.getName(), studioClass.getCategory(),
                studioClass.getDescription(), studioClass.getDefaultCapacity(), studioClass.getColorCode(),
                studioClass.getIsActive());
    }

    private TrainerResponse toTrainerResponse(Long studioId, Trainer trainer) {
        List<Long> customerIds = assignmentRepository
                .findByTrainerIdAndStudioIdOrderByAssignedDateDesc(trainer.getId(), studioId).stream()
                .filter(assignment -> "ACTIVE".equals(assignment.getStatus()))
                .map(TrainerAssignment::getCustomerId)
                .toList();
        return new TrainerResponse(trainer.getId(), trainer.getUserId(), trainer.getFullName(), trainer.getPhone(),
                trainer.getEmail(), trainer.getSpecialization(), trainer.getExperienceYears(), trainer.getRating(),
                trainer.getIsActive(), customerIds);
    }

    private ExerciseResponse toExerciseResponse(Exercise exercise) {
        return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getCategory(),
                exercise.getMuscleGroup(), exercise.getEquipment(), exercise.getDifficulty(),
                exercise.getInstruction(), exercise.getVideoUrl());
    }
}
