package com.fitos.order;

import com.fitos.plan.PlanType;
import com.fitos.plan.ServicePlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #142: Unit test OrderService & OrderPricing.
 * Tests: total calculation, discount application, negative discount rejection,
 * discount exceeding subtotal rejection, duplicate plan type prevention.
 */
class OrderPricingTest {

    private ServicePlan createPlan(Long id, String name, PlanType type, BigDecimal price) {
        ServicePlan plan = new ServicePlan();
        plan.setId(id);
        plan.setName(name);
        plan.setPlanType(type);
        plan.setPrice(price);
        plan.setDurationDays(30);
        return plan;
    }

    @Test
    @DisplayName("Should correctly calculate subtotal and total without discount")
    void calculate_withoutDiscount_success() {
        ServicePlan plan = createPlan(1L, "Gold Plan", PlanType.MEMBERSHIP, new BigDecimal("500000.00"));
        List<OrderPricing.Line> lines = List.of(new OrderPricing.Line(plan, 1));

        OrderPricing.Result result = OrderPricing.calculate(lines, BigDecimal.ZERO);

        assertEquals(new BigDecimal("500000.00"), result.subtotal());
        assertEquals(new BigDecimal("0.00"), result.discount());
        assertEquals(new BigDecimal("500000.00"), result.total());
    }

    @Test
    @DisplayName("Should correctly calculate total with valid discount")
    void calculate_withValidDiscount_success() {
        ServicePlan plan = createPlan(1L, "Gold Plan", PlanType.MEMBERSHIP, new BigDecimal("500000.00"));
        List<OrderPricing.Line> lines = List.of(new OrderPricing.Line(plan, 1));

        OrderPricing.Result result = OrderPricing.calculate(lines, new BigDecimal("50000.00"));

        assertEquals(new BigDecimal("500000.00"), result.subtotal());
        assertEquals(new BigDecimal("500000.00").subtract(new BigDecimal("50000.00")), result.total());
    }

    @Test
    @DisplayName("Should allow combo of 1 MEMBERSHIP and 1 PT_PACKAGE")
    void calculate_combo_success() {
        ServicePlan membership = createPlan(1L, "Gold Plan", PlanType.MEMBERSHIP, new BigDecimal("500000.00"));
        ServicePlan pt = createPlan(2L, "PT 10 Sessions", PlanType.PT_PACKAGE, new BigDecimal("3000000.00"));
        pt.setTotalSessions(10);

        List<OrderPricing.Line> lines = List.of(
                new OrderPricing.Line(membership, 1),
                new OrderPricing.Line(pt, 1)
        );

        OrderPricing.Result result = OrderPricing.calculate(lines, new BigDecimal("200000.00"));

        assertEquals(new BigDecimal("3500000.00"), result.subtotal());
        assertEquals(new BigDecimal("3300000.00"), result.total());
    }

    @Test
    @DisplayName("Should reject negative discount")
    void calculate_negativeDiscount_throwsException() {
        ServicePlan plan = createPlan(1L, "Gold Plan", PlanType.MEMBERSHIP, new BigDecimal("500000.00"));
        List<OrderPricing.Line> lines = List.of(new OrderPricing.Line(plan, 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                OrderPricing.calculate(lines, new BigDecimal("-10000.00"))
        );
        assertTrue(ex.getMessage().contains("Discount must not be negative"));
    }

    @Test
    @DisplayName("Should reject discount exceeding subtotal (negative total prevention)")
    void calculate_discountExceedingSubtotal_throwsException() {
        ServicePlan plan = createPlan(1L, "Gold Plan", PlanType.MEMBERSHIP, new BigDecimal("500000.00"));
        List<OrderPricing.Line> lines = List.of(new OrderPricing.Line(plan, 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                OrderPricing.calculate(lines, new BigDecimal("600000.00"))
        );
        assertTrue(ex.getMessage().contains("exceeds subtotal"));
    }

    @Test
    @DisplayName("Should reject multiple plans of same plan type in single order")
    void calculate_duplicatePlanType_throwsException() {
        ServicePlan plan1 = createPlan(1L, "Gold Plan", PlanType.MEMBERSHIP, new BigDecimal("500000.00"));
        ServicePlan plan2 = createPlan(2L, "Platinum Plan", PlanType.MEMBERSHIP, new BigDecimal("800000.00"));

        List<OrderPricing.Line> lines = List.of(
                new OrderPricing.Line(plan1, 1),
                new OrderPricing.Line(plan2, 1)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                OrderPricing.calculate(lines, BigDecimal.ZERO)
        );
        assertTrue(ex.getMessage().contains("at most one MEMBERSHIP"));
    }

    @Test
    @DisplayName("Should reject empty or null lines")
    void calculate_emptyLines_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                OrderPricing.calculate(List.of(), BigDecimal.ZERO)
        );
        assertThrows(IllegalArgumentException.class, () ->
                OrderPricing.calculate(null, BigDecimal.ZERO)
        );
    }

    @Test
    @DisplayName("Should reject line with quantity not equal to 1")
    void calculate_invalidQuantity_throwsException() {
        ServicePlan plan = createPlan(1L, "Gold Plan", PlanType.MEMBERSHIP, new BigDecimal("500000.00"));
        List<OrderPricing.Line> lines = List.of(new OrderPricing.Line(plan, 2));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                OrderPricing.calculate(lines, BigDecimal.ZERO)
        );
        assertTrue(ex.getMessage().contains("Quantity must be 1"));
    }

    @Test
    @DisplayName("Should default null discount to 0.00")
    void calculate_nullDiscount_defaultsToZero() {
        ServicePlan plan = createPlan(1L, "Gold Plan", PlanType.MEMBERSHIP, new BigDecimal("500000.00"));
        List<OrderPricing.Line> lines = List.of(new OrderPricing.Line(plan, 1));

        OrderPricing.Result result = OrderPricing.calculate(lines, null);

        assertEquals(new BigDecimal("500000.00"), result.subtotal());
        assertEquals(new BigDecimal("0.00"), result.discount());
        assertEquals(new BigDecimal("500000.00"), result.total());
    }

    @Test
    @DisplayName("Should reject plan with negative price")
    void calculate_negativePlanPrice_throwsException() {
        ServicePlan plan = createPlan(1L, "Freebie Plan", PlanType.MEMBERSHIP, new BigDecimal("-100.00"));
        List<OrderPricing.Line> lines = List.of(new OrderPricing.Line(plan, 1));

        assertThrows(IllegalArgumentException.class, () ->
                OrderPricing.calculate(lines, BigDecimal.ZERO)
        );
    }
}
