package com.fitos.dto.commerce;

import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.time.LocalDate;

public final class MembershipDtos {

    private MembershipDtos() {
    }

    public record MembershipResponse(
            Long id,
            Long customerId,
            String customerName,
            Long planId,
            String planName,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            long daysRemaining,
            LocalDate pausedAt,
            Integer pauseDaysUsed,
            Boolean autoRenew,
            /** LOW | MEDIUM | HIGH — dùng cho widget Renewal Risk. */
            String riskLevel,
            String riskReason,
            Instant createdAt
    ) {
    }

    public record PauseRequest(
            @Pattern(regexp = "PAUSE|RESUME", message = "action phải là PAUSE hoặc RESUME") String action
    ) {
    }

    public record RenewRequest(Long planId, Integer extraDays) {
    }
}
