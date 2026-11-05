package com.fitos.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FITOS_TEST_REDIS", matches = "true")
class RedisTokenBlacklistIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("FITOS_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("FITOS_TEST_DATABASE_USER", "fitos_user"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("FITOS_TEST_DATABASE_PASSWORD", "supersecret"));
    }

    @Autowired JwtTokenProvider tokens;
    @Autowired StringRedisTemplate redis;

    @Test
    void logoutBlacklistIsStoredInRedisForBothTokenTypes() {
        UserPrincipal principal = new UserPrincipal(
                987654L, "redis-test@fitos.test", "unused", "Redis Test", true,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")), 1L);
        String access = tokens.generateAccessToken(principal);
        String refresh = tokens.generateRefreshToken(principal);
        assertTrue(tokens.validateToken(access));
        assertTrue(tokens.validateToken(refresh));
        tokens.blacklistToken(access);
        tokens.blacklistToken(refresh);
        assertFalse(tokens.validateToken(access));
        assertFalse(tokens.validateToken(refresh));
        assertTrue(redis.keys("fitos:jwt:blacklist:*").size() >= 2);
    }
}
