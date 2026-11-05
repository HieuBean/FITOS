package com.fitos.controller;

import com.fitos.dto.dashboard.CrmFunnelResponse;
import com.fitos.dto.dashboard.DashboardStatsResponse;
import com.fitos.dto.dashboard.RevenueChartResponse;
import com.fitos.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Manager Dashboard", description = "Endpoints for business KPIs, revenue charts and CRM pipeline funnels")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get high-level studio manager KPI statistics")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/revenue-chart")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get monthly revenue trend for pure CSS chart visualization")
    public ResponseEntity<RevenueChartResponse> getRevenueChart() {
        return ResponseEntity.ok(dashboardService.getRevenueChart());
    }

    @GetMapping("/crm-funnel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get CRM lead conversion funnel breakdown")
    public ResponseEntity<CrmFunnelResponse> getCrmFunnel() {
        return ResponseEntity.ok(dashboardService.getCrmFunnel());
    }
}
