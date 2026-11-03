package com.fitos.repository;

import com.fitos.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByStudioIdOrderByNameAsc(Long studioId);

    List<Exercise> findByStudioIdAndCategoryIgnoreCaseOrderByNameAsc(Long studioId, String category);

    Optional<Exercise> findByIdAndStudioId(Long id, Long studioId);

    boolean existsByStudioIdAndNameIgnoreCase(Long studioId, String name);
}
