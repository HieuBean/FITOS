package com.fitos.repository;

import com.fitos.model.FitnessClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FitnessClassRepository extends JpaRepository<FitnessClass, Long> {
    Page<FitnessClass> findByTypeIgnoreCase(String type, Pageable pageable);
    Page<FitnessClass> findByIsActiveTrue(Pageable pageable);
}

