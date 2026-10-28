package com.fitos.membership;

import com.fitos.plan.PlanType;
import com.fitos.plan.ServicePlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #144: Unit test Membership entity & lifecycle.
 * Tests: activation end_date calculation, pause/resume end_date extension.
 */
class MembershipLifecycleTest {

    @Test
    @DisplayName("Activation calculates end_date correctly inclusive of start_date")
    void activate_calculatesEndDate() {
        ServicePlan plan = new ServicePlan();
        plan.setId(10L);
        plan.setName("Monthly 30 Days");
        plan.setPlanType(PlanType.MEMBERSHIP);
        plan.setPrice(BigDecimal.valueOf(500000));
        plan.setDurationDays(30);

        LocalDate start = LocalDate.of(2026, 10, 1);
        Membership membership = Membership.activate(100L, plan, 1L, start);

        assertEquals(start, membership.getStartDate());
        // 30 days inclusive: Oct 1 + 29 days = Oct 30
        assertEquals(LocalDate.of(2026, 10, 30), membership.getEndDate());
        assertEquals(Membership.Status.ACTIVE, membership.getStatus());
    }

    @Test
    @DisplayName("Pause and resume extends end_date by the exact number of paused days")
    void pauseAndResume_extendsEndDate() {
        ServicePlan plan = new ServicePlan();
        plan.setId(10L);
        plan.setDurationDays(30);

        LocalDate start = LocalDate.of(2026, 10, 1);
        Membership m = Membership.activate(100L, plan, 1L, start);
        LocalDate initialEnd = m.getEndDate(); // Oct 30

        // Pause on Oct 10
        LocalDate pauseDate = LocalDate.of(2026, 10, 10);
        m.pause(pauseDate);

        assertEquals(Membership.Status.PAUSED, m.getStatus());
        assertEquals(pauseDate, m.getPausedAt());

        // Resume 5 days later on Oct 15
        LocalDate resumeDate = LocalDate.of(2026, 10, 15);
        long pausedDays = m.resume(resumeDate);

        assertEquals(5, pausedDays);
        assertEquals(5, m.getTotalPausedDays());
        assertEquals(Membership.Status.ACTIVE, m.getStatus());
        assertNull(m.getPausedAt());
        // End date should be extended by 5 days: Oct 30 + 5 days = Nov 4
        assertEquals(initialEnd.plusDays(5), m.getEndDate());
    }

    @Test
    @DisplayName("Cannot pause non-active membership")
    void pause_nonActive_throwsException() {
        Membership m = new Membership();
        m.setStatus(Membership.Status.EXPIRED);

        assertThrows(IllegalStateException.class, () -> m.pause(LocalDate.now()));
    }

    @Test
    @DisplayName("Cannot resume non-paused membership")
    void resume_nonPaused_throwsException() {
        Membership m = new Membership();
        m.setStatus(Membership.Status.ACTIVE);

        assertThrows(IllegalStateException.class, () -> m.resume(LocalDate.now()));
    }

    @Test
    @DisplayName("Resume date before pause date throws IllegalArgumentException")
    void resume_beforePauseDate_throwsException() {
        ServicePlan plan = new ServicePlan();
        plan.setId(10L);
        plan.setDurationDays(30);

        LocalDate start = LocalDate.of(2026, 10, 1);
        Membership m = Membership.activate(100L, plan, 1L, start);

        LocalDate pauseDate = LocalDate.of(2026, 10, 10);
        m.pause(pauseDate);

        LocalDate invalidResume = LocalDate.of(2026, 10, 5);
        assertThrows(IllegalArgumentException.class, () -> m.resume(invalidResume));
    }

    @Test
    @DisplayName("Days remaining calculation returns correct number of days")
    void daysRemaining_calculatesCorrectly() {
        ServicePlan plan = new ServicePlan();
        plan.setId(10L);
        plan.setDurationDays(30);

        LocalDate start = LocalDate.of(2026, 10, 1);
        Membership m = Membership.activate(100L, plan, 1L, start); // ends Oct 30

        assertEquals(30, m.daysRemaining(LocalDate.of(2026, 10, 1)));
        assertEquals(21, m.daysRemaining(LocalDate.of(2026, 10, 10)));
        assertEquals(1, m.daysRemaining(LocalDate.of(2026, 10, 30)));
        assertEquals(0, m.daysRemaining(LocalDate.of(2026, 10, 31)));
    }
}
