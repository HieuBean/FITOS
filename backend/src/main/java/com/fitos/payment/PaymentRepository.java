package com.fitos.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Dùng cho idempotency webhook (#39, test #143). */
    Optional<Payment> findByTransactionRef(String transactionRef);

    boolean existsByOrderIdAndStatus(Long orderId, Payment.Status status);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findByOrderIdInOrderByCreatedAtDesc(Collection<Long> orderIds);
}
