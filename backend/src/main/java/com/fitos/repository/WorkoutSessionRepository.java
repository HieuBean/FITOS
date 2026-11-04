package com.fitos.repository;

import com.fitos.model.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    List<WorkoutSession> findByCustomerIdOrderByStartedAtDesc(Long customerId);
    
    @Query("SELECT s FROM WorkoutSession s WHERE s.customer.id = :customerId AND s.status = 'IN_PROGRESS' ORDER BY s.startedAt DESC")
    Optional<WorkoutSession> findActiveSessionByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT s FROM WorkoutSession s WHERE s.customer.id = :customerId AND s.status = 'COMPLETED' ORDER BY s.startedAt DESC")
    List<WorkoutSession> findCompletedSessionsByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT s FROM WorkoutSession s WHERE s.customer.id = :customerId AND s.startedAt >= :since ORDER BY s.startedAt ASC")
    List<WorkoutSession> findSessionsSince(@Param("customerId") Long customerId, @Param("since") LocalDateTime since);
}

