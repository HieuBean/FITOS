package com.fitos.service;

import com.fitos.dto.ExerciseDto;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Exercise;
import com.fitos.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public Page<ExerciseDto.Response> findAll(String muscleGroup, String difficulty, Pageable pageable) {
        if (muscleGroup != null && !muscleGroup.isBlank()) {
            return exerciseRepository.findByMuscleGroupIgnoreCase(muscleGroup, pageable).map(this::mapToResponse);
        }
        if (difficulty != null && !difficulty.isBlank()) {
            return exerciseRepository.findByDifficultyIgnoreCase(difficulty, pageable).map(this::mapToResponse);
        }
        return exerciseRepository.findAll(pageable).map(this::mapToResponse);
    }

    public ExerciseDto.Response findById(Long id) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with id: " + id));
        return mapToResponse(exercise);
    }

    @Transactional
    public ExerciseDto.Response create(ExerciseDto.CreateRequest req) {
        Exercise exercise = Exercise.builder()
                .name(req.getName())
                .description(req.getDescription())
                .muscleGroup(req.getMuscleGroup().toUpperCase())
                .equipment(req.getEquipment())
                .difficulty(req.getDifficulty() != null ? req.getDifficulty().toUpperCase() : "BEGINNER")
                .videoUrl(req.getVideoUrl())
                .imageUrl(req.getImageUrl())
                .isActive(true)
                .build();
        return mapToResponse(exerciseRepository.save(exercise));
    }

    @Transactional
    public ExerciseDto.Response update(Long id, ExerciseDto.UpdateRequest req) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found with id: " + id));

        if (req.getName() != null) exercise.setName(req.getName());
        if (req.getDescription() != null) exercise.setDescription(req.getDescription());
        if (req.getMuscleGroup() != null) exercise.setMuscleGroup(req.getMuscleGroup().toUpperCase());
        if (req.getEquipment() != null) exercise.setEquipment(req.getEquipment());
        if (req.getDifficulty() != null) exercise.setDifficulty(req.getDifficulty().toUpperCase());
        if (req.getVideoUrl() != null) exercise.setVideoUrl(req.getVideoUrl());
        if (req.getImageUrl() != null) exercise.setImageUrl(req.getImageUrl());
        if (req.getIsActive() != null) exercise.setIsActive(req.getIsActive());

        return mapToResponse(exerciseRepository.save(exercise));
    }

    public ExerciseDto.Response mapToResponse(Exercise e) {
        return ExerciseDto.Response.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .muscleGroup(e.getMuscleGroup())
                .equipment(e.getEquipment())
                .difficulty(e.getDifficulty())
                .videoUrl(e.getVideoUrl())
                .imageUrl(e.getImageUrl())
                .isActive(e.getIsActive())
                .build();
    }
}

