package com.fitos.service;

import com.fitos.model.Booking;
import com.fitos.dto.auth.LoginRequest;
import com.fitos.repository.UserRepository;
import com.fitos.repository.LeadRepository;
import com.fitos.repository.AttendanceRepository;
import com.fitos.controller.MemberActivityController;
import com.fitos.security.JwtTokenProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fitos.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Run against a disposable PostgreSQL database whose name ends in _test. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FITOS_TEST_DATABASE_URL", matches = "jdbc:postgresql:.+_test(?:\\?.*)?")
class PostgresWorkflowIntegrationTest {
    private static final String TEST_ADMIN_PASSWORD = "IntegrationAdmin_2026";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("FITOS_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("FITOS_TEST_DATABASE_USER", "fitos_user"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("FITOS_TEST_DATABASE_PASSWORD", "supersecret"));
        registry.add("fitos.bootstrap.admin-password", () -> TEST_ADMIN_PASSWORD);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired BookingService bookings;
    @Autowired CheckInService checkIns;
    @Autowired PtSessionService ptSessions;
    @Autowired StudioAccessService studioAccess;
    @Autowired UserRepository users;
    @Autowired LeadRepository leads;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthService auth;
    @Autowired JwtTokenProvider tokens;
    @Autowired DashboardService dashboard;
    @Autowired ProgressService progress;
    @Autowired SessionAutoCompletionJob autoCompletion;
    @Autowired AttendanceRepository attendance;
    @Autowired MemberActivityController memberActivity;

    @Test
    void oneIdentityCanJoinMultipleStudiosWithoutCrossStudioReferences() {
        Long homeStudio = jdbc.queryForObject("SELECT MIN(id) FROM studios", Long.class);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long secondStudio = jdbc.queryForObject(
                "INSERT INTO studios (name, address) VALUES (?, 'Integration test') RETURNING id",
                Long.class, "FITOS Test " + suffix);
        Long platformAdminId = jdbc.queryForObject(
                "SELECT id FROM users WHERE email = 'admin@fitos.com'", Long.class);
        String seededHash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, platformAdminId);
        assertTrue(passwords.matches(TEST_ADMIN_PASSWORD, seededHash), "Configured admin credentials must work");
        assertFalse(passwords.matches("Admin@123", seededHash), "Public seed credentials must be disabled");
        UserPrincipal adminInNewStudio = studioAccess.principal(users.findById(platformAdminId).orElseThrow(), secondStudio);
        assertTrue(adminInNewStudio.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin@fitos.com");
        adminLogin.setPassword(TEST_ADMIN_PASSWORD);
        adminLogin.setStudioId(secondStudio);
        var adminSession = auth.login(adminLogin);
        assertEquals(secondStudio, tokens.getStudioIdFromToken(adminSession.getAccessToken()));
        Long user = jdbc.queryForObject(
                "INSERT INTO users (studio_id, email, password_hash, full_name) VALUES (?, ?, 'test-only', 'Integration Member') RETURNING id",
                Long.class, homeStudio, "member-" + suffix + "@fitos.test");
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = 'ROLE_MEMBER'", user);
        jdbc.update("INSERT INTO user_studio_roles (user_id, studio_id, role_id) " +
                "SELECT ?, ?, id FROM roles WHERE name = 'ROLE_MEMBER'", user, homeStudio);
        jdbc.update("INSERT INTO user_studio_roles (user_id, studio_id, role_id) " +
                "SELECT ?, ?, id FROM roles WHERE name = 'ROLE_MEMBER'", user, secondStudio);
        studioAccess.grantStaffRole(user, secondStudio, "ROLE_MANAGER");
        UserPrincipal secondPrincipal = studioAccess.principal(users.findById(user).orElseThrow(), secondStudio);
        assertEquals(secondStudio, secondPrincipal.getStudioId());
        assertTrue(secondPrincipal.getAuthorities().stream().anyMatch(a -> "ROLE_MEMBER".equals(a.getAuthority())));
        assertTrue(secondPrincipal.getAuthorities().stream().anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority())));

