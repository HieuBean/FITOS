package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sản phẩm bán lẻ tại quầy (thực phẩm bổ sung, nước uống, dụng cụ, phụ kiện).
 *
 * <p>{@code sellPrice} là giá niêm yết <b>đã bao gồm VAT</b> — chuẩn bán lẻ Việt Nam.
 * {@code costPrice} là giá vốn, dùng để tính lãi gộp.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(length = 50)
    private String barcode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** VIEN | CHAI | HOP | CAI | DOI | BO | GOI | KG */
    @Builder.Default
    @Column(nullable = false, length = 10)
    private String unit = "CAI";

    @Builder.Default
    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "sell_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellPrice;

    /** % VAT; sellPrice đã bao gồm VAT này. */
    @Builder.Default
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Builder.Default
    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 5;

    @Builder.Default
    @Column(name = "track_inventory", nullable = false)
    private Boolean trackInventory = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Unit { VIEN, CHAI, HOP, CAI, DOI, BO, GOI, KG }

    /** Nhãn tiếng Việt của đơn vị, dùng cho hoá đơn và thông báo. */
    public static String unitLabel(String unit) {
        if (unit == null) return "cái";
        return switch (unit) {
            case "VIEN" -> "viên";
            case "CHAI" -> "chai";
            case "HOP" -> "hộp";
            case "DOI" -> "đôi";
            case "BO" -> "bộ";
            case "GOI" -> "gói";
            case "KG" -> "kg";
            default -> "cái";
        };
    }

    public boolean isLowStock() {
        return Boolean.TRUE.equals(trackInventory)
                && stockQuantity != null && lowStockThreshold != null
                && stockQuantity <= lowStockThreshold;
    }

    public boolean isOutOfStock() {
        return Boolean.TRUE.equals(trackInventory)
                && stockQuantity != null && stockQuantity <= 0;
    }
}
