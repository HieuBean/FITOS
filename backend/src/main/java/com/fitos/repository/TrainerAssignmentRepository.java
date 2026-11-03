package com.fitos.repository;

import com.fitos.model.TrainerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainerAssignmentRepository extends JpaRepository<TrainerAssignment, Long> {

    List<TrainerAssignment> findByTrainerIdAndStudioIdOrderByAssignedDateDesc(Long trainerId, Long studioId);

    List<TrainerAssignment> findByCustomerIdAndStudioIdOrderByAssignedDateDesc(Long customerId, Long studioId);

    boolean existsByTrainerIdAndCustomerIdAndStatus(Long trainerId, Long customerId, String status);
}
