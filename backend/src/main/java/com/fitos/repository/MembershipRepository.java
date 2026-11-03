package com.fitos.repository;

import com.fitos.model.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByIdAndStudioId(Long id, Long studioId);

    List<Membership> findByCustomerIdAndStudioIdOrderByEndDateDesc(Long customerId, Long studioId);

    List<Membership> findByStudioIdAndStatusOrderByEndDateAsc(Long studioId, String status);

    List<Membership> findByStudioIdOrderByEndDateAsc(Long studioId);

    boolean existsByOrderIdAndCustomerIdAndStudioId(Long orderId, Long customerId, Long studioId);

    long countByStudioIdAndStatus(Long studioId, String status);

    @Query("SELECT m FROM Membership m WHERE m.customerId = :customerId AND m.studioId = :studioId " +
           "AND m.status = 'ACTIVE' AND m.endDate >= :today ORDER BY m.endDate DESC")
    List<Membership> findActive(@Param("customerId") Long customerId,
                                @Param("studioId") Long studioId,
                                @Param("today") LocalDate today);

    @Query("SELECT m FROM Membership m WHERE m.studioId = :studioId AND m.status = 'ACTIVE' " +
           "AND m.endDate BETWEEN :today AND :until ORDER BY m.endDate ASC")
    List<Membership> findExpiring(@Param("studioId") Long studioId,
                                  @Param("today") LocalDate today,
                                  @Param("until") LocalDate until);
}
