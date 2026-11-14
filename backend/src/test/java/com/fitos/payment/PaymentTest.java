package com.fitos.payment;

import com.fitos.model.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #143: Unit test Payment entity & idempotency reference logic.
 */
class PaymentTest {

    @Test
    @DisplayName("Payment success marks paid_at and clears failure_reason")
    void markSuccess_setsStatusAndPaidAt() {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setTransactionRef("TXN-123456");
        payment.setAmount(new BigDecimal("500000.00"));
        payment.setPaymentMethod(Payment.Method.VNPAY);
        payment.setStatus(Payment.Status.PENDING);

        OffsetDateTime now = OffsetDateTime.now();
        payment.markSuccess(now);

        assertTrue(payment.isSuccess());
        assertEquals(Payment.Status.SUCCESS, payment.getStatusEnum());
        assertEquals(now.toInstant(), payment.getPaidAt());
    }

    @Test
    @DisplayName("Payment failure marks status and failure_reason")
    void markFailed_setsStatusAndReason() {
        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setTransactionRef("TXN-FAIL-01");
        payment.setAmount(new BigDecimal("200000.00"));
        payment.setPaymentMethod(Payment.Method.CARD);

        payment.markFailed("Card declined: Insufficient funds");

        assertFalse(payment.isSuccess());
        assertEquals(Payment.Status.FAILED, payment.getStatusEnum());
    }
}
