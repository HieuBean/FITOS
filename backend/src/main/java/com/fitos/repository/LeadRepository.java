package com.fitos.repository;

import com.fitos.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    @Query("SELECT l FROM Lead l WHERE l.studioId = :studioId AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:source IS NULL OR l.source = :source) AND " +
           "(:search IS NULL OR LOWER(l.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR l.phone LIKE CONCAT('%', :search, '%'))")
    Page<Lead> findWithFilters(
            @Param("studioId") Long studioId,
            @Param("status") String status,
            @Param("source") String source,
            @Param("search") String search,
            Pageable pageable);

    Optional<Lead> findByIdAndStudioId(Long id, Long studioId);
    boolean existsByIdAndStudioId(Long id, Long studioId);
    long countByStatusAndStudioId(String status, Long studioId);
    long countByCreatedAtAfterAndStudioId(Instant after, Long studioId);
    long countByStudioId(Long studioId);

    long countByCreatedAtAfter(Instant after);

    @Query("SELECT l.status, COUNT(l) FROM Lead l WHERE l.studioId = :studioId GROUP BY l.status")
    List<Object[]> countGroupedByStatus(@Param("studioId") Long studioId);

}
