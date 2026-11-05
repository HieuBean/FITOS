package com.fitos.repository;

import com.fitos.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByUserIdAndStudioId(Long userId, Long studioId);
    Optional<Customer> findByIdAndStudioId(Long id, Long studioId);
    Optional<Customer> findByEmailAndStudioId(String email, Long studioId);
    Optional<Customer> findByPhoneAndStudioId(String phone, Long studioId);
    long countByStatusAndStudioId(String status, Long studioId);
}
