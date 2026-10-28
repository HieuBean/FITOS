package com.fitos.order.dto;

import com.fitos.order.Order;
import com.fitos.order.OrderItem;
import com.fitos.order.OrderStatus;
import com.fitos.payment.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    /**
     * Body POST /api/orders. KHÔNG có trường giá/tổng tiền — server tự tính từ service_plans (#35).
     * customerId: bắt buộc với nhân viên, bị bỏ qua với MEMBER (lấy theo tài khoản).
     */
    public record CreateOrderRequest(
            Long customerId,
            @NotEmpty @Size(max = 2) List<@Valid OrderItemRequest> items,
            @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal discountAmount,
            @Size(max = 500) String notes
    ) {
    }

    public record OrderItemRequest(
            @NotNull Long planId,
            @Min(1) @Max(1) Integer quantity
    ) {
        public int quantityOrDefault() {
            return quantity == null ? 1 : quantity;
        }
    }

    public record OrderItemResponse(Long planId, String planName, BigDecimal unitPrice, int quantity,
                                    BigDecimal lineTotal) {
        public static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(i.getPlanId(), i.getPlanName(), i.getUnitPrice(), i.getQuantity(),
                    i.getLineTotal());
        }
    }

    public record PaymentSummary(Long id, String transactionRef, BigDecimal amount, String paymentMethod,
                                 String status, OffsetDateTime paidAt, String failureReason) {
        public static PaymentSummary from(Payment p) {
            return new PaymentSummary(p.getId(), p.getTransactionRef(), p.getAmount(),
                    p.getPaymentMethod().name(), p.getStatus().name(), p.getPaidAt(), p.getFailureReason());
        }
    }

    public record OrderResponse(
            Long id,
            String orderReference,
            Long customerId,
            String customerName,
            String customerEmail,
            List<OrderItemResponse> items,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            OrderStatus status,
            String notes,
            Long createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime paidAt,
            OffsetDateTime cancelledAt,
            List<PaymentSummary> payments
    ) {
        public static OrderResponse from(Order o, String customerName, String customerEmail,
                                         List<PaymentSummary> payments) {
            return new OrderResponse(o.getId(), o.getOrderReference(), o.getCustomerId(), customerName,
                    customerEmail, o.getItems().stream().map(OrderItemResponse::from).toList(),
                    o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(), o.getStatus(),
                    o.getNotes(), o.getCreatedBy(), o.getCreatedAt(), o.getPaidAt(), o.getCancelledAt(),
                    payments == null ? List.of() : payments);
        }
    }
}
