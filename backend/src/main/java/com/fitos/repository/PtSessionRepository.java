package com.fitos.repository;

import com.fitos.model.PtSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PtSessionRepository extends JpaRepository<PtSession, Long> {

    Optional<PtSession> findByCompletionRef(String completionRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PtSession s where s.id = :id")
    Optional<PtSession> findByIdWithLock(@Param("id") Long id);

}
