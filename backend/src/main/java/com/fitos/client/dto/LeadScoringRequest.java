package com.fitos.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadScoringRequest {
    private Long leadId;
    private String fullName;
    private String goal;
    private String source;
    private String status;
    private int activitiesCount;
}
