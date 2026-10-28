package com.fitos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.dto.lead.*;
import com.fitos.security.CustomUserDetailsService;
import com.fitos.security.JwtTokenProvider;
import com.fitos.service.LeadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeadController.class)
@AutoConfigureMockMvc(addFilters = false)
public class LeadControllerTest {

    @MockBean
    private com.fitos.repository.UserRepository userRepository;

    @MockBean
    private com.fitos.service.StudioAccessService studioAccessService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeadService leadService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateLead() throws Exception {
        LeadCreateRequest req = new LeadCreateRequest();
        req.setFullName("Nguyen Van Lead");
        req.setPhone("0912345678");
        req.setGoal("WEIGHT_LOSS");
        req.setSource("WALK_IN");

        LeadResponse res = LeadResponse.builder()
                .id(1L)
                .fullName("Nguyen Van Lead")
                .phone("0912345678")
                .goal("WEIGHT_LOSS")
                .source("WALK_IN")
                .status("NEW")
                .aiScore(75)
                .build();

        when(leadService.createLead(any(LeadCreateRequest.class), any())).thenReturn(res);

        mockMvc.perform(post("/api/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van Lead"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.aiScore").value(75));
    }

    @Test
    public void testGetLeads() throws Exception {
        LeadResponse l1 = LeadResponse.builder()
                .id(1L)
                .fullName("Lead One")
                .phone("0911111111")
                .status("NEW")
                .build();

        when(leadService.getLeads(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(l1)));

        mockMvc.perform(get("/api/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Lead One"));
    }

    @Test
    public void testGetLeadById() throws Exception {
        LeadResponse res = LeadResponse.builder()
                .id(10L)
                .fullName("Lead Ten")
                .phone("0910101010")
                .status("CONTACTED")
                .build();

        when(leadService.getLeadById(10L)).thenReturn(res);

        mockMvc.perform(get("/api/leads/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CONTACTED"));
    }

    @Test
    public void testUpdateStatus() throws Exception {
        LeadStatusUpdateRequest req = new LeadStatusUpdateRequest();
        req.setStatus("TRIAL");

        LeadResponse res = LeadResponse.builder()
                .id(10L)
                .fullName("Lead Ten")
                .status("TRIAL")
                .build();

        when(leadService.updateStatus(eq(10L), any(LeadStatusUpdateRequest.class), any())).thenReturn(res);

        mockMvc.perform(patch("/api/leads/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIAL"));
    }

    @Test
    public void testAddActivity() throws Exception {
        LeadActivityRequest req = new LeadActivityRequest();
        req.setActivityType("CALL");
        req.setNotes("Called customer to schedule trial class.");

        LeadActivityResponse res = LeadActivityResponse.builder()
                .id(100L)
                .leadId(10L)
                .activityType("CALL")
                .notes("Called customer to schedule trial class.")
                .activityTime(Instant.now())
                .build();

        when(leadService.addActivity(eq(10L), any(LeadActivityRequest.class), any())).thenReturn(res);

        mockMvc.perform(post("/api/leads/10/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.activityType").value("CALL"));
    }
}
