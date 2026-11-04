package com.fitos.service;

import com.fitos.dto.TrainerDto;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.ClassSession;
import com.fitos.model.Customer;
import com.fitos.model.PTSession;
import com.fitos.model.Trainer;
import com.fitos.repository.ClassSessionRepository;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.PTSessionRepository;
import com.fitos.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private final TrainerRepository trainerRepository;
    private final CustomerRepository customerRepository;
    private final PTSessionRepository ptSessionRepository;
    private final ClassSessionRepository classSessionRepository;

    public Page<TrainerDto.Response> findAll(Pageable pageable) {
        return trainerRepository.findAll(pageable).map(this::mapToResponse);
    }

    public TrainerDto.Response findById(Long id) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with id: " + id));
        return mapToResponse(trainer);
    }

    @Transactional
    public TrainerDto.Response create(TrainerDto.CreateRequest req) {
        Trainer trainer = Trainer.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .specialty(req.getSpecialty())
                .bio(req.getBio())
                .isActive(true)
                .build();
        return mapToResponse(trainerRepository.save(trainer));
    }

    @Transactional
    public TrainerDto.Response update(Long id, TrainerDto.UpdateRequest req) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with id: " + id));

        if (req.getName() != null) trainer.setName(req.getName());
        if (req.getPhone() != null) trainer.setPhone(req.getPhone());
        if (req.getSpecialty() != null) trainer.setSpecialty(req.getSpecialty());
        if (req.getBio() != null) trainer.setBio(req.getBio());
        if (req.getIsActive() != null) trainer.setIsActive(req.getIsActive());

        return mapToResponse(trainerRepository.save(trainer));
    }

    @Transactional
    public void assignCustomer(Long trainerId, Long customerId) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + trainerId));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        trainer.getAssignedCustomers().add(customer);
        trainerRepository.save(trainer);
    }

    @Transactional(readOnly = true)
    public TrainerDto.ScheduleResponse getWeeklySchedule(Long trainerId, LocalDate weekStart) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + trainerId));

        LocalDateTime fromTime = weekStart.atStartOfDay();
        LocalDateTime toTime = weekStart.plusDays(7).atTime(LocalTime.MAX);

        List<PTSession> ptSessions = ptSessionRepository.findByTrainerAndRange(trainerId, fromTime, toTime);
        List<ClassSession> classSessions = classSessionRepository.findByTrainerAndRange(trainerId, fromTime, toTime);

        List<TrainerDto.PTSessionItem> ptItems = ptSessions.stream().map(pt -> TrainerDto.PTSessionItem.builder()
                .id(pt.getId())
                .customerId(pt.getCustomer().getId())
                .customerName(pt.getCustomer().getName())
                .startTime(pt.getStartTime().toString())
                .endTime(pt.getEndTime().toString())
                .status(pt.getStatus())
                .build()).collect(Collectors.toList());

        List<TrainerDto.ClassSessionItem> classItems = classSessions.stream().map(cs -> TrainerDto.ClassSessionItem.builder()
                .id(cs.getId())
                .className(cs.getFitnessClass().getName())
                .startTime(cs.getStartTime().toString())
                .endTime(cs.getEndTime().toString())
                .currentBookings(cs.getCurrentBookings())
                .maxCapacity(cs.getMaxCapacity())
                .status(cs.getStatus())
                .build()).collect(Collectors.toList());

        return TrainerDto.ScheduleResponse.builder()
                .trainerId(trainer.getId())
                .trainerName(trainer.getName())
                .ptSessions(ptItems)
                .classSessions(classItems)
                .build();
    }

    private TrainerDto.Response mapToResponse(Trainer trainer) {
        return TrainerDto.Response.builder()
                .id(trainer.getId())
                .name(trainer.getName())
                .email(trainer.getEmail())
                .phone(trainer.getPhone())
                .specialty(trainer.getSpecialty())
                .bio(trainer.getBio())
                .isActive(trainer.getIsActive())
                .assignedCustomerCount(trainer.getAssignedCustomers() != null ? trainer.getAssignedCustomers().size() : 0)
                .build();
    }
}

