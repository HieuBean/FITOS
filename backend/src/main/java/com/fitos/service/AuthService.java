package com.fitos.service;

import com.fitos.dto.auth.AuthResponse;
import com.fitos.dto.auth.LoginRequest;
import com.fitos.dto.auth.RefreshTokenRequest;
import com.fitos.dto.auth.RegisterRequest;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.model.Role;
import com.fitos.model.User;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.RoleRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.JwtTokenProvider;
import com.fitos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StudioAccessService studioAccessService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        Long studioId = studioAccessService.registrationStudio(req.getStudioId());
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already in use: " + req.getEmail());
        }

        // Public registration must never self-assign a privileged role.
        String roleName = "ROLE_MEMBER";


        Role userRole = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_MEMBER").build()));

        User user = User.builder()
                .studioId(studioId)
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .isActive(true)
                .roles(Collections.singleton(userRole))
                .build();

        user = userRepository.save(user);

        // If registered as Member, also create associated Customer profile
        if ("ROLE_MEMBER".equals(userRole.getName())) {
            Customer customer = Customer.builder()
                    .studioId(studioId)
                    .user(user)
                    .fullName(user.getFullName())
                    .phone(user.getPhone() != null ? user.getPhone() : "")
                    .email(user.getEmail())
                    .status("ACTIVE")
                    .build();
            customerRepository.save(customer);
        }

        studioAccessService.grantMember(user.getId(), studioId);
        UserPrincipal principal = studioAccessService.principal(user, studioId);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshToken = tokenProvider.generateRefreshToken(principal);

        return buildAuthResponse(user, principal, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest req) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );

            UserPrincipal authenticated = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(authenticated.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            UserPrincipal principal = studioAccessService.principal(user, req.getStudioId());

            String accessToken = tokenProvider.generateAccessToken(principal);
            String refreshToken = tokenProvider.generateRefreshToken(principal);

            return buildAuthResponse(user, principal, accessToken, refreshToken);
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String token = req.getRefreshToken();
        if (!tokenProvider.validateToken(token) || !"REFRESH".equals(tokenProvider.getTokenType(token))) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException("User account is inactive");
        }

        UserPrincipal principal = studioAccessService.principal(user, tokenProvider.getStudioIdFromToken(token));
        String newAccessToken = tokenProvider.generateAccessToken(principal);

        return buildAuthResponse(user, principal, newAccessToken, token);
    }

    public void logout(String authHeader) {
        logout(authHeader, null);
    }

    public void logout(String authHeader, String refreshToken) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenProvider.blacklistToken(token);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenProvider.blacklistToken(refreshToken);
        }
        log.info("Session tokens blacklisted on logout");
    }

    private AuthResponse buildAuthResponse(User user, UserPrincipal principal, String accessToken, String refreshToken) {
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                .userId(user.getId())
                .studioId(principal.getStudioId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }
}
