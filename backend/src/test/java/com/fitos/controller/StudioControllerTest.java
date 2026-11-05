package com.fitos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.model.Studio;
import com.fitos.repository.StudioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fitos.security.CustomUserDetailsService;
import com.fitos.security.JwtTokenProvider;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(StudioController.class)
@AutoConfigureMockMvc(addFilters = false)
public class StudioControllerTest {

    @MockBean
    private com.fitos.repository.UserRepository userRepository;

    @MockBean
    private com.fitos.service.StudioAccessService studioAccessService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudioRepository studioRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllStudios() throws Exception {
        Studio s1 = new Studio(1L, "FITOS Q1", "District 1", "0123456789");
        when(studioRepository.findAll()).thenReturn(Arrays.asList(s1));

        mockMvc.perform(get("/api/studios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("FITOS Q1"));
    }

    @Test
    public void testCreateStudio() throws Exception {
        Studio newStudio = new Studio(null, "FITOS Q2", "District 2", "0987654321");
        Studio savedStudio = new Studio(2L, "FITOS Q2", "District 2", "0987654321");

        when(studioRepository.save(any(Studio.class))).thenReturn(savedStudio);

        mockMvc.perform(post("/api/studios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newStudio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("FITOS Q2"));
    }
}
