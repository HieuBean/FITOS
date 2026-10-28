package com.fitos.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BodyMetricResponse {
    private Long id;
    private Long customerId;
    private LocalDate recordedDate;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal bodyFatPercent;
    private BigDecimal muscleMassKg;
    private String notes;
    private Instant createdAt;
}
