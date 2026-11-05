package com.fitos.repository;

import com.fitos.model.LeadActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {
    List<LeadActivity> findByLeadIdOrderByActivityTimeDesc(Long leadId);
}
