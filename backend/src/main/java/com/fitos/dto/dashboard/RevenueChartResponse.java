package com.fitos.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueChartResponse {

    private List<MonthlyData> points;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyData {
        private String month;
        private BigDecimal amount;
    }
}
