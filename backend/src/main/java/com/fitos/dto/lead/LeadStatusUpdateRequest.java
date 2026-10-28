package com.fitos.dto.lead;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeadStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
    private String lostReason;
}
