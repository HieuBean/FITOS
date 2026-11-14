package com.fitos.dto.retail;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** DTO cho module bán lẻ / POS. */
public final class RetailDtos {

    private RetailDtos() {
    }

    // ------------------------------------------------------------------ category
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryRequest {
        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 100)
        private String name;
        private String description;
        private Integer sortOrder;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String description;
        private Integer sortOrder;
        private Boolean isActive;
        private long productCount;
    }

    // ------------------------------------------------------------------- product
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductRequest {
        @NotBlank(message = "Mã SKU không được để trống")
        @Pattern(regexp = "[A-Za-z0-9._-]{2,50}", message = "SKU chỉ gồm chữ, số và . _ -")
        private String sku;
        private String barcode;
        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(max = 150)
        private String name;
        private String description;
        private Long categoryId;
        @Pattern(regexp = "VIEN|CHAI|HOP|CAI|DOI|BO|GOI|KG", message = "Đơn vị không hợp lệ")
        private String unit;
        @NotNull @DecimalMin(value = "0")
        private BigDecimal costPrice;
        @NotNull @DecimalMin(value = "0")
        private BigDecimal sellPrice;
        @DecimalMin(value = "0") @DecimalMax(value = "100")
        private BigDecimal vatRate;
        @Min(0)
        private Integer lowStockThreshold;
        private Boolean trackInventory;
        private String imageUrl;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductResponse {
        private Long id;
        private String sku;
        private String barcode;
        private String name;
        private String description;
        private Long categoryId;
        private String categoryName;
        private String unit;
        private String unitLabel;
        private BigDecimal costPrice;
        private BigDecimal sellPrice;
        private BigDecimal vatRate;
        private Integer stockQuantity;
        private Integer lowStockThreshold;
        private Boolean trackInventory;
        private String imageUrl;
        private Boolean isActive;
        private boolean lowStock;
        private boolean outOfStock;
        private Instant createdAt;
        private Instant updatedAt;
    }

    // ----------------------------------------------------------------- inventory
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockInRequest {
        @NotNull(message = "Cần chọn sản phẩm")
        private Long productId;
        @NotNull @Positive(message = "Số lượng nhập phải lớn hơn 0")
        private Integer quantity;
        @NotNull @DecimalMin(value = "0")
        private BigDecimal unitCost;
        @Size(max = 255)
        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockAdjustRequest {
        @NotNull
        private Long productId;
        @NotNull @Min(0)
        private Integer newQuantity;
        @NotBlank(message = "Phải ghi lý do điều chỉnh")
        @Size(min = 5, max = 255)
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryTransactionResponse {
        private Long id;
        private String transactionType;
        private Long productId;
        private String productName;
        private String unit;
        private Integer quantityDelta;
        private Integer stockBefore;
        private Integer stockAfter;
        private BigDecimal unitCost;
        private Long orderId;
        private String orderReference;
        private String note;
        private Long createdBy;
        private String createdByName;
        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockItem {
        private Long id;
        private String sku;
        private String name;
        private String categoryName;
        private String unit;
        private Integer stockQuantity;
        private Integer lowStockThreshold;
        private int shortage;
        private String severity;
    }

    // --------------------------------------------------------------------- order
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetailOrderItemRequest {
        private Long productId;
        private Long planId;
        private Long sessionId;
        @NotNull(message = "Số lượng không được để trống")
        @Positive(message = "Số lượng phải lớn hơn 0")
        @Max(value = 999, message = "Số lượng tối đa 999")
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetailOrderCreateRequest {
        @NotNull(message = "Cần chọn khách hàng")
        private Long customerId;
        @NotEmpty(message = "Giỏ hàng đang trống")
        @Valid
        private List<RetailOrderItemRequest> items;
        @DecimalMin(value = "0")
        private BigDecimal discountAmount;
        private String discountCode;
        @Pattern(regexp = "CASH|CARD|BANK_TRANSFER|VNPAY|MOMO|OTHER",
                message = "Phương thức thanh toán không hợp lệ")
        private String paymentMethod;
        @DecimalMin(value = "0")
        private BigDecimal cashReceived;
        @Size(max = 1000)
        private String notes;
        @Size(max = 64)
        private String clientRequestId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayOrderRequest {
        @NotBlank
        @Pattern(regexp = "CASH|CARD|BANK_TRANSFER|VNPAY|MOMO|OTHER",
                message = "Phương thức thanh toán không hợp lệ")
        private String paymentMethod;
        private String transactionRef;
        @DecimalMin(value = "0")
        private BigDecimal cashReceived;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetailReturnItemRequest {
        @NotNull
        private Long orderItemId;
        @NotNull @Positive
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetailReturnRequest {
        @NotEmpty(message = "Chọn ít nhất một mặt hàng để trả")
        @Valid
        private List<RetailReturnItemRequest> items;
        @NotBlank(message = "Phải ghi lý do trả hàng")
        @Size(max = 255)
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetailOrderItemResponse {
        private Long id;
        private String itemType;
        private Long productId;
        private Long planId;
        private Long sessionId;
        private String itemName;
        private String unit;
        private String unitLabel;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private BigDecimal vatRate;
        private BigDecimal vatAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetailOrderResponse {
        private Long id;
        private String orderReference;
        private String orderType;
        private String status;
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private BigDecimal subtotalAmount;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private int itemCount;
        private List<RetailOrderItemResponse> items;
        private String paymentMethod;
        private String paymentStatus;
        private BigDecimal cashReceived;
        private BigDecimal changeDue;
        private String notes;
        private Long parentOrderId;
        private Instant paidAt;
        private Instant createdAt;
        private boolean idempotentReplay;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceiptResponse {
        private String studioName;
        private String studioAddress;
        private String studioPhone;
        private String orderReference;
        private Instant issuedAt;
        private String cashierName;
        private String customerName;
        private String customerPhone;
        private List<RetailOrderItemResponse> items;
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal vat;
        private BigDecimal total;
        private String paymentMethod;
        private BigDecimal cashReceived;
        private BigDecimal changeDue;
        private String note;
    }

    // -------------------------------------------------------------------- report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethodTotal {
        private String method;
        private BigDecimal amount;
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryTotal {
        private String categoryName;
        private int quantity;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProduct {
        private Long productId;
        private String name;
        private int quantity;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailySalesSummaryResponse {
        private String date;
        private BigDecimal grossRevenue;
        private BigDecimal returnAmount;
        private BigDecimal netRevenue;
        private BigDecimal vatTotal;
        private BigDecimal cogs;
        private BigDecimal grossProfit;
        private int orderCount;
        private int itemCount;
        private List<PaymentMethodTotal> byPaymentMethod;
        private List<CategoryTotal> byCategory;
        private List<TopProduct> topProducts;
    }

    // ------------------------------------------------------------ quick customer
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuickCustomerRequest {
        @NotBlank(message = "Tên khách không được để trống")
        @Size(max = 100)
        private String fullName;
        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "0\\d{8,10}", message = "Số điện thoại không hợp lệ")
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuickCustomerResponse {
        private Long id;
        private String fullName;
        private String phone;
        private boolean created;
    }
}
