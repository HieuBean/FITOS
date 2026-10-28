package com.fitos.dto.lead;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeadUpdateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String email;
    private LocalDate dob;
    private String goal;
    private String source;
    private String status;
    private String notes;
    private String lostReason;
    private Long assignedToUserId;
}
