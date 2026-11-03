package com.fitos.repository;

import com.fitos.model.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {

    List<AiRecommendation> findByCustomerIdAndStudioIdOrderByCreatedAtDesc(Long customerId, Long studioId);

    List<AiRecommendation> findByCustomerIdAndStudioIdAndStatusOrderByCreatedAtDesc(
            Long customerId, Long studioId, String status);

    Optional<AiRecommendation> findByIdAndStudioId(Long id, Long studioId);
}
