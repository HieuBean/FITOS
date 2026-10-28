package com.fitos.auth.password;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kênh gửi link đặt lại mật khẩu. Mặc định chỉ log (dev).
 * Khi có SMTP / SendGrid: tạo bean khác implement interface này → bean log tự tắt.
 */
public interface PasswordResetNotifier {

    void sendResetLink(String email, String fullName, String resetUrl);

    @Slf4j
    @Configuration
    class DefaultConfig {
        @Bean
        @ConditionalOnMissingBean(PasswordResetNotifier.class)
        PasswordResetNotifier loggingPasswordResetNotifier(
                @Value("${app.password-reset.log-links:true}") boolean logLinks) {
            return (email, fullName, url) -> {
                if (logLinks) {
                    log.info("[DEV] Password reset link for {}: {}", email, url);
                } else {
                    log.info("Password reset requested for {} (link hidden)", email);
                }
            };
        }
    }
}