        Long firstCustomer = jdbc.queryForObject(
                "INSERT INTO customers (studio_id, user_id, full_name, phone) VALUES (?, ?, 'Integration Member', ?) RETURNING id",
                Long.class, homeStudio, user, "multi-a-" + suffix);
        Long secondCustomer = jdbc.queryForObject(
                "INSERT INTO customers (studio_id, user_id, full_name, phone) VALUES (?, ?, 'Integration Member', ?) RETURNING id",
                Long.class, secondStudio, user, "multi-b-" + suffix);
        assertNotEquals(firstCustomer, secondCustomer);
        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(secondPrincipal, null, secondPrincipal.getAuthorities()));
            long before = dashboard.getStats().getActiveMembers();
            Long ptPlan = jdbc.queryForObject(
                    "INSERT INTO service_plans (studio_id, name, plan_type, price, duration_days, session_count) " +
                    "VALUES (?, ?, 'PT_PACKAGE', 100, 30, 2) RETURNING id",
                    Long.class, secondStudio, "PT Test " + suffix);
            jdbc.update("INSERT INTO pt_packages (studio_id, customer_id, plan_id, total_sessions, remaining_sessions, expires_at) " +
                    "VALUES (?, ?, ?, 2, 2, CURRENT_DATE + 30)", secondStudio, secondCustomer, ptPlan);
            assertEquals(before, dashboard.getStats().getActiveMembers());
            Long membershipPlan = jdbc.queryForObject(
                    "INSERT INTO service_plans (studio_id, name, plan_type, price, duration_days) " +
                    "VALUES (?, ?, 'MEMBERSHIP', 100, 30) RETURNING id",
                    Long.class, secondStudio, "Membership Test " + suffix);
            jdbc.update("INSERT INTO memberships (studio_id, customer_id, plan_id, start_date, end_date) " +
                    "VALUES (?, ?, ?, CURRENT_DATE, CURRENT_DATE + 30)",
                    secondStudio, secondCustomer, membershipPlan);
            assertEquals(before + 1, dashboard.getStats().getActiveMembers());
            Long workout = jdbc.queryForObject(
                    "INSERT INTO workout_sessions (studio_id, customer_id, session_date, start_time, end_time) " +
                    "VALUES (?, ?, CURRENT_DATE, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP) RETURNING id",
                    Long.class, secondStudio, secondCustomer);
            Long exercise = jdbc.queryForObject("SELECT MIN(id) FROM exercises", Long.class);
            jdbc.update("INSERT INTO workout_logs (studio_id, session_id, exercise_id, set_number, weight_kg, reps_completed) " +
                    "VALUES (?, ?, ?, 1, 100, 3), (?, ?, ?, 2, 80, 10)",
                    secondStudio, workout, exercise, secondStudio, workout, exercise);
            var memberProgress = progress.getProgress(user);
            assertEquals(1, memberProgress.getWorkoutsThisMonth());
            assertEquals(0, memberProgress.getPersonalRecords().get(0).getMaxWeightKg().compareTo(new java.math.BigDecimal("100")));
            assertEquals(3, memberProgress.getPersonalRecords().get(0).getReps());
        } finally {
            SecurityContextHolder.clearContext();
        }
        Long classId = jdbc.queryForObject(
                "INSERT INTO classes (studio_id, name, category) VALUES (?, ?, 'Yoga') RETURNING id",
                Long.class, secondStudio, "Studio 2 Yoga " + suffix);
        Long classSession = jdbc.queryForObject(
                "INSERT INTO class_sessions (studio_id, class_id, session_date, start_time, end_time, capacity) " +
                "VALUES (?, ?, CURRENT_DATE + 1, '13:00', '14:00', 10) RETURNING id",
                Long.class, secondStudio, classId);
        Booking memberBooking = bookings.bookClass(secondCustomer, classSession);
        checkIns.checkIn(secondCustomer, memberBooking.getId(), "QR");
        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(secondPrincipal, null, secondPrincipal.getAuthorities()));
            assertTrue(memberActivity.recentActivity(secondPrincipal).stream()
                    .anyMatch(event -> event.kind().equals("CHECK_IN") && event.title().contains("Studio 2 Yoga")));
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO bookings (studio_id, customer_id, session_id) VALUES (?, ?, ?)",
                        secondStudio, firstCustomer, classSession));
        Long leadId = jdbc.queryForObject(
                "INSERT INTO leads (studio_id, full_name, phone) VALUES (?, 'Studio 2 Lead', ?) RETURNING id",
                Long.class, secondStudio, "lead-" + suffix);
        assertTrue(leads.findByIdAndStudioId(leadId, secondStudio).isPresent());
        assertTrue(leads.findByIdAndStudioId(leadId, homeStudio).isEmpty());
        Long anotherCustomer = jdbc.queryForObject(
                "INSERT INTO customers (studio_id, full_name, phone) VALUES (?, 'No Show Test', ?) RETURNING id",
                Long.class, secondStudio, "no-show-" + suffix);
        Long pastClass = jdbc.queryForObject(
                "INSERT INTO class_sessions (studio_id, class_id, session_date, start_time, end_time, capacity) " +
                "VALUES (?, ?, CURRENT_DATE - 1, '13:00', '14:00', 2) RETURNING id",
                Long.class, secondStudio, classId);
        Long attendedBooking = jdbc.queryForObject(
                "INSERT INTO bookings (studio_id, customer_id, session_id) VALUES (?, ?, ?) RETURNING id",
                Long.class, secondStudio, secondCustomer, pastClass);
        Long noShowBooking = jdbc.queryForObject(
                "INSERT INTO bookings (studio_id, customer_id, session_id) VALUES (?, ?, ?) RETURNING id",
                Long.class, secondStudio, anotherCustomer, pastClass);
        jdbc.update("INSERT INTO attendance (studio_id, customer_id, booking_id, class_session_id, method) " +
                "VALUES (?, ?, ?, ?, 'DESK')", secondStudio, secondCustomer, attendedBooking, pastClass);
        autoCompletion.autoCompletePastSessions();
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM class_sessions WHERE id = ?", String.class, pastClass));
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM bookings WHERE id = ?", String.class, attendedBooking));
        assertEquals("NO_SHOW", jdbc.queryForObject("SELECT status FROM bookings WHERE id = ?", String.class, noShowBooking));
    }

    @Test
    void lastSlotCheckInAndPtCompletionAreConsistent() throws Exception {
        Long studio = jdbc.queryForObject("SELECT MIN(id) FROM studios", Long.class);
        Long classId = jdbc.queryForObject("SELECT MIN(id) FROM classes WHERE studio_id = ?", Long.class, studio);
        Long trainer = jdbc.queryForObject("SELECT MIN(id) FROM trainers WHERE studio_id = ?", Long.class, studio);
        Long plan = jdbc.queryForObject("SELECT MIN(id) FROM service_plans WHERE plan_type='PT_PACKAGE' AND studio_id = ?", Long.class, studio);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long first = jdbc.queryForObject(
                "INSERT INTO customers (studio_id, full_name, phone) VALUES (?, ?, ?) RETURNING id",
                Long.class, studio, "Integration A", "it-a-" + suffix);
        Long second = jdbc.queryForObject(
                "INSERT INTO customers (studio_id, full_name, phone) VALUES (?, ?, ?) RETURNING id",
                Long.class, studio, "Integration B", "it-b-" + suffix);
        Long session = jdbc.queryForObject(
                "INSERT INTO class_sessions (studio_id, class_id, session_date, start_time, end_time, capacity) " +
                        "VALUES (?, ?, CURRENT_DATE + 1, '09:00', '10:00', 1) RETURNING id",
                Long.class, studio, classId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var one = pool.submit(() -> bookAfterGate(first, session, ready, start));
            var two = pool.submit(() -> bookAfterGate(second, session, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            Booking winner = one.get(10, TimeUnit.SECONDS);
            Booking other = two.get(10, TimeUnit.SECONDS);
            assertEquals(1, (winner == null ? 0 : 1) + (other == null ? 0 : 1));
            assertEquals(1, jdbc.queryForObject("SELECT booked_count FROM class_sessions WHERE id = ?", Integer.class, session));
            Booking booked = winner != null ? winner : other;
            assertNotNull(checkIns.checkIn(booked.getCustomerId(), booked.getId(), "QR"));
            assertThrows(IllegalStateException.class,
                    () -> checkIns.checkIn(booked.getCustomerId(), booked.getId(), "QR"));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM attendance WHERE booking_id = ?", Integer.class, booked.getId()));
            assertEquals(1, attendance.findByCustomerIdOrderByCheckinTimeDesc(booked.getCustomerId()).size());
        } finally {
            pool.shutdown();
        }

        Long rebookSession = jdbc.queryForObject(
                "INSERT INTO class_sessions (studio_id, class_id, session_date, start_time, end_time, capacity) " +
                        "VALUES (?, ?, CURRENT_DATE + 2, '09:00', '10:00', 1) RETURNING id",
                Long.class, studio, classId);
        Booking cancelled = bookings.bookClass(first, rebookSession);
        bookings.cancelBooking(cancelled.getId(), "Integration test");
        Booking rebooked = bookings.bookClass(first, rebookSession);
        assertNotEquals(cancelled.getId(), rebooked.getId());
        assertEquals(1, jdbc.queryForObject("SELECT booked_count FROM class_sessions WHERE id = ?", Integer.class, rebookSession));

        Long pkg = jdbc.queryForObject(
                "INSERT INTO pt_packages (studio_id, customer_id, plan_id, total_sessions, remaining_sessions, expires_at) " +
                        "VALUES (?, ?, ?, 1, 1, CURRENT_DATE + 30) RETURNING id",
                Long.class, studio, first, plan);
        Long pt = jdbc.queryForObject(
                "INSERT INTO pt_sessions (studio_id, trainer_id, customer_id, pt_package_id, session_date, start_time, end_time) " +
                        "VALUES (?, ?, ?, ?, CURRENT_DATE, '11:00', '12:00') RETURNING id",
                Long.class, studio, trainer, first, pkg);
        String ref = "it-" + suffix;
        ptSessions.completePtSession(pt, ref);
        ptSessions.completePtSession(pt, ref);
        assertThrows(IllegalStateException.class, () -> ptSessions.completePtSession(pt, ref + "-new"));
        Long anotherPt = jdbc.queryForObject(
                "INSERT INTO pt_sessions (studio_id, trainer_id, customer_id, session_date, start_time, end_time) " +
                "VALUES (?, ?, ?, CURRENT_DATE, '13:00', '14:00') RETURNING id",
                Long.class, studio, trainer, first);
        assertThrows(IllegalStateException.class, () -> ptSessions.completePtSession(anotherPt, ref));
        assertEquals(0, jdbc.queryForObject("SELECT remaining_sessions FROM pt_packages WHERE id = ?", Integer.class, pkg));
    }

    private Booking bookAfterGate(Long customer, Long session, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            return bookings.bookClass(customer, session);
        } catch (IllegalStateException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }
}
