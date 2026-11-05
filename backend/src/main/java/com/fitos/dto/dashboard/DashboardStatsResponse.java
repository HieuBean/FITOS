package com.fitos.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private BigDecimal revenueThisMonth;
    private long activeMembers;
    private int classesToday;
    private int bookedSpotsToday;
    private int totalCapacityToday;
    private double classFillRate;
    private long newLeadsThisMonth;
    private double leadConversionRate;
}
