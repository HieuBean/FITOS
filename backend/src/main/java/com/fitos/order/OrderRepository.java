package com.fitos.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    boolean existsByOrderReference(String orderReference);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    Optional<Order> findByOrderReference(String orderReference);

    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    List<Order> findTop20ByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
