package com.fitos.service;

import com.fitos.dto.progress.BodyMetricRequest;
import com.fitos.dto.progress.BodyMetricResponse;
import com.fitos.dto.progress.ProgressResponse;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.BodyMetric;
import com.fitos.model.Customer;
import com.fitos.repository.BodyMetricRepository;
import com.fitos.security.StudioContext;
import com.fitos.security.UserPrincipal;
import com.fitos.model.Role;
import com.fitos.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Task #147 â€” Unit test ProgressService: ghi/sá»­a body metric theo ngÃ y,
 * tÃ­nh streak hiá»‡n táº¡i & dÃ i nháº¥t (ká»ƒ cáº£ edge case nghá»‰ cÃ¡ch ngÃ y), personal records.
 */
@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long STUDIO_ID = 1L;
    private static final Long CUSTOMER_ID = 99L;

    @Mock
    private com.fitos.repository.CustomerRepository customerRepository;
    @Mock
    private BodyMetricRepository bodyMetricRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    @InjectMocks
    private ProgressService progressService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(USER_ID).studioId(STUDIO_ID).email("member@fitos.test")
                .passwordHash("hash").fullName("Member").isActive(true)
                .roles(new HashSet<>(List.of(Role.builder().id(1).name("ROLE_MEMBER").build())))
                .build();
        UserPrincipal principal = UserPrincipal.create(user, STUDIO_ID, List.of("ROLE_MEMBER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        customer = Customer.builder()
                .id(CUSTOMER_ID)
                .studioId(STUDIO_ID)
                .fullName("Member")
                .phone("0900000000")
                .status("ACTIVE")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubNativeQuery(Object result) {
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        if (result != null) {
            lenient().when(query.getSingleResult()).thenReturn(result);
            lenient().when(query.getResultList()).thenReturn((List<?>) result);
        }
    }

    @Test
    @DisplayName("Ghi body metric táº¡o báº£n ghi má»›i khi ngÃ y chÆ°a cÃ³")
    void recordBodyMetricCreatesNewRecord() {
        when(customerRepository.findByUserIdAndStudioId(USER_ID, STUDIO_ID)).thenReturn(Optional.of(customer));
        when(bodyMetricRepository.findByCustomerIdAndRecordedDate(CUSTOMER_ID, LocalDate.of(2026, 3, 10)))
                .thenReturn(Optional.empty());

        BodyMetricRequest request = new BodyMetricRequest();
        request.setRecordedDate(LocalDate.of(2026, 3, 10));
        request.setWeightKg(new BigDecimal("78.5"));
        request.setHeightCm(new BigDecimal("175.0"));

        when(bodyMetricRepository.save(any(BodyMetric.class))).thenAnswer(invocation -> {
            BodyMetric metric = invocation.getArgument(0);
            metric.setId(1L);
            return metric;
        });

        BodyMetricResponse response = progressService.recordBodyMetric(USER_ID, request);

        assertEquals(new BigDecimal("78.5"), response.getWeightKg());
        assertEquals(CUSTOMER_ID, response.getCustomerId());
        assertEquals(LocalDate.of(2026, 3, 10), response.getRecordedDate());
    }

    @Test
    @DisplayName("Ghi body metric cÃ¹ng ngÃ y thÃ¬ cáº­p nháº­t, khÃ´ng táº¡o báº£n ghi thá»© hai")
    void recordBodyMetricUpdatesSameDay() {
        when(customerRepository.findByUserIdAndStudioId(USER_ID, STUDIO_ID)).thenReturn(Optional.of(customer));
        BodyMetric existing = BodyMetric.builder()
                .id(5L).studioId(STUDIO_ID).customer(customer)
                .recordedDate(LocalDate.of(2026, 3, 10))
                .weightKg(new BigDecimal("80.0"))
                .build();
        when(bodyMetricRepository.findByCustomerIdAndRecordedDate(CUSTOMER_ID, LocalDate.of(2026, 3, 10)))
                .thenReturn(Optional.of(existing));
        when(bodyMetricRepository.save(any(BodyMetric.class))).thenAnswer(i -> i.getArgument(0));

        BodyMetricRequest request = new BodyMetricRequest();
        request.setRecordedDate(LocalDate.of(2026, 3, 10));
        request.setWeightKg(new BigDecimal("79.2"));

        BodyMetricResponse response = progressService.recordBodyMetric(USER_ID, request);

        assertEquals(5L, response.getId());
        assertEquals(new BigDecimal("79.2"), response.getWeightKg());
    }

    @Test
    @DisplayName("Há»™i viÃªn khÃ´ng tá»“n táº¡i trong studio pháº£i nÃ©m ResourceNotFoundException")
    void recordBodyMetricRejectsUnknownCustomer() {
        when(customerRepository.findByUserIdAndStudioId(USER_ID, STUDIO_ID)).thenReturn(Optional.empty());
        BodyMetricRequest request = new BodyMetricRequest();
        request.setWeightKg(new BigDecimal("70"));

        assertThrows(ResourceNotFoundException.class, () -> progressService.recordBodyMetric(USER_ID, request));
        verify(bodyMetricRepository, never()).save(any());
    }

    @Test
    @DisplayName("Streak hiá»‡n táº¡i tÃ­nh tá»« hÃ´m nay lÃ¹i dáº§n; hÃ´m qua váº«n Ä‘Æ°á»£c ná»‘i")
    void currentStreakAllowsStartingYesterday() {
        // KhÃ´ng táº­p hÃ´m nay, táº­p hÃ´m qua vÃ  hÃ´m kia â†’ streak = 2
        LocalDate today = LocalDate.now();
        List<Object> rows = List.of(Date.valueOf(today.minusDays(1)), Date.valueOf(today.minusDays(2)));
        stubNativeQuery(rows);

        assertEquals(2, invokeStreak("calculateCurrentStreak"));
    }

    @Test
    @DisplayName("Streak hiá»‡n táº¡i = 0 khi buá»•i gáº§n nháº¥t Ä‘Ã£ cÃ¡ch Ä‘Ã¢y 2 ngÃ y")
    void currentStreakResetsAfterGap() {
        LocalDate today = LocalDate.now();
        stubNativeQuery(List.of(Date.valueOf(today.minusDays(3))));

        assertEquals(0, invokeStreak("calculateCurrentStreak"));
    }

    @Test
    @DisplayName("Streak dÃ i nháº¥t Ä‘o qua khoáº£ng nghá»‰, khÃ´ng cá»™ng dá»“n khi Ä‘á»©t quÃ£ng")
    void longestStreakBreaksOnGap() {
        LocalDate base = LocalDate.of(2026, 1, 1);
        // 3 ngÃ y liÃªn tiáº¿p, nghá»‰, rá»“i 2 ngÃ y liÃªn tiáº¿p â†’ dÃ i nháº¥t = 3
        stubNativeQuery(List.of(
                Date.valueOf(base),
                Date.valueOf(base.plusDays(1)),
                Date.valueOf(base.plusDays(2)),
                Date.valueOf(base.plusDays(5)),
                Date.valueOf(base.plusDays(6))));

        assertEquals(3, invokeStreak("calculateLongestStreak"));
    }

    @Test
    @DisplayName("KhÃ´ng cÃ³ buá»•i táº­p nÃ o thÃ¬ cáº£ hai streak Ä‘á»u báº±ng 0")
    void streaksAreZeroWithoutWorkouts() {
        stubNativeQuery(List.of());

        assertEquals(0, invokeStreak("calculateCurrentStreak"));
        assertEquals(0, invokeStreak("calculateLongestStreak"));
    }

    @Test
    @DisplayName("Progress gá»™p cÃ¢n náº·ng, streak, workouts vÃ  personal records")
    void getProgressAggregatesEverything() {
        when(customerRepository.findByUserIdAndStudioId(USER_ID, STUDIO_ID)).thenReturn(Optional.of(customer));

        BodyMetric newest = BodyMetric.builder().id(2L).customer(customer)
                .recordedDate(LocalDate.of(2026, 3, 10)).weightKg(new BigDecimal("75.0")).build();
        BodyMetric oldest = BodyMetric.builder().id(1L).customer(customer)
                .recordedDate(LocalDate.of(2026, 1, 10)).weightKg(new BigDecimal("80.0")).build();
        when(bodyMetricRepository.findByCustomerIdOrderByRecordedDateDesc(CUSTOMER_ID))
                .thenReturn(List.of(newest, oldest));

        Query countQuery = org.mockito.Mockito.mock(Query.class);
        Query prQuery = org.mockito.Mockito.mock(Query.class);
        Query streakQuery = org.mockito.Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.contains("COUNT(*)")))
                .thenReturn(countQuery);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.contains("ROW_NUMBER")))
                .thenReturn(prQuery);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.contains("DISTINCT session_date")))
                .thenReturn(streakQuery);
        for (Query q : List.of(countQuery, prQuery, streakQuery)) {
            lenient().when(q.setParameter(anyString(), any())).thenReturn(q);
        }
        when(countQuery.getSingleResult()).thenReturn(7);
        // Collections.singletonList Ä‘á»ƒ trÃ¡nh varargs tráº£i pháº³ng máº¥t Object[] cá»§a káº¿t quáº£ native query
        when(prQuery.getResultList()).thenReturn(java.util.Collections.singletonList(
                new Object[]{"Barbell Squat", new BigDecimal("120.00"), 5, Date.valueOf(LocalDate.of(2026, 3, 1))}));
        when(streakQuery.getResultList()).thenReturn(List.of());

        ProgressResponse response = progressService.getProgress(USER_ID);

        assertEquals(7, response.getWorkoutsThisMonth());
        assertEquals(new BigDecimal("75.0"), response.getCurrentWeightKg());
        assertEquals(new BigDecimal("80.0"), response.getStartingWeightKg());
        assertEquals(new BigDecimal("-5.0"), response.getWeightChangeKg());
        assertEquals(1, response.getPersonalRecords().size());
        assertEquals("Barbell Squat", response.getPersonalRecords().get(0).getExerciseName());
    }

    @Test
    @DisplayName("ChÆ°a cÃ³ body metric thÃ¬ cÃ¡c trÆ°á»ng cÃ¢n náº·ng lÃ  null, khÃ´ng chia cho 0")
    void getProgressHandlesNoMetrics() {
        when(customerRepository.findByUserIdAndStudioId(USER_ID, STUDIO_ID)).thenReturn(Optional.of(customer));
        when(bodyMetricRepository.findByCustomerIdOrderByRecordedDateDesc(CUSTOMER_ID)).thenReturn(List.of());

        Query q = org.mockito.Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(q);
        lenient().when(q.setParameter(anyString(), any())).thenReturn(q);
        lenient().when(q.getSingleResult()).thenReturn(0);
        lenient().when(q.getResultList()).thenReturn(List.of());

        ProgressResponse response = progressService.getProgress(USER_ID);

        assertNull(response.getCurrentWeightKg());
        assertNull(response.getWeightChangeKg());
        assertEquals(0, response.getWorkoutsThisMonth());
        assertTrue(response.getPersonalRecords().isEmpty());
    }

    /** Gá»i method private qua reflection Ä‘á»ƒ kiá»ƒm thá»­ thuáº­t toÃ¡n streak trá»±c tiáº¿p. */
    private int invokeStreak(String methodName) {
        try {
            java.lang.reflect.Method method = ProgressService.class.getDeclaredMethod(methodName, Long.class);
            method.setAccessible(true);
            return (int) method.invoke(progressService, CUSTOMER_ID);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("KhÃ´ng gá»i Ä‘Æ°á»£c " + methodName, exception);
        }
    }
}
