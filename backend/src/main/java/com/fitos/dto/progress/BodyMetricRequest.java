package com.fitos.dto.progress;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BodyMetricRequest {

    private LocalDate recordedDate;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "20.0", message = "Weight must be at least 20 kg")
    private BigDecimal weightKg;

    private BigDecimal heightCm;
    private BigDecimal bodyFatPercent;
    private BigDecimal muscleMassKg;
    private String notes;
}
