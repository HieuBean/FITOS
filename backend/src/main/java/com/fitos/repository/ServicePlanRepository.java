package com.fitos.repository;

import com.fitos.model.ServicePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePlanRepository extends JpaRepository<ServicePlan, Long> {

    List<ServicePlan> findByStudioIdOrderByPlanTypeAscPriceAsc(Long studioId);

    List<ServicePlan> findByStudioIdAndIsActiveTrueOrderByPriceAsc(Long studioId);

    Optional<ServicePlan> findByIdAndStudioId(Long id, Long studioId);

    boolean existsByStudioIdAndNameIgnoreCase(Long studioId, String name);
}
