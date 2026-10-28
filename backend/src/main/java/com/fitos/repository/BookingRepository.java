package com.fitos.repository;

import com.fitos.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByCustomerIdAndSessionIdAndStatusNot(Long customerId, Long sessionId, String status);

    Optional<Booking> findByCustomerIdAndSessionId(Long customerId, Long sessionId);

    List<Booking> findByCustomerIdOrderByBookedAtDesc(Long customerId);

    List<Booking> findBySessionId(Long sessionId);
}
