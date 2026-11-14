package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một lần biến động kho. Mọi thay đổi {@code products.stock_quantity} đều phải
 * đi kèm một dòng ở đây để có lịch sử kiểm toán đầy đủ.
 */
@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** STOCK_IN | SALE_OUT | RETURN_IN | ADJUSTMENT | CANCEL_RETURN */
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "order_id")
    private Long orderId;

    @Column(length = 255)
    private String note;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum Type { STOCK_IN, SALE_OUT, RETURN_IN, ADJUSTMENT, CANCEL_RETURN }

    public static InventoryTransaction of(Long studioId, Long productId, Type type,
                                          int before, int delta, BigDecimal unitCost,
                                          Long orderId, Long actorId, String note) {
        return InventoryTransaction.builder()
                .studioId(studioId)
                .productId(productId)
                .transactionType(type.name())
                .stockBefore(before)
                .quantityDelta(delta)
                .stockAfter(before + delta)
                .unitCost(unitCost)
                .orderId(orderId)
                .createdBy(actorId)
                .note(note)
                .build();
    }
}
