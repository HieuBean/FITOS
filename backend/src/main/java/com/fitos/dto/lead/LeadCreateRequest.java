package com.fitos.dto.lead;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeadCreateRequest {

    private String fullName;
    private String firstName;
    private String lastName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String email;
    private LocalDate dob;
    private String goal;
    private String interest;
    private String source;
    private String notes;
    private Long assignedToUserId;
    private Long assignedStaffId;

    public String getFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        if (firstName != null || lastName != null) {
            String lName = lastName != null ? lastName.trim() : "";
            String fName = firstName != null ? firstName.trim() : "";
            return (lName + " " + fName).trim();
        }
        return fullName;
    }

    public Long getAssignedToUserId() {
        return assignedToUserId != null ? assignedToUserId : assignedStaffId;
    }

    public String getGoal() {
        if (goal != null && !goal.trim().isEmpty()) {
            return goal.trim();
        }
        if ("MOVE".equalsIgnoreCase(interest)) return "GENERAL_FITNESS";
        if ("PERFORM".equalsIgnoreCase(interest)) return "STRENGTH";
        if ("PT_FOCUS".equalsIgnoreCase(interest)) return "WEIGHT_LOSS";
        return "GENERAL_FITNESS";
    }
}
