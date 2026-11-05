package com.fitos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitos.dto.auth.AuthResponse;
import com.fitos.dto.auth.LoginRequest;
import com.fitos.dto.auth.RefreshTokenRequest;
import com.fitos.dto.auth.RegisterRequest;
import com.fitos.security.CustomUserDetailsService;
import com.fitos.security.JwtTokenProvider;
import com.fitos.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @MockBean
    private com.fitos.repository.UserRepository userRepository;

    @MockBean
    private com.fitos.service.StudioAccessService studioAccessService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegisterSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("newuser@fitos.com");
        req.setPassword("Password123");
        req.setFullName("New User");
        req.setPhone("0909888777");

        AuthResponse res = AuthResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .userId(1L)
                .email("newuser@fitos.com")
                .fullName("New User")
                .roles(List.of("ROLE_MEMBER"))
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.email").value("newuser@fitos.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_MEMBER"));
    }

    @Test
    public void testLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@fitos.com");
        req.setPassword("Admin@123");

        AuthResponse res = AuthResponse.builder()
                .accessToken("mock-admin-access-token")
                .refreshToken("mock-admin-refresh-token")
                .tokenType("Bearer")
                .userId(1L)
                .email("admin@fitos.com")
                .fullName("System Admin")
                .roles(List.of("ROLE_ADMIN"))
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-admin-access-token"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    public void testRefreshTokenSuccess() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("mock-valid-refresh-token");

        AuthResponse res = AuthResponse.builder()
                .accessToken("new-mock-access-token")
                .refreshToken("mock-valid-refresh-token")
                .tokenType("Bearer")
                .userId(1L)
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-mock-access-token"));
    }

    @Test
    public void testLogoutSuccess() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer sample-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"));
    }
}
