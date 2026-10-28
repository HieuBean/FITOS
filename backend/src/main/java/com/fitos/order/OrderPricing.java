package com.fitos.order;

import com.fitos.plan.PlanType;
import com.fitos.plan.ServicePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Tính tiền đơn hàng thuần tuý (không I/O) → dễ unit test (#142).
 * Quy tắc:
 * <ul>
 *   <li>Giá lấy từ ServicePlan (server-side), không bao giờ từ client.</li>
 *   <li>Giảm giá ≥ 0 và ≤ subtotal → tổng không bao giờ âm.</li>
 *   <li>Mỗi đơn tối đa 1 gói MEMBERSHIP + 1 gói PT_PACKAGE (khớp unique index
 *       uq_memberships_order / uq_pt_packages_order trong V2_4).</li>
 * </ul>
 */
public final class OrderPricing {

    private OrderPricing() {
    }

    public record Line(ServicePlan plan, int quantity) {
    }

    public record Result(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }

    public static Result calculate(List<Line> lines, BigDecimal discountAmount) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        Set<PlanType> seenTypes = EnumSet.noneOf(PlanType.class);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Line line : lines) {
            if (line == null || line.plan() == null) {
                throw new IllegalArgumentException("Order line and plan must not be null");
            }
            ServicePlan plan = line.plan();
            if (line.quantity() != 1) {
                throw new IllegalArgumentException("Quantity must be 1 for plan " + plan.getName());
            }
            if (!seenTypes.add(plan.getPlanType())) {
                throw new IllegalArgumentException("An order can contain at most one " + plan.getPlanType() + " plan");
            }
            if (plan.getPrice() == null || plan.getPrice().signum() < 0) {
                throw new IllegalArgumentException("Invalid price for plan " + plan.getName());
            }
            subtotal = subtotal.add(plan.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
        subtotal = scale(subtotal);

        BigDecimal discount = scale(discountAmount == null ? BigDecimal.ZERO : discountAmount);
        if (discount.signum() < 0) {
            throw new IllegalArgumentException("Discount must not be negative");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount (" + discount + ") exceeds subtotal (" + subtotal + ")");
        }
        return new Result(subtotal, discount, subtotal.subtract(discount));
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
