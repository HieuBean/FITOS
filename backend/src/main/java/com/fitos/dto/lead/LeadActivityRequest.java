package com.fitos.dto.lead;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeadActivityRequest {

    @NotBlank(message = "Activity type is required")
    private String activityType; // CALL, MEETING, CONSULTATION, TRIAL, NOTE, FOLLOW_UP

    @NotBlank(message = "Notes are required")
    private String notes;
}
