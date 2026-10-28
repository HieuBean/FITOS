package com.fitos.commerce;

import com.fitos.membership.Membership;
import com.fitos.order.Order;
import com.fitos.order.OrderStatus;
import com.fitos.payment.Payment;
import com.fitos.plan.PlanType;
import com.fitos.plan.ServicePlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #156: Integration / Business Flow Test: Order → Payment → Membership ACTIVE.
 * Verifies domain logic chaining from order creation to payment success to membership activation.
 */
class CommerceFlowIntegrationTest {

    @Test
    @DisplayName("End-to-end commerce flow: Order PENDING -> Payment SUCCESS -> Order PAID -> Membership ACTIVE")
    void fullCommerceFlow_orderToMembership() {
        // Step 1: Create Plan
        ServicePlan plan = new ServicePlan();
        plan.setId(1L);
        plan.setName("Perform Pro 30");
        plan.setPlanType(PlanType.MEMBERSHIP);
        plan.setPrice(new BigDecimal("490000.00"));
        plan.setDurationDays(30);

        // Step 2: Create Order
        Order order = new Order();
        order.setId(101L);
        order.setOrderReference("ORD-TEST-001");
        order.setCustomerId(50L);
        order.setSubtotalAmount(plan.getPrice());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(plan.getPrice());
        order.setStatus(OrderStatus.PENDING);

        assertTrue(order.isPending());

        // Step 3: Payment processed
        OffsetDateTime now = OffsetDateTime.now();
        Payment payment = new Payment();
        payment.setId(201L);
        payment.setOrderId(order.getId());
        payment.setTransactionRef("TXN-TEST-999");
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(Payment.Method.VNPAY);
        payment.markSuccess(now);

        assertTrue(payment.isSuccess());

        // Step 4: Order marked as PAID upon successful payment
        order.markPaid(payment.getPaidAt());
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(now, order.getPaidAt());

        // Step 5: Service activation triggers Membership creation
        LocalDate today = LocalDate.now();
        Membership membership = Membership.activate(order.getCustomerId(), plan, order.getId(), today);

        assertNotNull(membership);
        assertEquals(order.getCustomerId(), membership.getCustomerId());
        assertEquals(plan.getId(), membership.getPlanId());
        assertEquals(order.getId(), membership.getOrderId());
        assertEquals(Membership.Status.ACTIVE, membership.getStatus());
        assertEquals(today, membership.getStartDate());
        assertEquals(today.plusDays(29), membership.getEndDate());
        assertTrue(membership.isActiveOn(today));
    }

    @Test
    @DisplayName("Combo commerce flow: Order with Membership + PT Package -> Paid -> Activates both services")
    void comboCommerceFlow_activatesBothMembershipAndPtPackage() {
        // Step 1: Create Membership plan and PT Package plan
        ServicePlan memberPlan = new ServicePlan();
        memberPlan.setId(1L);
        memberPlan.setName("Perform Pro 30");
        memberPlan.setPlanType(PlanType.MEMBERSHIP);
        memberPlan.setPrice(new BigDecimal("490000.00"));
        memberPlan.setDurationDays(30);

        ServicePlan ptPlan = new ServicePlan();
        ptPlan.setId(2L);
        ptPlan.setName("PT 10 Sessions");
        ptPlan.setPlanType(PlanType.PT_PACKAGE);
        ptPlan.setPrice(new BigDecimal("3000000.00"));
        ptPlan.setDurationDays(60);
        ptPlan.setTotalSessions(10);

        // Step 2: Calculate pricing for combo
        var pricing = com.fitos.order.OrderPricing.calculate(List.of(
                new com.fitos.order.OrderPricing.Line(memberPlan, 1),
                new com.fitos.order.OrderPricing.Line(ptPlan, 1)
        ), new BigDecimal("100000.00"));

        assertEquals(new BigDecimal("3490000.00"), pricing.subtotal());
        assertEquals(new BigDecimal("3390000.00"), pricing.total());

        // Step 3: Create Order
        Order order = new Order();
        order.setId(102L);
        order.setOrderReference("ORD-COMBO-002");
        order.setCustomerId(88L);
        order.setSubtotalAmount(pricing.subtotal());
        order.setDiscountAmount(pricing.discount());
        order.setTotalAmount(pricing.total());
        order.setStatus(OrderStatus.PENDING);

        // Step 4: Pay
        OffsetDateTime now = OffsetDateTime.now();
        order.markPaid(now);
        assertEquals(OrderStatus.PAID, order.getStatus());

        // Step 5: Activations
        LocalDate today = LocalDate.now();
        Membership membership = Membership.activate(order.getCustomerId(), memberPlan, order.getId(), today);
        com.fitos.membership.PtPackage ptPackage = com.fitos.membership.PtPackage.activate(order.getCustomerId(), ptPlan, order.getId(), today);

        assertNotNull(membership);
        assertEquals(Membership.Status.ACTIVE, membership.getStatus());

        assertNotNull(ptPackage);
        assertEquals(com.fitos.membership.PtPackage.Status.ACTIVE, ptPackage.getStatus());
        assertEquals(10, ptPackage.getTotalSessions());
        assertEquals(10, ptPackage.getRemainingSessions());

        // Step 6: Use 1 PT session
        ptPackage.useOneSession(today);
        assertEquals(9, ptPackage.getRemainingSessions());
        assertEquals(com.fitos.membership.PtPackage.Status.ACTIVE, ptPackage.getStatus());
    }
}
