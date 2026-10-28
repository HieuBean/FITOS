package com.fitos.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PtPackageRepository extends JpaRepository<PtPackage, Long> {

    List<PtPackage> findByCustomerIdOrderByExpiryDateDesc(Long customerId);

    List<PtPackage> findByCustomerIdAndStatus(Long customerId, PtPackage.Status status);

    Optional<PtPackage> findByOrderId(Long orderId);
}
