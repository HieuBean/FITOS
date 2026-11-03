package com.fitos.dto.commerce;

import com.fitos.model.ServicePlan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class PlanDtos {

    private PlanDtos() {
    }

    public record PlanRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "MEMBERSHIP|PT_PACKAGE", message = "planType phải là MEMBERSHIP hoặc PT_PACKAGE")
            String planType,
            @NotNull @DecimalMin(value = "0.0", message = "Giá không được âm") BigDecimal price,
            @NotNull @Positive Integer durationDays,
            Integer sessionCount,
            String description,
            List<String> features,
            Boolean isActive
    ) {
    }

    public record PlanResponse(
            Long id,
            String name,
            String planType,
            BigDecimal price,
            Integer durationDays,
            Integer sessionCount,
            String description,
            List<String> features,
            Boolean isActive
    ) {
        public static PlanResponse from(ServicePlan plan, List<String> features) {
            return new PlanResponse(plan.getId(), plan.getName(), plan.getPlanType(), plan.getPrice(),
                    plan.getDurationDays(), plan.getSessionCount(), plan.getDescription(), features,
                    plan.getIsActive());
        }
    }
}
