package com.fitos.service;

import com.fitos.dto.ClassDto;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.ClassSession;
import com.fitos.model.FitnessClass;
import com.fitos.model.Trainer;
import com.fitos.repository.ClassSessionRepository;
import com.fitos.repository.FitnessClassRepository;
import com.fitos.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassSessionService {

    private final FitnessClassRepository classRepository;
    private final ClassSessionRepository sessionRepository;
    private final TrainerRepository trainerRepository;

    public Page<ClassDto.ClassResponse> findAllClasses(String type, Pageable pageable) {
        if (type != null && !type.isBlank()) {
            return classRepository.findByTypeIgnoreCase(type, pageable).map(this::mapToClassResponse);
        }
        return classRepository.findAll(pageable).map(this::mapToClassResponse);
    }

    public ClassDto.ClassResponse findClassById(Long id) {
        FitnessClass fc = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + id));
        return mapToClassResponse(fc);
    }

    @Transactional
    public ClassDto.ClassResponse createClass(ClassDto.CreateClassRequest req) {
        FitnessClass fc = FitnessClass.builder()
                .name(req.getName())
                .description(req.getDescription())
                .type(req.getType())
                .durationMinutes(req.getDurationMinutes())
                .maxCapacity(req.getMaxCapacity())
                .isActive(true)
                .build();
        return mapToClassResponse(classRepository.save(fc));
    }

    @Transactional
    public ClassDto.ClassResponse updateClass(Long id, ClassDto.UpdateClassRequest req) {
        FitnessClass fc = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + id));

        if (req.getName() != null) fc.setName(req.getName());
        if (req.getDescription() != null) fc.setDescription(req.getDescription());
        if (req.getType() != null) fc.setType(req.getType());
        if (req.getDurationMinutes() != null) fc.setDurationMinutes(req.getDurationMinutes());
        if (req.getMaxCapacity() != null) fc.setMaxCapacity(req.getMaxCapacity());
        if (req.getIsActive() != null) fc.setIsActive(req.getIsActive());

        return mapToClassResponse(classRepository.save(fc));
    }

    public List<ClassDto.SessionResponse> getSessions(LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = from != null ? from : LocalDateTime.now().minusDays(1);
        LocalDateTime end = to != null ? to : LocalDateTime.now().plusDays(14);
        return sessionRepository.findByDateRange(start, end).stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClassDto.SessionResponse createSession(ClassDto.CreateSessionRequest req) {
        FitnessClass fc = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + req.getClassId()));

        Trainer trainer = null;
        if (req.getTrainerId() != null) {
            trainer = trainerRepository.findById(req.getTrainerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + req.getTrainerId()));
        }

        ClassSession session = ClassSession.builder()
                .fitnessClass(fc)
                .trainer(trainer)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .maxCapacity(req.getMaxCapacity() != null ? req.getMaxCapacity() : fc.getMaxCapacity())
                .currentBookings(0)
                .status("SCHEDULED")
                .build();

        return mapToSessionResponse(sessionRepository.save(session));
    }

    public ClassDto.SessionResponse findSessionById(Long id) {
        ClassSession s = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
        return mapToSessionResponse(s);
    }

    @Transactional
    public ClassDto.SessionResponse updateSession(Long id, ClassDto.UpdateSessionRequest req) {
        ClassSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));

        if (req.getTrainerId() != null) {
            Trainer trainer = trainerRepository.findById(req.getTrainerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + req.getTrainerId()));
            session.setTrainer(trainer);
        }
        if (req.getStartTime() != null) session.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) session.setEndTime(req.getEndTime());
        if (req.getMaxCapacity() != null) session.setMaxCapacity(req.getMaxCapacity());
        if (req.getStatus() != null) session.setStatus(req.getStatus());
        if (req.getCancelReason() != null) session.setCancelReason(req.getCancelReason());

        return mapToSessionResponse(sessionRepository.save(session));
    }

    @Transactional
    public void cancelSession(Long id, String reason) {
        ClassSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));

        session.setStatus("CANCELLED");
        session.setCancelReason(reason);
        sessionRepository.save(session);
    }

    private ClassDto.ClassResponse mapToClassResponse(FitnessClass fc) {
        return ClassDto.ClassResponse.builder()
                .id(fc.getId())
                .name(fc.getName())
                .description(fc.getDescription())
                .type(fc.getType())
                .durationMinutes(fc.getDurationMinutes())
                .maxCapacity(fc.getMaxCapacity())
                .isActive(fc.getIsActive())
                .build();
    }

    private ClassDto.SessionResponse mapToSessionResponse(ClassSession s) {
        return ClassDto.SessionResponse.builder()
                .id(s.getId())
                .classId(s.getFitnessClass().getId())
                .className(s.getFitnessClass().getName())
                .trainerId(s.getTrainer() != null ? s.getTrainer().getId() : null)
                .trainerName(s.getTrainer() != null ? s.getTrainer().getName() : null)
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .maxCapacity(s.getMaxCapacity())
                .currentBookings(s.getCurrentBookings())
                .status(s.getStatus())
                .cancelReason(s.getCancelReason())
                .build();
    }
}

