package com.fitos.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByCustomerIdOrderByEndDateDesc(Long customerId);

    List<Membership> findByCustomerIdInOrderByEndDateDesc(Collection<Long> customerIds);

    Optional<Membership> findFirstByCustomerIdAndStatusOrderByEndDateDesc(Long customerId, Membership.Status status);

    Optional<Membership> findByOrderId(Long orderId);

    List<Membership> findByStatusAndEndDateBetweenOrderByEndDateAsc(Membership.Status status, LocalDate from, LocalDate to);

    long countByStatus(Membership.Status status);
}
