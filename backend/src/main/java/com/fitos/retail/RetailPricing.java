package com.fitos.retail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tính tiền cho đơn bán lẻ — hàm thuần, không I/O nên unit-test được.
 *
 * <p>Quy tắc giá:
 * <ul>
 *   <li>Giá bán lấy từ DB ({@code products.sell_price}, {@code service_plans.price}),
 *       KHÔNG bao giờ lấy từ client.</li>
 *   <li>{@code sellPrice} đã bao gồm VAT (chuẩn niêm yết bán lẻ Việt Nam), nên
 *       {@code total = subtotal - discount} và KHÔNG cộng thêm VAT.</li>
 *   <li>{@code vatAmount} trên từng dòng chỉ mang tính thông tin/kê khai:
 *       {@code lineTotal × rate / (100 + rate)}.</li>
 * </ul>
 */
public final class RetailPricing {

    private RetailPricing() {
    }

    /** Một dòng hàng đưa vào tính tiền. */
    public record Line(BigDecimal unitPrice, BigDecimal unitCost, int quantity, BigDecimal vatRate) {
    }

    public record Result(BigDecimal subtotal, BigDecimal discount, BigDecimal tax,
                         BigDecimal total, BigDecimal cogs) {
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * VAT nằm trong giá: phần thuế = tổng dòng × thuế suất / (100 + thuế suất).
     */
    public static BigDecimal vatPortion(BigDecimal lineTotal, BigDecimal vatRate) {
        BigDecimal rate = vatRate == null ? BigDecimal.ZERO : vatRate;
        if (rate.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.valueOf(100).add(rate);
        return lineTotal.multiply(rate)
                .divide(divisor, 2, RoundingMode.HALF_UP);
    }

    public static Result calculate(List<Line> lines,
                                   BigDecimal discountAmount,
                                   BigDecimal discountRate) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất một mặt hàng");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;

        for (Line line : lines) {
            if (line == null || line.unitPrice() == null) {
                throw new IllegalArgumentException("Dòng hàng thiếu đơn giá");
            }
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            }
            if (line.unitPrice().signum() < 0) {
                throw new IllegalArgumentException("Đơn giá không được âm");
            }
            BigDecimal lineTotal = money(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
            subtotal = subtotal.add(lineTotal);
            tax = tax.add(vatPortion(lineTotal, line.vatRate()));
            BigDecimal unitCost = line.unitCost() == null ? BigDecimal.ZERO : line.unitCost();
            cogs = cogs.add(money(unitCost.multiply(BigDecimal.valueOf(line.quantity()))));
        }
        subtotal = money(subtotal);
        tax = money(tax);
        cogs = money(cogs);

        BigDecimal discount;
        if (discountAmount != null) {
            discount = money(discountAmount);
            if (discount.signum() < 0) {
                throw new IllegalArgumentException("Giảm giá không được âm");
            }
            if (discount.compareTo(subtotal) > 0) {
                throw new IllegalArgumentException("Giảm giá vượt quá tạm tính");
            }
        } else if (discountRate != null && discountRate.signum() > 0) {
            discount = money(subtotal.multiply(discountRate));
            if (discount.compareTo(subtotal) > 0) discount = subtotal;
        } else {
            discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = money(subtotal.subtract(discount));

        return new Result(subtotal, discount, tax, total, cogs);
    }

    /** Tiền thối lại cho khách khi trả bằng tiền mặt. */
    public static BigDecimal changeDue(BigDecimal cashReceived, BigDecimal total) {
        if (cashReceived == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = cashReceived.subtract(total == null ? BigDecimal.ZERO : total);
        return change.signum() < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : money(change);
    }
}
