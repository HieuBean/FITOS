package com.fitos.repository;

import com.fitos.model.Trainer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerRepository extends JpaRepository<Trainer, Long> {
    Optional<Trainer> findByEmail(String email);
    Page<Trainer> findByIsActiveTrue(Pageable pageable);
    List<Trainer> findByStudioIdOrderByFullNameAsc(Long studioId);
    List<Trainer> findByStudioIdAndIsActiveTrueOrderByFullNameAsc(Long studioId);
    Optional<Trainer> findByIdAndStudioId(Long id, Long studioId);
}
