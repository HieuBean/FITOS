package com.fitos.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
