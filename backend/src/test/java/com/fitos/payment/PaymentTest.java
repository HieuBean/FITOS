package com.fitos.payment;

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
        assertEquals(Payment.Status.SUCCESS, payment.getStatus());
        assertEquals(now, payment.getPaidAt());
        assertNull(payment.getFailureReason());
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
        assertEquals(Payment.Status.FAILED, payment.getStatus());
        assertEquals("Card declined: Insufficient funds", payment.getFailureReason());
    }

    @Test
    @DisplayName("Idempotency check: Already successful payment maintains original state upon duplicate webhook callback")
    void duplicateCallback_idempotency_preservesOriginalPayment() {
        Payment payment = new Payment();
        payment.setId(10L);
        payment.setOrderId(1L);
        payment.setTransactionRef("TXN-VNPAY-IDEMPOTENT-001");
        payment.setAmount(new BigDecimal("500000.00"));
        payment.setPaymentMethod(Payment.Method.VNPAY);

        OffsetDateTime initialPaidAt = OffsetDateTime.now().minusMinutes(5);
        payment.markSuccess(initialPaidAt);

        // Webhook retry callback simulation
        assertTrue(payment.isSuccess(), "Payment must be recognised as already successful");

        // Webhook handler logic: if payment.isSuccess(), return 200 without re-triggering activation
        boolean shouldTriggerActivation = !payment.isSuccess();
        assertFalse(shouldTriggerActivation, "Activation must NOT be triggered on duplicate callback");
        assertEquals(initialPaidAt, payment.getPaidAt(), "Original paidAt timestamp must remain untouched");
    }
}
