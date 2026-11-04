package com.fitos.repository;

import com.fitos.model.PTPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PTPackageRepository extends JpaRepository<PTPackage, Long> {
    List<PTPackage> findByCustomerId(Long customerId);
    List<PTPackage> findByCustomerIdAndStatus(Long customerId, String status);
    Optional<PTPackage> findFirstByCustomerIdAndRemainingSessionsGreaterThanAndStatus(
            Long customerId, int minRemaining, String status
    );
}

