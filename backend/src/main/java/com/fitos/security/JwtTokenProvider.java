package com.fitos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.time.Duration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:fitos_jwt_secret_key_very_secure_for_production_and_dev_2026_at_least_32_bytes_long}")
    private String jwtSecret;

    // 15 minutes access token per Task #17
    private final long accessTokenExpirationMs = 15 * 60 * 1000L;

    // 7 days refresh token per Task #17
    private final long refreshTokenExpirationMs = 7 * 24 * 60 * 60 * 1000L;

    private SecretKey key;

    // In-memory token blacklist for Task #19 (Logout)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @Autowired(required = false)
    private StringRedisTemplate redis;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            String padded = String.format("%-32s", jwtSecret).replace(' ', 'X');
            keyBytes = padded.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("name", userPrincipal.getFullName())
                .claim("roles", roles)
                .claim("studioId", userPrincipal.getStudioId())
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("type", "REFRESH")
                .claim("studioId", userPrincipal.getStudioId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public Long getStudioIdFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number studioId = claims.get("studioId", Number.class);
        return studioId == null ? null : studioId.longValue();
    }

    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return (String) claims.get("type");
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            if (isBlacklisted(token)) {
                log.warn("Token is blacklisted");
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public void blacklistToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Duration ttl = Duration.between(new Date().toInstant(), claims.getExpiration().toInstant());
        if (ttl.isNegative() || ttl.isZero()) return;
        if (redis == null) {
            blacklistedTokens.add(token);
        } else {
            redis.opsForValue().set(redisKey(token), "1", ttl);
        }
    }

    private boolean isBlacklisted(String token) {
        return redis == null ? blacklistedTokens.contains(token) : Boolean.TRUE.equals(redis.hasKey(redisKey(token)));
    }

    private String redisKey(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return "fitos:jwt:blacklist:" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}
