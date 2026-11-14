package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 100)
    private String transactionRef;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "paid_at")
    private Instant paidAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_response", columnDefinition = "jsonb")
    private String payloadResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum Method {
        CASH, CARD, BANK_TRANSFER, VNPAY, MOMO
    }

    public enum Status {
        PENDING, SUCCESS, FAILED
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public void markSuccess(Instant paidAt) {
        this.status = "SUCCESS";
        this.paidAt = paidAt;
    }

    public void markSuccess(java.time.OffsetDateTime paidAt) {
        this.status = "SUCCESS";
        this.paidAt = paidAt != null ? paidAt.toInstant() : Instant.now();
    }

    public void setPaymentMethod(String method) {
        this.paymentMethod = method;
    }

    public void setPaymentMethod(Method method) {
        this.paymentMethod = method != null ? method.name() : null;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatus(Status status) {
        this.status = status != null ? status.name() : null;
    }

    public Status getStatusEnum() {
        try {
            return status != null ? Status.valueOf(status) : Status.PENDING;
        } catch (Exception e) {
            return Status.PENDING;
        }
    }

    public void markFailed(String reason) {
        this.status = "FAILED";
    }
}
