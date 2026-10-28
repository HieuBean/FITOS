package com.fitos.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng orders (V2_2 — Người B).
 * Lưu ý: tên class "Order" trùng từ khoá SQL nhưng @Table(name="orders") nên an toàn.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_reference", nullable = false, unique = true, length = 64)
    private String orderReference;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** users.id của nhân viên tạo đơn (null nếu member tự mua). */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    /** Chỉ đơn PENDING mới huỷ được (#37). */
    public void cancel(OffsetDateTime now) {
        if (!isPending()) {
            throw new IllegalStateException("Only PENDING orders can be cancelled (current: " + status + ")");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = now;
    }

    /** Gọi bởi PaymentService (C) khi thanh toán SUCCESS. */
    public void markPaid(OffsetDateTime now) {
        if (!isPending()) {
            throw new IllegalStateException("Only PENDING orders can be paid (current: " + status + ")");
        }
        this.status = OrderStatus.PAID;
        this.paidAt = now;
    }
}
