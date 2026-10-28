package com.fitos.repository;

import com.fitos.model.PtPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PtPackageRepository extends JpaRepository<PtPackage, Long> {

    Optional<PtPackage> findByCustomerIdAndStatus(Long customerId, String status);
}
