package com.fitos.service;

import com.fitos.client.LeadAIClient;
import com.fitos.dto.lead.LeadActivityRequest;
import com.fitos.dto.lead.LeadCreateRequest;
import com.fitos.dto.lead.LeadResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.model.Lead;
import com.fitos.model.LeadActivity;
import com.fitos.model.Role;
import com.fitos.model.User;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.LeadActivityRepository;
import com.fitos.repository.LeadRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.StudioContext;
import com.fitos.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Task #141 — Unit test LeadService: convert hợp lệ / sai trạng thái / đã convert,
 * cùng với đánh dấu LOST và chuẩn hoá source/goal.
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    private static final Long STUDIO_ID = 1L;

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private LeadActivityRepository leadActivityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private LeadAIClient leadAIClient;
    @Mock
    private StudioAccessService studioAccessService;

    @InjectMocks
    private LeadService leadService;

    private Lead lead;

    @BeforeEach
    void setUp() {
        User staff = User.builder()
                .id(5L).studioId(STUDIO_ID).email("staff@fitos.test")
                .passwordHash("hash").fullName("Staff Member").isActive(true)
                .roles(new HashSet<>(List.of(Role.builder().id(1).name("ROLE_MANAGER").build())))
                .build();
        UserPrincipal principal = UserPrincipal.create(staff, STUDIO_ID, List.of("ROLE_MANAGER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lead = Lead.builder()
                .id(10L)
                .studioId(STUDIO_ID)
                .fullName("Tran Van Khach")
                .phone("0909111222")
                .email("khach@fitos.test")
                .goal("WEIGHT_LOSS")
                .source("WALK_IN")
                .status("NEW")
                .build();

        lenientStubAi();
    }

    private void lenientStubAi() {
        org.mockito.Mockito.lenient().when(leadAIClient.scoreLead(any())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Convert lead NEW: tạo Customer và chuyển trạng thái CONVERTED")
    void convertNewLeadCreatesCustomer() {
        when(leadRepository.findByIdAndStudioId(10L, STUDIO_ID)).thenReturn(Optional.of(lead));
        when(customerRepository.findByPhoneAndStudioId("0909111222", STUDIO_ID)).thenReturn(Optional.empty());
        Customer saved = Customer.builder()
                .id(55L).studioId(STUDIO_ID).leadId(10L)
                .fullName("Tran Van Khach").phone("0909111222").status("ACTIVE")
                .build();
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadActivityRepository.findByLeadIdOrderByActivityTimeDesc(10L)).thenReturn(List.of());

        LeadResponse response = leadService.convertLead(10L);

        assertEquals("CONVERTED", response.getStatus());
        assertEquals(55L, response.getConvertedCustomerId());

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertEquals(10L, customerCaptor.getValue().getLeadId());
        assertEquals("WEIGHT_LOSS", customerCaptor.getValue().getFitnessGoal());
    }

    @Test
    @DisplayName("Convert lead đã CONVERTED phải ném BadRequestException")
    void convertAlreadyConvertedLeadFails() {
        lead.setStatus("CONVERTED");
        lead.setConvertedCustomerId(55L);
        when(leadRepository.findByIdAndStudioId(10L, STUDIO_ID)).thenReturn(Optional.of(lead));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> leadService.convertLead(10L));
        assertTrue(exception.getMessage().contains("already been converted"));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Convert lead không tồn tại ném ResourceNotFoundException")
    void convertMissingLeadFails() {
        when(leadRepository.findByIdAndStudioId(99L, STUDIO_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> leadService.convertLead(99L));
    }

    @Test
    @DisplayName("Convert lead có phone trùng hội viên cũ thì tái sử dụng hồ sơ đó")
    void convertReusesExistingCustomer() {
        Customer existing = Customer.builder()
                .id(77L).studioId(STUDIO_ID).fullName("Khach Cu").phone("0909111222").status("ACTIVE")
                .build();
        when(leadRepository.findByIdAndStudioId(10L, STUDIO_ID)).thenReturn(Optional.of(lead));
        when(customerRepository.findByPhoneAndStudioId("0909111222", STUDIO_ID)).thenReturn(Optional.of(existing));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadActivityRepository.findByLeadIdOrderByActivityTimeDesc(10L)).thenReturn(List.of());

        LeadResponse response = leadService.convertLead(10L);

        assertEquals(77L, response.getConvertedCustomerId());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Đánh dấu LOST yêu cầu lý do và ghi activity")
    void markLostRequiresReason() {
        when(leadRepository.findByIdAndStudioId(10L, STUDIO_ID)).thenReturn(Optional.of(lead));

        assertThrows(BadRequestException.class, () -> leadService.markLost(10L, "   ", null));
        verify(leadRepository, never()).save(any(Lead.class));
    }

    @Test
    @DisplayName("Đánh dấu LOST đồng thời lưu lý do và tạo activity NOTE")
    void markLostPersistsReasonAndActivity() {
        when(leadRepository.findByIdAndStudioId(10L, STUDIO_ID)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadActivityRepository.findByLeadIdOrderByActivityTimeDesc(10L)).thenReturn(List.of());
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        LeadResponse response = leadService.markLost(10L, "Không đủ ngân sách", 5L);

        assertEquals("LOST", response.getStatus());
        assertEquals("Không đủ ngân sách", response.getLostReason());

        ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(leadActivityRepository).save(activityCaptor.capture());
        assertTrue(activityCaptor.getValue().getNotes().contains("LOST"));
    }

    @Test
    @DisplayName("Lead CONVERTED không thể chuyển sang LOST")
    void convertedLeadCannotBeMarkedLost() {
        lead.setStatus("CONVERTED");
        lead.setConvertedCustomerId(55L);
        when(leadRepository.findByIdAndStudioId(10L, STUDIO_ID)).thenReturn(Optional.of(lead));

        assertThrows(BadRequestException.class, () -> leadService.markLost(10L, "lý do", null));
    }

    @Test
    @DisplayName("Tạo lead chuẩn hoá source tiếng Việt và tạo activity đầu tiên")
    void createLeadNormalizesSource() {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFullName("  Nguyen  Van A ");
        request.setPhone("0912000111");
        request.setSource("giới thiệu");
        request.setGoal("giảm cân");

        when(userRepository.findById(5L)).thenReturn(Optional.empty());
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> {
            Lead saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        when(leadActivityRepository.save(any(LeadActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leadActivityRepository.findByLeadIdOrderByActivityTimeDesc(anyLong())).thenReturn(List.of());

        LeadResponse response = leadService.createLead(request, 5L);

        assertEquals("REFERRAL", response.getSource());
        assertEquals("WEIGHT_LOSS", response.getGoal());
        assertEquals("NEW", response.getStatus());

        ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(leadActivityRepository).save(activityCaptor.capture());
        assertEquals(STUDIO_ID, activityCaptor.getValue().getStudioId());
        assertNotNull(activityCaptor.getValue().getLead());
    }

    @Test
    @DisplayName("Thêm activity cho lead thuộc studio khác bị chặn")
    void addActivityRejectsForeignLead() {
        when(leadRepository.findByIdAndStudioId(10L, STUDIO_ID)).thenReturn(Optional.empty());
        LeadActivityRequest request = new LeadActivityRequest();
        request.setActivityType("CALL");
        request.setNotes("Gọi lại");

        assertThrows(ResourceNotFoundException.class, () -> leadService.addActivity(10L, request, 5L));
    }
}
