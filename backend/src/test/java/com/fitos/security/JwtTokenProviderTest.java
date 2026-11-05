package com.fitos.security;

import com.fitos.model.Role;
import com.fitos.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    public void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "super_secret_jwt_key_that_is_at_least_32_bytes_long_for_tests!");
        tokenProvider.init();
    }

    @Test
    public void testGenerateAndValidateAccessToken() {
        Role role = Role.builder().id(1).name("ROLE_MEMBER").build();
        User user = User.builder()
                .id(100L)
                .email("member@fitos.com")
                .fullName("Test Member")
                .passwordHash("hashed")
                .roles(Collections.singleton(role))
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        String token = tokenProvider.generateAccessToken(principal);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(100L, tokenProvider.getUserIdFromToken(token));
        assertEquals("ACCESS", tokenProvider.getTokenType(token));
    }

    @Test
    public void testBlacklistToken() {
        Role role = Role.builder().id(1).name("ROLE_ADMIN").build();
        User user = User.builder()
                .id(1L)
                .email("admin@fitos.com")
                .fullName("Admin")
                .roles(Collections.singleton(role))
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        String token = tokenProvider.generateAccessToken(principal);

        assertTrue(tokenProvider.validateToken(token));
        tokenProvider.blacklistToken(token);
        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    public void testRefreshTokenGeneration() {
        Role role = Role.builder().id(1).name("ROLE_MEMBER").build();
        User user = User.builder()
                .id(200L)
                .email("refresh@fitos.com")
                .fullName("Refresh User")
                .roles(Collections.singleton(role))
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        String refreshToken = tokenProvider.generateRefreshToken(principal);

        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateToken(refreshToken));
        assertEquals("REFRESH", tokenProvider.getTokenType(refreshToken));
    }
}
