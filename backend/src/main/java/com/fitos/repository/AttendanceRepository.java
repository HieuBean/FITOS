package com.fitos.repository;

import com.fitos.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByBookingId(Long bookingId);

    List<Attendance> findByCustomerIdOrderByCheckinTimeDesc(Long customerId);

}
