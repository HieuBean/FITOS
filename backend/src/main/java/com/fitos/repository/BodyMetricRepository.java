package com.fitos.repository;

import com.fitos.model.BodyMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BodyMetricRepository extends JpaRepository<BodyMetric, Long> {
    List<BodyMetric> findByCustomerIdOrderByRecordedAtDesc(Long customerId);
    Optional<BodyMetric> findFirstByCustomerIdOrderByRecordedAtDesc(Long customerId);
}

