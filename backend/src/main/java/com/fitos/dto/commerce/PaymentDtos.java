package com.fitos.dto.commerce;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record PaymentRequest(
            @NotNull Long orderId,
            @NotBlank @Pattern(regexp = "CASH|BANK_TRANSFER|VNPAY|MOMO|STRIPE|OTHER",
                    message = "paymentMethod không hợp lệ") String paymentMethod,
            /** Không bắt buộc: nếu gửi lên phải khớp đúng tổng tiền server tự tính. */
            @DecimalMin(value = "0.0") BigDecimal amount,
            String transactionRef
    ) {
    }

    public record PaymentCallbackRequest(
            @NotBlank String transactionRef,
            @NotBlank @Pattern(regexp = "SUCCESS|FAILED|REFUNDED|PENDING",
                    message = "status không hợp lệ") String status,
            String gatewayReference,
            Map<String, Object> payload
    ) {
    }

    public record PaymentResponse(
            Long id,
            Long orderId,
            Long customerId,
            String transactionRef,
            String paymentMethod,
            BigDecimal amount,
            String status,
            Instant paidAt,
            Instant createdAt,
            boolean idempotentReplay
    ) {
    }
}
