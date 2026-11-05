package com.fitos.plan.dto;

import com.fitos.plan.PlanType;
import com.fitos.plan.ServicePlan;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class PlanDtos {

    private PlanDtos() {
    }

    /** Body cho POST /api/plans và PUT /api/plans/{id}. */
    public record PlanRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull PlanType planType,
            @NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(1) @Max(3650) Integer durationDays,
            @Min(1) @Max(500) Integer totalSessions,
            @Size(max = 2000) String description,
            @Size(max = 20) List<@NotBlank @Size(max = 120) String> features,
            Boolean active
    ) {
    }

    /** Body cho PATCH /api/plans/{id}/status. */
    public record PlanStatusRequest(@NotNull Boolean active) {
    }

    public record PlanResponse(
            Long id,
            String name,
            PlanType planType,
            BigDecimal price,
            Integer durationDays,
            Integer totalSessions,
            String description,
            List<String> features,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static PlanResponse from(ServicePlan p) {
            return new PlanResponse(p.getId(), p.getName(), p.getPlanType(), p.getPrice(),
                    p.getDurationDays(), p.getTotalSessions(), p.getDescription(),
                    p.getFeatures() == null ? List.of() : List.copyOf(p.getFeatures()),
                    p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
