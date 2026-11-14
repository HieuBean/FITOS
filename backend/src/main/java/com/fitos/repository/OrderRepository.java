package com.fitos.repository;

import com.fitos.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndStudioId(Long id, Long studioId);

    Optional<Order> findByOrderReference(String orderReference);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Order> findTop20ByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<Order> findByStudioIdOrderByCreatedAtDesc(Long studioId, Pageable pageable);

    Page<Order> findByStudioIdAndStatusOrderByCreatedAtDesc(Long studioId, String status, Pageable pageable);

    List<Order> findByCustomerIdAndStudioIdOrderByCreatedAtDesc(Long customerId, Long studioId);

    boolean existsByOrderReference(String orderReference);

    Optional<Order> findByStudioIdAndClientRequestId(Long studioId, String clientRequestId);

    Page<Order> findByStudioIdAndOrderTypeOrderByCreatedAtDesc(Long studioId, String orderType, Pageable pageable);

    Page<Order> findByStudioIdAndOrderTypeAndStatusOrderByCreatedAtDesc(
            Long studioId, String orderType, String status, Pageable pageable);

    @Query("""
           SELECT o FROM Order o
           WHERE o.studioId = :studioId AND o.orderType IN ('RETAIL','RETAIL_RETURN')
             AND o.createdAt >= :from AND o.createdAt < :to
           ORDER BY o.createdAt DESC
           """)
    List<Order> findRetailBetween(@Param("studioId") Long studioId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.studioId = :studioId AND o.status = 'PAID' AND o.createdAt >= :from")
    BigDecimal sumPaidSince(@Param("studioId") Long studioId, @Param("from") Instant from);
}
