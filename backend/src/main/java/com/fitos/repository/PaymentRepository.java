package com.fitos.repository;

import com.fitos.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionRef(String transactionRef);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findByCustomerIdAndStudioIdOrderByCreatedAtDesc(Long customerId, Long studioId);

    Page<Payment> findByStudioIdOrderByCreatedAtDesc(Long studioId, Pageable pageable);

    boolean existsByTransactionRef(String transactionRef);
}
