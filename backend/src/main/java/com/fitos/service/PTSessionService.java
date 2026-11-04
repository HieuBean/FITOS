package com.fitos.service;

import com.fitos.dto.PTDto;
import com.fitos.exception.BusinessException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.model.PTPackage;
import com.fitos.model.PTSession;
import com.fitos.model.Trainer;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.PTPackageRepository;
import com.fitos.repository.PTSessionRepository;
import com.fitos.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PTSessionService {

    private final PTSessionRepository ptSessionRepository;
    private final PTPackageRepository ptPackageRepository;
    private final CustomerRepository customerRepository;
    private final TrainerRepository trainerRepository;
    private final PTPackageService ptPackageService;

    public List<PTDto.SessionResponse> getSessions(Long customerId, Long trainerId) {
        if (customerId != null) {
            return ptSessionRepository.findByCustomerId(customerId).stream()
                    .map(this::mapToSessionResponse)
                    .collect(Collectors.toList());
        }
        if (trainerId != null) {
            return ptSessionRepository.findByTrainerId(trainerId).stream()
                    .map(this::mapToSessionResponse)
                    .collect(Collectors.toList());
        }
        return ptSessionRepository.findAll().stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Task 63: Đặt lịch tập PT, xác thực Trainer có rảnh không và Customer có gói hợp lệ không
     */
    @Transactional
    public PTDto.SessionResponse scheduleSession(PTDto.ScheduleSessionRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + req.getCustomerId()));

        Trainer trainer = trainerRepository.findById(req.getTrainerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + req.getTrainerId()));

        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new BusinessException("End time must be after start time");
        }

        // Kiểm tra trainer có bị trùng lịch PT khác không
        List<PTSession> conflicts = ptSessionRepository.findConflictingSessions(
                trainer.getId(), req.getStartTime(), req.getEndTime()
        );
        if (!conflicts.isEmpty()) {
            throw new BusinessException("Trainer " + trainer.getName() + " already has a scheduled session during this time slot.");
        }

        // Tìm PT package của customer nếu có
        PTPackage ptPackage = null;
        if (req.getPtPackageId() != null) {
            ptPackage = ptPackageRepository.findById(req.getPtPackageId())
                    .orElseThrow(() -> new ResourceNotFoundException("PT Package not found: " + req.getPtPackageId()));
        } else {
            // Tự động tìm package còn buổi
            ptPackage = ptPackageRepository.findFirstByCustomerIdAndRemainingSessionsGreaterThanAndStatus(
                    customer.getId(), 0, "ACTIVE"
            ).orElse(null);
        }

        PTSession session = PTSession.builder()
                .customer(customer)
                .trainer(trainer)
                .ptPackage(ptPackage)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status("SCHEDULED")
                .notes(req.getNotes())
                .build();

        log.info("Scheduled PT Session for customer {} with trainer {} at {}", 
                customer.getId(), trainer.getId(), req.getStartTime());

        return mapToSessionResponse(ptSessionRepository.save(session));
    }

    /**
     * Task 64: Đổi lịch PT session
     */
    @Transactional
    public PTDto.SessionResponse rescheduleSession(Long sessionId, PTDto.RescheduleSessionRequest req) {
        PTSession session = ptSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("PT Session not found: " + sessionId));

        if ("COMPLETED".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("Cannot reschedule already COMPLETED session");
        }

        // Kiểm tra xung đột lịch mới
        List<PTSession> conflicts = ptSessionRepository.findConflictingSessions(
                session.getTrainer().getId(), req.getStartTime(), req.getEndTime()
        );
        boolean hasConflict = conflicts.stream().anyMatch(s -> !s.getId().equals(sessionId));
        if (hasConflict) {
            throw new BusinessException("Trainer has conflict in the new time slot");
        }

        session.setStartTime(req.getStartTime());
        session.setEndTime(req.getEndTime());
        session.setStatus("RESCHEDULED");

        return mapToSessionResponse(ptSessionRepository.save(session));
    }

    /**
     * Task 64: Hủy lịch PT session
     */
    @Transactional
    public void cancelSession(Long sessionId, String reason) {
        PTSession session = ptSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("PT Session not found: " + sessionId));

        if ("COMPLETED".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("Cannot cancel an already COMPLETED session");
        }

        session.setStatus("CANCELLED");
        session.setCancelReason(reason);
        ptSessionRepository.save(session);
    }

    /**
     * Task 65: Hoàn tất buổi tập PT - Idempotent via completion_ref
     * Gọi nhiều lần với cùng 1 session id chỉ trừ số buổi đúng 1 lần!
     */
    @Transactional
    public PTDto.SessionResponse completeSession(Long sessionId) {
        PTSession session = ptSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("PT Session not found: " + sessionId));

        // Kiểm tra Idempotency: nếu đã complete và có completionRef thì trả về ngay lập tức
        if ("COMPLETED".equalsIgnoreCase(session.getStatus()) && session.getCompletionRef() != null) {
            log.info("Session {} already completed with ref {}. Returning idempotently.", 
                    sessionId, session.getCompletionRef());
            return mapToSessionResponse(session);
        }

        String completionRef = UUID.randomUUID().toString();
        session.setCompletionRef(completionRef);
        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        PTSession savedSession = ptSessionRepository.save(session);

        // Trừ buổi trong gói tập PT nếu session gắn với gói
        if (savedSession.getPtPackage() != null) {
            ptPackageService.useSession(savedSession.getPtPackage().getId());
        }

        log.info("Successfully completed PT Session {} with idempotent completion_ref: {}", sessionId, completionRef);
        return mapToSessionResponse(savedSession);
    }

    public PTDto.SessionResponse mapToSessionResponse(PTSession s) {
        return PTDto.SessionResponse.builder()
                .id(s.getId())
                .ptPackageId(s.getPtPackage() != null ? s.getPtPackage().getId() : null)
                .customerId(s.getCustomer().getId())
                .customerName(s.getCustomer().getName())
                .trainerId(s.getTrainer().getId())
                .trainerName(s.getTrainer().getName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .completionRef(s.getCompletionRef())
                .completedAt(s.getCompletedAt())
                .notes(s.getNotes())
                .cancelReason(s.getCancelReason())
                .build();
    }
}

