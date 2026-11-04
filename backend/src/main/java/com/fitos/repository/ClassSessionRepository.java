package com.fitos.repository;

import com.fitos.model.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    List<ClassSession> findByFitnessClassId(Long classId);
    
    @Query("SELECT s FROM ClassSession s WHERE s.trainer.id = :trainerId AND s.startTime >= :fromTime AND s.endTime <= :toTime")
    List<ClassSession> findByTrainerAndRange(@Param("trainerId") Long trainerId,
                                            @Param("fromTime") LocalDateTime fromTime,
                                            @Param("toTime") LocalDateTime toTime);

    @Query("SELECT s FROM ClassSession s WHERE s.startTime >= :fromTime AND s.endTime <= :toTime ORDER BY s.startTime ASC")
    List<ClassSession> findByDateRange(@Param("fromTime") LocalDateTime fromTime,
                                      @Param("toTime") LocalDateTime toTime);
}

