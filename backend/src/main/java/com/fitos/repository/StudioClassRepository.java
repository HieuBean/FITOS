package com.fitos.repository;

import com.fitos.model.StudioClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudioClassRepository extends JpaRepository<StudioClass, Long> {

    List<StudioClass> findByStudioIdOrderByNameAsc(Long studioId);

    List<StudioClass> findByStudioIdAndIsActiveTrueOrderByNameAsc(Long studioId);

    Optional<StudioClass> findByIdAndStudioId(Long id, Long studioId);
}
