package com.fitos.controller;

import com.fitos.dto.dashboard.CrmFunnelResponse;
import com.fitos.dto.dashboard.DashboardStatsResponse;
import com.fitos.dto.dashboard.RevenueChartResponse;
import com.fitos.security.CustomUserDetailsService;
import com.fitos.security.JwtTokenProvider;
import com.fitos.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DashboardControllerTest {

    @MockBean
    private com.fitos.repository.UserRepository userRepository;

    @MockBean
    private com.fitos.service.StudioAccessService studioAccessService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    public void testGetStats() throws Exception {
        DashboardStatsResponse res = DashboardStatsResponse.builder()
                .revenueThisMonth(new BigDecimal("18450"))
                .activeMembers(2412)
                .classesToday(8)
                .bookedSpotsToday(156)
                .totalCapacityToday(196)
                .classFillRate(79.6)
                .newLeadsThisMonth(87)
                .leadConversionRate(34.0)
                .build();

        when(dashboardService.getStats()).thenReturn(res);

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenueThisMonth").value(18450))
                .andExpect(jsonPath("$.activeMembers").value(2412))
                .andExpect(jsonPath("$.classesToday").value(8))
                .andExpect(jsonPath("$.classFillRate").value(79.6));
    }

    @Test
    public void testGetRevenueChart() throws Exception {
        RevenueChartResponse res = RevenueChartResponse.builder()
                .points(List.of(
                        new RevenueChartResponse.MonthlyData("May", new BigDecimal("16100")),
                        new RevenueChartResponse.MonthlyData("Jun", new BigDecimal("17300")),
                        new RevenueChartResponse.MonthlyData("Jul", new BigDecimal("18450"))
                ))
                .build();

        when(dashboardService.getRevenueChart()).thenReturn(res);

        mockMvc.perform(get("/api/dashboard/revenue-chart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[2].month").value("Jul"))
                .andExpect(jsonPath("$.points[2].amount").value(18450));
    }

    @Test
    public void testGetCrmFunnel() throws Exception {
        CrmFunnelResponse res = CrmFunnelResponse.builder()
                .stages(List.of(
                        new CrmFunnelResponse.FunnelStage("NEW", 45, 45.0),
                        new CrmFunnelResponse.FunnelStage("CONVERTED", 6, 6.0)
                ))
                .build();

        when(dashboardService.getCrmFunnel()).thenReturn(res);

        mockMvc.perform(get("/api/dashboard/crm-funnel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages[0].stage").value("NEW"))
                .andExpect(jsonPath("$.stages[0].count").value(45));
    }
}
