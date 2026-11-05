package com.fitos.repository;

import com.fitos.model.ClassSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM ClassSession cs WHERE cs.id = :id")
    Optional<ClassSession> findByIdWithLock(@Param("id") Long id);

    Optional<ClassSession> findByIdAndStudioId(Long id, Long studioId);

    List<ClassSession> findByStudioIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            Long studioId, LocalDate from, LocalDate to);

    List<ClassSession> findByStudioIdAndSessionDateOrderByStartTimeAsc(Long studioId, LocalDate date);

    List<ClassSession> findByClassIdAndStudioIdOrderBySessionDateAscStartTimeAsc(Long classId, Long studioId);
}
