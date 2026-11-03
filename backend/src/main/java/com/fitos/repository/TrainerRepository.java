package com.fitos.repository;

import com.fitos.model.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    List<Trainer> findByStudioIdOrderByFullNameAsc(Long studioId);

    List<Trainer> findByStudioIdAndIsActiveTrueOrderByFullNameAsc(Long studioId);

    Optional<Trainer> findByIdAndStudioId(Long id, Long studioId);
}
