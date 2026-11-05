package com.fitos.report.dto;

import java.math.BigDecimal;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record DailyRevenue(String date, BigDecimal amount, long orderCount) {
    }

    public record PlanSalesBreakdown(String planName, long count, BigDecimal totalRevenue) {
    }

    public record SalesReportResponse(
            BigDecimal totalRevenue,
            long totalPaidOrders,
            BigDecimal averageOrderValue,
            List<DailyRevenue> timeline,
            List<PlanSalesBreakdown> planBreakdown
    ) {
    }

    public record MembershipStatusCount(String status, long count) {
    }

    public record MembershipReportResponse(
            long activeMembers,
            long pausedMembers,
            long expiredMembers,
            long totalMembers,
            List<MembershipStatusCount> breakdown,
            double retentionRate
    ) {
    }

    public record ClassUtilization(String className, long totalSessions, long totalBookings, double avgOccupancy) {
    }

    public record ClassesReportResponse(
            long totalClassesConducted,
            long totalBookings,
            double overallOccupancyRate,
            List<ClassUtilization> topClasses
    ) {
    }
}
