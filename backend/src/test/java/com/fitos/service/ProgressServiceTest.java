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
 * Task #147 — Unit test ProgressService: ghi/sửa body metric theo ngày,
 * tính streak hiện tại & dài nhất (kể cả edge case nghỉ cách ngày), personal records.
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
    @DisplayName("Ghi body metric tạo bản ghi mới khi ngày chưa có")
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
    @DisplayName("Ghi body metric cùng ngày thì cập nhật, không tạo bản ghi thứ hai")
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
    @DisplayName("Hội viên không tồn tại trong studio phải ném ResourceNotFoundException")
    void recordBodyMetricRejectsUnknownCustomer() {
        when(customerRepository.findByUserIdAndStudioId(USER_ID, STUDIO_ID)).thenReturn(Optional.empty());
        BodyMetricRequest request = new BodyMetricRequest();
        request.setWeightKg(new BigDecimal("70"));

        assertThrows(ResourceNotFoundException.class, () -> progressService.recordBodyMetric(USER_ID, request));
        verify(bodyMetricRepository, never()).save(any());
    }

    @Test
    @DisplayName("Streak hiện tại tính từ hôm nay lùi dần; hôm qua vẫn được nối")
    void currentStreakAllowsStartingYesterday() {
        // Không tập hôm nay, tập hôm qua và hôm kia → streak = 2
        LocalDate today = LocalDate.now();
        List<Object> rows = List.of(Date.valueOf(today.minusDays(1)), Date.valueOf(today.minusDays(2)));
        stubNativeQuery(rows);

        assertEquals(2, invokeStreak("calculateCurrentStreak"));
    }

    @Test
    @DisplayName("Streak hiện tại = 0 khi buổi gần nhất đã cách đây 2 ngày")
    void currentStreakResetsAfterGap() {
        LocalDate today = LocalDate.now();
        stubNativeQuery(List.of(Date.valueOf(today.minusDays(3))));

        assertEquals(0, invokeStreak("calculateCurrentStreak"));
    }

    @Test
    @DisplayName("Streak dài nhất đo qua khoảng nghỉ, không cộng dồn khi đứt quãng")
    void longestStreakBreaksOnGap() {
        LocalDate base = LocalDate.of(2026, 1, 1);
        // 3 ngày liên tiếp, nghỉ, rồi 2 ngày liên tiếp → dài nhất = 3
        stubNativeQuery(List.of(
                Date.valueOf(base),
                Date.valueOf(base.plusDays(1)),
                Date.valueOf(base.plusDays(2)),
                Date.valueOf(base.plusDays(5)),
                Date.valueOf(base.plusDays(6))));

        assertEquals(3, invokeStreak("calculateLongestStreak"));
    }

    @Test
    @DisplayName("Không có buổi tập nào thì cả hai streak đều bằng 0")
    void streaksAreZeroWithoutWorkouts() {
        stubNativeQuery(List.of());

        assertEquals(0, invokeStreak("calculateCurrentStreak"));
        assertEquals(0, invokeStreak("calculateLongestStreak"));
    }

    @Test
    @DisplayName("Progress gộp cân nặng, streak, workouts và personal records")
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
        // Collections.singletonList để tránh varargs trải phẳng mất Object[] của kết quả native query
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
    @DisplayName("Chưa có body metric thì các trường cân nặng là null, không chia cho 0")
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

    /** Gọi method private qua reflection để kiểm thử thuật toán streak trực tiếp. */
    private int invokeStreak(String methodName) {
        try {
            var method = ProgressService.class.getDeclaredMethod(methodName, Long.class);
            method.setAccessible(true);
            return (int) method.invoke(progressService, CUSTOMER_ID);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Không gọi được " + methodName, exception);
        }
    }
}
