package com.fitos.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadActivityResponse {
    private Long id;
    private Long leadId;
    private Long userId;
    private String userName;
    private String activityType;
    private String notes;
    private Instant activityTime;
    private Instant createdAt;
}
