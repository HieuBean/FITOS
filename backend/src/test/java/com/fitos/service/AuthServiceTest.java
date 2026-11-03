package com.fitos.service;

import com.fitos.dto.auth.AuthResponse;
import com.fitos.dto.auth.LoginRequest;
import com.fitos.dto.auth.RefreshTokenRequest;
import com.fitos.dto.auth.RegisterRequest;
import com.fitos.exception.BadRequestException;
import com.fitos.model.Customer;
import com.fitos.model.Role;
import com.fitos.model.User;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.RoleRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.JwtTokenProvider;
import com.fitos.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Task #140 — Unit test AuthService: register, login, refresh, logout, disabled user.
 * Không cần DB: mọi cộng tác đều là mock.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private StudioAccessService studioAccessService;

    @InjectMocks
    private AuthService authService;

    private Role memberRole;
    private User savedUser;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        memberRole = Role.builder().id(1).name("ROLE_MEMBER").build();
        savedUser = User.builder()
                .id(7L)
                .studioId(1L)
                .email("khanh@fitos.test")
                .passwordHash("$2a$10$hashed")
                .fullName("Khanh Nguyen")
                .phone("0901234567")
                .isActive(true)
                .roles(new java.util.HashSet<>(List.of(memberRole)))
                .build();
        principal = UserPrincipal.create(savedUser, 1L, List.of("ROLE_MEMBER"));
    }

    private void stubTokens() {
        when(tokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");
        when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
    }

    @Test
    @DisplayName("Register tạo user + customer profile và cấp token")
    void registerCreatesUserAndCustomer() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("khanh@fitos.test");
        request.setPassword("secret123");
        request.setFullName("Khanh Nguyen");
        request.setPhone("0901234567");

        when(studioAccessService.registrationStudio(null)).thenReturn(1L);
        when(userRepository.existsByEmail("khanh@fitos.test")).thenReturn(false);
        when(roleRepository.findByName("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(studioAccessService.principal(any(User.class), anyLong())).thenReturn(principal);
        stubTokens();

        AuthResponse response = authService.register(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(7L, response.getUserId());
        assertEquals(List.of("ROLE_MEMBER"), response.getRoles());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("$2a$10$hashed", userCaptor.getValue().getPasswordHash(),
                "Mật khẩu phải được băm trước khi lưu");

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertEquals(1L, customerCaptor.getValue().getStudioId());
        assertEquals("ACTIVE", customerCaptor.getValue().getStatus());
        verify(studioAccessService).grantMember(7L, 1L);
    }

    @Test
    @DisplayName("Register không bao giờ tự cấp quyền ADMIN dù client gửi role")
    void registerNeverGrantsPrivilegedRole() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("khanh@fitos.test");
        request.setPassword("secret123");
        request.setFullName("Khanh Nguyen");
        request.setRole("ROLE_ADMIN");

        when(studioAccessService.registrationStudio(null)).thenReturn(1L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(studioAccessService.principal(any(User.class), anyLong())).thenReturn(principal);
        stubTokens();

        authService.register(request);

        verify(roleRepository, never()).findByName("ROLE_ADMIN");
        verify(roleRepository).findByName("ROLE_MEMBER");
    }

    @Test
    @DisplayName("Register email trùng ném BadRequestException")
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("khanh@fitos.test");
        request.setPassword("secret123");
        request.setFullName("Khanh Nguyen");

        when(studioAccessService.registrationStudio(null)).thenReturn(1L);
        when(userRepository.existsByEmail("khanh@fitos.test")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.register(request));
        assertTrue(exception.getMessage().contains("already in use"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login thành công trả token mới")
    void loginReturnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("khanh@fitos.test");
        request.setPassword("secret123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findById(7L)).thenReturn(Optional.of(savedUser));
        when(studioAccessService.principal(savedUser, null)).thenReturn(principal);
        stubTokens();

        AuthResponse response = authService.login(request);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("khanh@fitos.test", response.getEmail());
    }

    @Test
    @DisplayName("Login sai mật khẩu ném BadCredentialsException")
    void loginRejectsWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("khanh@fitos.test");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Refresh với token bị disable/không phải REFRESH bị từ chối")
    void refreshRejectsWrongTokenType() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("access-token-gia");

        when(tokenProvider.validateToken("access-token-gia")).thenReturn(true);
        when(tokenProvider.getTokenType("access-token-gia")).thenReturn("ACCESS");

        assertThrows(BadRequestException.class, () -> authService.refreshToken(request));
    }

    @Test
    @DisplayName("Refresh cho user đã bị khoá phải ném BadCredentialsException")
    void refreshRejectsDisabledUser() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        User disabled = User.builder()
                .id(7L).studioId(1L).email("khanh@fitos.test")
                .passwordHash("hash").fullName("Khanh").isActive(false).build();

        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("refresh-token")).thenReturn("REFRESH");
        when(tokenProvider.getUserIdFromToken("refresh-token")).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(disabled));

        assertThrows(BadCredentialsException.class, () -> authService.refreshToken(request));
    }

    @Test
    @DisplayName("Refresh hợp lệ cấp access token mới, giữ nguyên refresh token")
    void refreshIssuesNewAccessToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("refresh-token")).thenReturn("REFRESH");
        when(tokenProvider.getUserIdFromToken("refresh-token")).thenReturn(7L);
        when(tokenProvider.getStudioIdFromToken("refresh-token")).thenReturn(1L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(savedUser));
        when(studioAccessService.principal(savedUser, 1L)).thenReturn(principal);
        // Refresh chỉ cấp access token mới, refresh token được tái sử dụng
        when(tokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.refreshToken(request);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertNotNull(response.getExpiresIn());
    }

    @Test
    @DisplayName("Logout đưa cả access và refresh token vào blacklist")
    void logoutBlacklistsBothTokens() {
        authService.logout("Bearer access-token", "refresh-token");

        verify(tokenProvider).blacklistToken("access-token");
        verify(tokenProvider).blacklistToken("refresh-token");
    }

    @Test
    @DisplayName("Logout không có header vẫn xử lý refresh token")
    void logoutWithoutHeaderStillWorks() {
        authService.logout(null, "refresh-token");
        verify(tokenProvider).blacklistToken("refresh-token");
        verify(tokenProvider, never()).blacklistToken(null);
    }
}
