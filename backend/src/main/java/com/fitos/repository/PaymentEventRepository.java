package com.fitos.repository;

import com.fitos.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    boolean existsByEventKey(String eventKey);
}
