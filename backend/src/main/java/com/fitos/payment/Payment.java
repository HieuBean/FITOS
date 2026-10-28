package com.fitos.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Bảng payments (V2_3 — Người B). PaymentService/callback do Người C viết (#38, #39).
 * UNIQUE(transaction_ref) + partial unique "1 SUCCESS / order" là chốt chặn idempotency.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    public enum Method { CASH, BANK_TRANSFER, CARD, VNPAY, MOMO }

    public enum Status { PENDING, SUCCESS, FAILED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 128)
    private String transactionRef;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private Method paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public void markSuccess(OffsetDateTime now) {
        this.status = Status.SUCCESS;
        this.paidAt = now;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = Status.FAILED;
        this.failureReason = reason;
    }
}
