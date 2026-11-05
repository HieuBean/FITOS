package com.fitos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.dto.progress.BodyMetricRequest;
import com.fitos.dto.progress.BodyMetricResponse;
import com.fitos.dto.progress.ProgressResponse;
import com.fitos.security.CustomUserDetailsService;
import com.fitos.security.JwtTokenProvider;
import com.fitos.security.UserPrincipal;
import com.fitos.service.ProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressController.class)
@AutoConfigureMockMvc
public class ProgressControllerTest {

    @MockBean
    private com.fitos.repository.UserRepository userRepository;

    @MockBean
    private com.fitos.service.StudioAccessService studioAccessService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProgressService progressService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPrincipal createMockPrincipal() {
        return new UserPrincipal(
                1L,
                "member@fitos.com",
                "password",
                "Member Test",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
    }


    @Test
    public void testRecordBodyMetric() throws Exception {
        UserPrincipal principal = createMockPrincipal();

        BodyMetricRequest req = new BodyMetricRequest();
        req.setRecordedDate(LocalDate.now());
        req.setWeightKg(new BigDecimal("74.50"));
        req.setHeightCm(new BigDecimal("175.00"));
        req.setBodyFatPercent(new BigDecimal("15.50"));

        BodyMetricResponse res = BodyMetricResponse.builder()
                .id(1L)
                .customerId(10L)
                .recordedDate(LocalDate.now())
                .weightKg(new BigDecimal("74.50"))
                .heightCm(new BigDecimal("175.00"))
                .bodyFatPercent(new BigDecimal("15.50"))
                .build();

        when(progressService.recordBodyMetric(eq(1L), any(BodyMetricRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/body-metrics/me")
                .with(user(principal))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weightKg").value(74.50));
    }

    @Test
    public void testGetBodyMetrics() throws Exception {
        UserPrincipal principal = createMockPrincipal();

        BodyMetricResponse m1 = BodyMetricResponse.builder()
                .id(1L)
                .weightKg(new BigDecimal("75.00"))
                .recordedDate(LocalDate.now())
                .build();

        when(progressService.getBodyMetrics(1L)).thenReturn(List.of(m1));

        mockMvc.perform(get("/api/body-metrics/me")
                .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weightKg").value(75.00));
    }

    @Test
    public void testGetProgress() throws Exception {
        UserPrincipal principal = createMockPrincipal();

        ProgressResponse res = ProgressResponse.builder()
                .workoutsThisMonth(14)
                .currentStreakDays(5)
                .longestStreakDays(14)
                .currentWeightKg(new BigDecimal("74.50"))
                .startingWeightKg(new BigDecimal("80.00"))
                .weightChangeKg(new BigDecimal("-5.50"))
                .personalRecords(List.of(
                        new ProgressResponse.PersonalRecord("Barbell Bench Press", new BigDecimal("100.00"), 5, LocalDate.now())
                ))
                .build();

        when(progressService.getProgress(1L)).thenReturn(res);

        mockMvc.perform(get("/api/progress/me")
                .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutsThisMonth").value(14))
                .andExpect(jsonPath("$.currentStreakDays").value(5))
                .andExpect(jsonPath("$.personalRecords[0].exerciseName").value("Barbell Bench Press"));
    }
}
