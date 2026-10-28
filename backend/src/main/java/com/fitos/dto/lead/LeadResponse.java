package com.fitos.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dob;
    private String goal;
    private String source;
    private String status;
    private Long assignedToUserId;
    private String assignedToName;
    private String notes;
    private String lostReason;
    private Long convertedCustomerId;
    private Integer aiScore;
    private String aiSuggestedPersona;
    private List<LeadActivityResponse> activities;
    private Instant createdAt;
    private Instant updatedAt;
}
