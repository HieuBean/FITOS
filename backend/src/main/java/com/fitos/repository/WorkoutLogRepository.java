package com.fitos.repository;

import com.fitos.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {
    List<WorkoutLog> findByWorkoutSessionIdOrderBySetNumberAsc(Long sessionId);

    @Query("SELECT l FROM WorkoutLog l JOIN l.workoutSession s WHERE s.customer.id = :customerId ORDER BY l.weightKg DESC")
    List<WorkoutLog> findCustomerLogsByWeightDesc(@Param("customerId") Long customerId);
}

