package com.fitos.repository;

import com.fitos.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByEmail(String email);

    /**
     * Customer gắn với User qua quan hệ {@code user}, nên Spring Data không suy ra
     * được thuộc tính {@code userId} từ tên method. Dùng JPQL tường minh.
     */
    @Query("select c from Customer c where c.user.id = :userId")
    Optional<Customer> findByUserId(@Param("userId") Long userId);

    @Query("select c from Customer c where c.user.id = :userId and c.studioId = :studioId")
    Optional<Customer> findByUserIdAndStudioId(@Param("userId") Long userId, @Param("studioId") Long studioId);

    Optional<Customer> findByIdAndStudioId(Long id, Long studioId);

    Optional<Customer> findByEmailAndStudioId(String email, Long studioId);

    Optional<Customer> findByPhoneAndStudioId(String phone, Long studioId);

    long countByStatusAndStudioId(String status, Long studioId);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
