package com.fitos.repository;

import com.fitos.model.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {
    List<AiRecommendation> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<AiRecommendation> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, String status);
}

