package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "plan_id")
    private Long planId;

    /** PLAN (bán gói) | PRODUCT (bán lẻ) | BOOKING (bán suất lớp) */
    @Builder.Default
    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType = "PLAN";

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "booking_id")
    private Long bookingId;

    /** Ảnh chụp tên hàng tại thời điểm bán (giá và tên có thể đổi về sau). */
    @Column(name = "item_name", length = 150)
    private String itemName;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /** Giá vốn đơn vị tại thời điểm bán — dùng tính lãi gộp. */
    @Builder.Default
    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;
}
