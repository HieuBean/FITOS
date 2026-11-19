package com.fitos.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PurchaseReceiptDtos {
    private PurchaseReceiptDtos() {}

    public record LineInput(@NotNull Long productId,
                            @NotNull @Positive Integer quantity,
                            @NotNull @DecimalMin("0") BigDecimal unitCost) {}

    public record SaveRequest(@NotBlank @Size(max = 150) String supplierName,
                              @Size(max = 255) String note,
                              @NotEmpty List<@Valid LineInput> items) {}

    public record Line(Long id, Long productId, String productName, int quantity,
                       BigDecimal unitCost, BigDecimal lineTotal) {}

    public record Receipt(Long id, String receiptNo, String supplierName, String note,
                          String status, BigDecimal totalAmount, Long createdBy,
                          Instant confirmedAt, Instant createdAt, List<Line> items) {}

    public record Page(List<Receipt> content, long totalElements, int page, int size) {}
}
