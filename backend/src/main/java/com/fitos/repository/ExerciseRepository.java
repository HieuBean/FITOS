package com.fitos.repository;

import com.fitos.model.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByMuscleGroupIgnoreCase(String muscleGroup);
    Page<Exercise> findByMuscleGroupIgnoreCase(String muscleGroup, Pageable pageable);
    Page<Exercise> findByDifficultyIgnoreCase(String difficulty, Pageable pageable);
}

