package com.fitos.repository;

import com.fitos.model.ClassSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    /**
     * SELECT FOR UPDATE pessimistic write lock to prevent race conditions during booking.
     * Task #51 & Task #154 (Concurrency protection)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM ClassSession cs WHERE cs.id = :id")
    Optional<ClassSession> findByIdWithLock(@Param("id") Long id);
}
