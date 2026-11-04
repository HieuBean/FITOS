package com.fitos.repository;

import com.fitos.model.PTSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PTSessionRepository extends JpaRepository<PTSession, Long> {
    Optional<PTSession> findByCompletionRef(String completionRef);

    List<PTSession> findByCustomerId(Long customerId);
    List<PTSession> findByTrainerId(Long trainerId);

    @Query("SELECT s FROM PTSession s WHERE s.trainer.id = :trainerId AND s.status = 'SCHEDULED' " +
           "AND ((s.startTime < :endTime AND s.endTime > :startTime))")
    List<PTSession> findConflictingSessions(@Param("trainerId") Long trainerId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    @Query("SELECT s FROM PTSession s WHERE s.trainer.id = :trainerId AND s.startTime >= :fromTime AND s.endTime <= :toTime ORDER BY s.startTime ASC")
    List<PTSession> findByTrainerAndRange(@Param("trainerId") Long trainerId,
                                         @Param("fromTime") LocalDateTime fromTime,
                                         @Param("toTime") LocalDateTime toTime);
}

