package com.fitos.auth.password;

import com.fitos.auth.password.dto.PasswordDtos.ChangePasswordRequest;
import com.fitos.auth.password.dto.PasswordDtos.ForgotPasswordRequest;
import com.fitos.auth.password.dto.PasswordDtos.ResetPasswordRequest;
import com.fitos.common.security.CurrentUserService;
import com.fitos.user.User;
import com.fitos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Tasks #20 (forgot & reset) and #21 (change-password).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotifier notifier;
    private final CurrentUserService currentUser;
    private final Clock clock;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.password-reset.token-validity-minutes:60}")
    private int tokenValidityMinutes;

    // ------------------------------------------------ Task #20: Forgot Password
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req, String clientIp) {
        String email = req.email().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        // Security best practice: Always return same response to avoid email enumeration
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existing email: {}", email);
            return;
        }

        User user = userOpt.get();
        OffsetDateTime now = OffsetDateTime.now(clock);

        // Rate limiting check: max 5 requests per hour
        long recentRequests = tokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minusHours(1));
        if (recentRequests >= 5) {
            log.warn("Password reset rate limit exceeded for user: {}", user.getId());
            return;
        }

        // Invalidate older tokens
        tokenRepository.invalidateAllForUser(user.getId(), now);

        // Generate high entropy random token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);
        String tokenHash = hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(tokenHash);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(tokenValidityMinutes));
        token.setRequestedIp(clientIp);

        tokenRepository.save(token);

        String resetUrl = frontendUrl + "/reset-password?token=" + rawToken;
        notifier.sendResetLink(user.getEmail(), user.getFullName(), resetUrl);
    }

    // ------------------------------------------------ Task #20: Reset Password
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String tokenHash = hashToken(req.token().trim());
        OffsetDateTime now = OffsetDateTime.now(clock);

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        if (!resetToken.isUsable(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(now);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user id: {}", user.getId());
    }

    // ------------------------------------------------ Task #21: Change Password
    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User user = currentUser.requireUser();

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password does not match");
        }

        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be the same as current password");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        log.info("Password changed by user id: {}", user.getId());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
