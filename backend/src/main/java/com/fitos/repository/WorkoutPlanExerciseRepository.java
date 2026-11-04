package com.fitos.repository;

import com.fitos.model.WorkoutPlanExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutPlanExerciseRepository extends JpaRepository<WorkoutPlanExercise, Long> {
    List<WorkoutPlanExercise> findByWorkoutPlanIdOrderByDayOfWeekAscSortOrderAsc(Long planId);
}

