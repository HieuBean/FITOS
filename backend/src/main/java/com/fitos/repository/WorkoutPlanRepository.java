package com.fitos.repository;

import com.fitos.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    List<WorkoutPlan> findByCustomerId(Long customerId);

    @Query("SELECT p FROM WorkoutPlan p WHERE p.customer.id = :customerId AND p.status = 'ACTIVE'")
    Optional<WorkoutPlan> findActiveByCustomerId(@Param("customerId") Long customerId);
}

