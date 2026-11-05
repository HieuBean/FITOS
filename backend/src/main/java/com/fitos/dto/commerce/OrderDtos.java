package com.fitos.dto.commerce;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemRequest(@NotNull Long planId, @NotNull @Positive Integer quantity) {
    }

    public record OrderCreateRequest(
            @NotNull Long customerId,
            @NotEmpty @Valid List<OrderItemRequest> items,
            String discountCode,
            String notes
    ) {
    }

    public record OrderItemResponse(
            Long id,
            Long planId,
            String planName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
    }

    public record OrderResponse(
            Long id,
            Long customerId,
            String customerName,
            String orderReference,
            String status,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String discountCode,
            String notes,
            List<OrderItemResponse> items,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
