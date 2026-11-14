package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "order_reference", nullable = false, unique = true, length = 50)
    private String orderReference;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_code", length = 50)
    private String discountCode;

    /** PLAN_SALE (bán gói) | RETAIL (bán lẻ tại quầy) | RETAIL_RETURN (trả hàng) */
    @Builder.Default
    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType = "PLAN_SALE";

    /** Khoá idempotency do POS sinh ra, chống tạo trùng khi retry. */
    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    /** Đơn trả hàng trỏ về đơn gốc. */
    @Column(name = "parent_order_id")
    private Long parentOrderId;

    /** Người tạo đơn (thu ngân / nhân viên). */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public void markPaid(Instant paidAt) {
        this.status = "PAID";
        this.paidAt = paidAt;
    }

    public void markPaid(java.time.OffsetDateTime paidAt) {
        this.status = "PAID";
        this.paidAt = paidAt != null ? paidAt.toInstant() : Instant.now();
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Status {
        PENDING, PAID, FAILED, CANCELLED, REFUNDED, EXPIRED
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }

    public boolean isRetail() {
        return "RETAIL".equalsIgnoreCase(orderType);
    }

    public boolean isReturn() {
        return "RETAIL_RETURN".equalsIgnoreCase(orderType);
    }
}
