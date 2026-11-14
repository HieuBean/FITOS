package com.fitos.order;

import com.fitos.model.ServicePlan;
import com.fitos.model.ServicePlan.PlanType;

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
 *   <li>Mỗi đơn tối đa 1 gói MEMBERSHIP + 1 gói PT_PACKAGE.</li>
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
            PlanType type = plan.getPlanTypeEnum();
            if (!seenTypes.add(type)) {
                throw new IllegalArgumentException("An order can contain at most one " + type + " plan");
            }
            if (plan.getPrice() == null || plan.getPrice().signum() < 0) {
                throw new IllegalArgumentException("Plan price must be non-negative: " + plan.getName());
            }
            subtotal = subtotal.add(plan.getPrice().setScale(2, RoundingMode.HALF_UP));
        }

        // scale 2 kể cả khi null để kết quả luôn có dạng 0.00 (nhất quán với cột NUMERIC(x,2)).
        BigDecimal discount = discountAmount == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : discountAmount.setScale(2, RoundingMode.HALF_UP);
        if (discount.signum() < 0) {
            throw new IllegalArgumentException("Discount must not be negative: " + discount.toPlainString());
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "Discount amount (" + discount.toPlainString() + ") exceeds subtotal (" + subtotal.toPlainString() + ")");
        }

        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        return new Result(subtotal, discount, total);
    }
}
