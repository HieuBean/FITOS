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

    /** Thời gian bỏ qua Redis sau một lần lỗi (60 giây). */
    private static final long REDIS_RETRY_COOLDOWN_MS = 60_000L;

    /** Mốc thời gian được phép thử Redis lại; 0 = chưa từng lỗi. */
    private volatile long redisRetryAt = 0L;

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
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
        // Kiểm tra blacklist KHÔNG được làm hỏng việc xác thực: nếu Redis sập,
        // isBlacklisted() trả về null (không kết luận) và token hợp lệ vẫn được chấp nhận.
        Boolean blacklisted = isBlacklisted(token);
        if (Boolean.TRUE.equals(blacklisted)) {
            log.warn("Token is blacklisted");
            return false;
        }
        return true;
    }

    public void blacklistToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Không đọc được token để blacklist: {}", ex.getMessage());
            return;
        }
        Duration ttl = Duration.between(new Date().toInstant(), claims.getExpiration().toInstant());
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        // Luôn ghi vào bộ nhớ cục bộ để logout vẫn có tác dụng khi không có Redis.
        blacklistedTokens.add(token);
        if (redis != null) {
            try {
                redis.opsForValue().set(redisKey(token), "1", ttl);
            } catch (Exception ex) {
                log.warn("Không ghi được blacklist vào Redis ({}). Chỉ áp dụng cục bộ.", ex.getMessage());
            }
        }
    }

    /**
     * @return {@code TRUE} nếu chắc chắn đã bị blacklist, {@code FALSE} nếu chắc chắn chưa,
     *         {@code null} khi không kiểm tra được (Redis lỗi). Trước đây lỗi Redis ném ra
     *         khỏi {@code validateToken()} khiến JwtAuthFilter bỏ qua xác thực và mọi request
     *         đã đăng nhập đều nhận 403 chỉ vì Redis sập.
     */
    private Boolean isBlacklisted(String token) {
        if (blacklistedTokens.contains(token)) {
            return Boolean.TRUE;
        }
        if (redis == null || !redisAvailable()) {
            return Boolean.FALSE;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(redisKey(token)));
        } catch (Exception ex) {
            // Cầu dao: Redis lỗi một lần thì bỏ qua hẳn trong 60 giây, tránh việc
            // MỌI request sau đó vẫn phải chờ timeout rồi mới xử lý.
            markRedisDown(ex.getMessage());
            return null;
        }
    }

    /**
     * Redis chỉ dùng để kiểm tra blacklist (logout) — KHÔNG phải điều kiện bắt buộc
     * để xác thực. Khi Redis không chạy, mọi request phải đi thẳng vào nghiệp vụ
     * thay vì chờ timeout.
     */
    private boolean redisAvailable() {
        long retryAfter = redisRetryAt;
        return retryAfter == 0L || System.currentTimeMillis() >= retryAfter;
    }

    private void markRedisDown(String reason) {
        long now = System.currentTimeMillis();
        // Chỉ log lần đầu của mỗi đợt để không làm ngập log
        if (redisRetryAt == 0L || now >= redisRetryAt) {
            log.warn("Redis không dùng được ({}). Tạm bỏ qua kiểm tra blacklist 60 giây.", reason);
        }
        redisRetryAt = now + REDIS_RETRY_COOLDOWN_MS;
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
