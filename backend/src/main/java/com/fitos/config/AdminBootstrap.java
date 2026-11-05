package com.fitos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private static final String SEED_HASH = "$2a$10$VtLeWIzr3aQO2ZZPpanOkuypePoSDRDJ7CuDyYlOA1tRuVaJnP7iC";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;

    @Value("${fitos.bootstrap.admin-password:}")
    private String initialPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (initialPassword == null || initialPassword.isBlank()) return;
        if (initialPassword.length() < 12) {
            throw new IllegalStateException("FITOS_BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters");
        }
        jdbc.update("UPDATE users SET password_hash = ?, is_active = TRUE " +
                        "WHERE email = 'admin@fitos.com' AND password_hash = ? AND is_active = FALSE",
                passwords.encode(initialPassword), SEED_HASH);
    }
}
