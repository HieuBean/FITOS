package com.fitos.service;

import com.fitos.dto.auth.AuthResponse;
import com.fitos.dto.auth.LoginRequest;
import com.fitos.dto.auth.RegisterRequest;
import com.fitos.dto.lead.LeadCreateRequest;
import com.fitos.dto.lead.LeadResponse;
import com.fitos.model.Lead;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.LeadRepository;
import com.fitos.repository.ServicePlanRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task #155 — Integration test luồng E2E của người A:
 * Register → Login → tạo Lead → Convert → Customer được tạo.
 *
 * Chỉ chạy khi có PostgreSQL test riêng (tên DB kết thúc bằng `_test`), giống
 * {@link PostgresWorkflowIntegrationTest}:
 * <pre>
 *   FITOS_TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/fitos_test
 * </pre>
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FITOS_TEST_DATABASE_URL", matches = "jdbc:postgresql:.+_test(?:\\?.*)?")
class LeadConversionFlowIntegrationTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("FITOS_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username",
                () -> System.getenv().getOrDefault("FITOS_TEST_DATABASE_USER", "fitos_user"));
        registry.add("spring.datasource.password",
                () -> System.getenv().getOrDefault("FITOS_TEST_DATABASE_PASSWORD", "supersecret"));
    }

    @Autowired
    private AuthService authService;
    @Autowired
    private LeadService leadService;
    @Autowired
    private UserRepository users;
    @Autowired
    private LeadRepository leads;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private ServicePlanRepository plans;
    @Autowired
    private StudioAccessService studioAccess;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registerLoginLeadConvertCreatesCustomer() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "e2e-" + suffix + "@fitos.test";
        String password = "Secret_" + suffix;

        // 1. Register
        RegisterRequest register = new RegisterRequest();
        register.setEmail(email);
        register.setPassword(password);
        register.setFullName("E2E Member " + suffix);
        register.setPhone("09" + suffix.replaceAll("[^0-9]", "0").substring(0, 8));
        AuthResponse registered = authService.register(register);
        assertNotNull(registered.getAccessToken());
        assertEquals(email, registered.getEmail());

        // 2. Login
        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);
        login.setStudioId(registered.getStudioId());
        AuthResponse session = authService.login(login);
        assertNotNull(session.getAccessToken());
        assertEquals(registered.getUserId(), session.getUserId());

        // 3. Tạo lead trong studio của chính user vừa đăng ký
        Long studioId = session.getStudioId();
        UserPrincipal principal = studioAccess.principal(users.findById(session.getUserId()).orElseThrow(), studioId);
        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

            LeadCreateRequest create = new LeadCreateRequest();
            create.setFullName("E2E Prospect " + suffix);
            create.setPhone("0909" + suffix.substring(0, 6));
            create.setEmail("prospect-" + suffix + "@fitos.test");
            create.setSource("website");
            create.setGoal("tăng cơ");

            LeadResponse lead = leadService.createLead(create, session.getUserId());
            assertNotNull(lead.getId());
            assertEquals("WEBSITE", lead.getSource());
            assertEquals("MUSCLE_GAIN", lead.getGoal());
            assertEquals("NEW", lead.getStatus());

            // 4. Convert → phải tạo Customer và giữ nguyên lịch sử activity
            LeadResponse converted = leadService.convertLead(lead.getId());
            assertEquals("CONVERTED", converted.getStatus());
            assertNotNull(converted.getConvertedCustomerId());
            assertTrue(customers.findById(converted.getConvertedCustomerId()).isPresent());

            Long activityCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM lead_activities WHERE lead_id = ?", Long.class, lead.getId());
            assertNotNull(activityCount);
            assertTrue(activityCount >= 1, "Lead phải giữ activity timeline sau khi convert");

            // 5. Convert lần hai phải bị chặn
            assertThrows(com.fitos.exception.BadRequestException.class,
                    () -> leadService.convertLead(lead.getId()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void seededPlansAreVisiblePerStudio() {
        Long studioId = jdbc.queryForObject("SELECT MIN(id) FROM studios", Long.class);
        assertTrue(plans.findByStudioIdAndIsActiveTrueOrderByPriceAsc(studioId).size() >= 3,
                "Studio mặc định phải có sẵn gói MEMBERSHIP và PT_PACKAGE từ migration V2_1");
        assertTrue(leads.countByStudioId(studioId) >= 0);
    }
}
