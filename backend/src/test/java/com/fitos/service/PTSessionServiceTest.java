package com.fitos.service;

import com.fitos.dto.PTDto;
import com.fitos.exception.BusinessException;
import com.fitos.model.Customer;
import com.fitos.model.PTPackage;
import com.fitos.model.PTSession;
import com.fitos.model.Trainer;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.PTPackageRepository;
import com.fitos.repository.PTSessionRepository;
import com.fitos.repository.TrainerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PTSessionServiceTest {

    @Mock
    private PTSessionRepository ptSessionRepo;
    @Mock
    private PTPackageRepository ptPackageRepo;
    @Mock
    private CustomerRepository customerRepo;
    @Mock
    private TrainerRepository trainerRepo;
    @Mock
    private PTPackageService ptPackageService;

    @InjectMocks
    private PTSessionService ptSessionService;

    private Customer customer;
    private Trainer trainer;
    private PTPackage ptPackage;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Le Van B").email("b@test.com").build();
        trainer = Trainer.builder().id(2L).name("Coach Nam").email("nam@test.com").build();
        ptPackage = PTPackage.builder().id(3L).customer(customer).trainer(trainer).totalSessions(10).remainingSessions(8).status("ACTIVE").build();
    }

    @Test
    void testScheduleSession_trainerConflict_shouldThrowException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(trainerRepo.findById(2L)).thenReturn(Optional.of(trainer));

        PTSession existingSession = PTSession.builder().id(99L).startTime(start).endTime(end).build();
        when(ptSessionRepo.findConflictingSessions(2L, start, end)).thenReturn(List.of(existingSession));

        PTDto.ScheduleSessionRequest req = PTDto.ScheduleSessionRequest.builder()
                .customerId(1L)
                .trainerId(2L)
                .startTime(start)
                .endTime(end)
                .build();

        assertThrows(BusinessException.class, () -> ptSessionService.scheduleSession(req));
        verify(ptSessionRepo, never()).save(any());
    }

    @Test
    void testCompleteSession_idempotency_shouldNotDeductTwice() {
        PTSession session = PTSession.builder()
                .id(100L)
                .customer(customer)
                .trainer(trainer)
                .ptPackage(ptPackage)
                .status("COMPLETED")
                .completionRef("REF-UUID-12345")
                .completedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(ptSessionRepo.findById(100L)).thenReturn(Optional.of(session));

        // Call completeSession on already completed session
        PTDto.SessionResponse response = ptSessionService.completeSession(100L);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals("REF-UUID-12345", response.getCompletionRef());
        // Verify ptPackageService.useSession is NEVER called again!
        verify(ptPackageService, never()).useSession(anyLong());
        verify(ptSessionRepo, never()).save(any());
    }

    @Test
    void testCompleteSession_firstTime_shouldGenerateCompletionRefAndDeductSession() {
        PTSession session = PTSession.builder()
                .id(101L)
                .customer(customer)
                .trainer(trainer)
                .ptPackage(ptPackage)
                .status("SCHEDULED")
                .build();

        when(ptSessionRepo.findById(101L)).thenReturn(Optional.of(session));
        when(ptSessionRepo.save(any(PTSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PTDto.SessionResponse response = ptSessionService.completeSession(101L);

        assertEquals("COMPLETED", response.getStatus());
        assertNotNull(response.getCompletionRef());
        assertNotNull(response.getCompletedAt());
        verify(ptPackageService, times(1)).useSession(3L);
        verify(ptSessionRepo, times(1)).save(session);
    }
}

