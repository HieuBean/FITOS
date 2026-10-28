package com.fitos.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicePlanRepository extends JpaRepository<ServicePlan, Long> {

    List<ServicePlan> findByActiveTrueOrderByPlanTypeAscPriceAsc();

    List<ServicePlan> findAllByOrderByPlanTypeAscPriceAsc();

    List<ServicePlan> findByPlanTypeAndActiveTrueOrderByPriceAsc(PlanType planType);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
