package com.fitos.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InvoiceDtos {
    private InvoiceDtos() {}

    public record SearchRequest(Integer page, Integer size, String keyword) {}
    public record Summary(Long id, String reference, String orderType, String customerName,
                          String employeeName, BigDecimal totalAmount, BigDecimal returnedAmount,
                          BigDecimal netAmount, Instant paidAt) {}
    public record Item(Long id, String itemType, Long productId, String name,
                       int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {}
    public record Detail(Summary invoice, List<Item> items, List<Summary> returns) {}
    public record Page(List<Summary> content, long totalElements, int page, int size) {}
}
