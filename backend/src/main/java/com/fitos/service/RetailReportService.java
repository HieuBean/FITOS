package com.fitos.service;

import com.fitos.dto.retail.RetailDtos.*;
import com.fitos.model.Order;
import com.fitos.model.OrderItem;
import com.fitos.model.Product;
import com.fitos.model.ProductCategory;
import com.fitos.repository.*;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/** Tổng kết bán hàng theo ngày cho màn hình quản trị. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetailReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public DailySalesSummaryResponse daily(LocalDate date) {
        Long studioId = StudioContext.currentStudioId();
        LocalDate day = date == null ? LocalDate.now(ZONE) : date;

        Instant from = day.atStartOfDay(ZONE).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(ZONE).toInstant();

        List<Order> orders = orderRepository.findRetailBetween(studioId, from, to);

        BigDecimal grossRevenue = money(orders.stream()
                .filter(o -> "RETAIL".equals(o.getOrderType()) && "PAID".equals(o.getStatus()))
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal returnAmount = money(orders.stream()
                .filter(o -> "RETAIL_RETURN".equals(o.getOrderType()))
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal vatTotal = money(orders.stream()
                .filter(o -> "RETAIL".equals(o.getOrderType()) && "PAID".equals(o.getStatus()))
                .map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        // VAT của hàng trả lại phải được hoàn ngược, nếu không doanh thu thuần
        // sẽ bị tính thuế hai lần trên phần hàng đã trả.
        BigDecimal returnVat = money(orders.stream()
                .filter(o -> "RETAIL_RETURN".equals(o.getOrderType()))
                .map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Nạp tên sản phẩm + danh mục một lần
        Map<Long, Product> products = productRepository
                .findByStudioIdAndIsActiveTrueOrderByNameAsc(studioId).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, String> categoryNames = categoryRepository
                .findByStudioIdOrderBySortOrderAscNameAsc(studioId).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));

        BigDecimal cogs = BigDecimal.ZERO;
        int itemCount = 0;
        Map<Long, int[]> qtyByProduct = new HashMap<>();
        Map<Long, BigDecimal> amountByProduct = new HashMap<>();
        Map<String, int[]> qtyByCategory = new HashMap<>();
        Map<String, BigDecimal> amountByCategory = new HashMap<>();

        List<Long> paidRetailIds = orders.stream()
                .filter(o -> "RETAIL".equals(o.getOrderType()) && "PAID".equals(o.getStatus()))
                .map(Order::getId).toList();

        for (Long orderId : paidRetailIds) {
            for (OrderItem item : orderItemRepository.findByOrderId(orderId)) {
                if (!"PRODUCT".equals(item.getItemType())) continue;
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                itemCount += qty;
                BigDecimal lineCost = (item.getUnitCost() == null ? BigDecimal.ZERO : item.getUnitCost())
                        .multiply(BigDecimal.valueOf(qty));
                cogs = cogs.add(lineCost);

                if (item.getProductId() != null) {
                    qtyByProduct.computeIfAbsent(item.getProductId(), k -> new int[1])[0] += qty;
                    amountByProduct.merge(item.getProductId(),
                            item.getTotalPrice() == null ? BigDecimal.ZERO : item.getTotalPrice(), BigDecimal::add);

                    Product p = products.get(item.getProductId());
                    String catName = (p != null && p.getCategoryId() != null)
                            ? categoryNames.getOrDefault(p.getCategoryId(), "Khác") : "Khác";
                    qtyByCategory.computeIfAbsent(catName, k -> new int[1])[0] += qty;
                    amountByCategory.merge(catName,
                            item.getTotalPrice() == null ? BigDecimal.ZERO : item.getTotalPrice(), BigDecimal::add);
                }
            }
        }

        cogs = money(cogs);

        // Giá vốn của hàng đã trả lại phải được hoàn ngược khỏi COGS, nếu không
        // lãi gộp sẽ bị âm sai (đã gặp: doanh thu thuần 2.69tr nhưng COGS 3.06tr
        // vì vẫn tính phần whey đã trả lại).
        BigDecimal returnCogs = BigDecimal.ZERO;
        List<Long> returnOrderIds = orders.stream()
                .filter(o -> "RETAIL_RETURN".equals(o.getOrderType()))
                .map(Order::getId).toList();
        for (Long orderId : returnOrderIds) {
            for (OrderItem item : orderItemRepository.findByOrderId(orderId)) {
                if (!"PRODUCT".equals(item.getItemType())) continue;
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                returnCogs = returnCogs.add(
                        (item.getUnitCost() == null ? BigDecimal.ZERO : item.getUnitCost())
                                .multiply(BigDecimal.valueOf(qty)));
            }
        }
        returnCogs = money(returnCogs);

        BigDecimal netCogs = money(cogs.subtract(returnCogs));
        BigDecimal netVat = money(vatTotal.subtract(returnVat));
        BigDecimal netRevenue = money(grossRevenue.subtract(returnAmount));

        // Lãi gộp = doanh thu thuần − VAT thuần − giá vốn thuần.
        // (Giá bán đã bao gồm VAT nên phải trừ VAT ra mới ra lãi thật.)
        BigDecimal grossProfit = money(netRevenue.subtract(netVat).subtract(netCogs));

        // Doanh thu theo phương thức thanh toán
        List<PaymentMethodTotal> byMethod = new ArrayList<>();
        if (!paidRetailIds.isEmpty()) {
            Map<String, BigDecimal> amountByMethod = new LinkedHashMap<>();
            Map<String, Integer> countByMethod = new LinkedHashMap<>();
            for (var payment : paymentRepository.findByOrderIdInOrderByCreatedAtDesc(paidRetailIds)) {
                if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) continue;
                String method = payment.getPaymentMethod() == null ? "OTHER" : payment.getPaymentMethod();
                amountByMethod.merge(method, payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount(),
                        BigDecimal::add);
                countByMethod.merge(method, 1, Integer::sum);
            }
            amountByMethod.forEach((method, amount) ->
                    byMethod.add(PaymentMethodTotal.builder()
                            .method(method).amount(money(amount))
                            .count(countByMethod.getOrDefault(method, 0)).build()));
        }

        List<CategoryTotal> byCategory = qtyByCategory.entrySet().stream()
                .map(e -> CategoryTotal.builder()
                        .categoryName(e.getKey())
                        .quantity(e.getValue()[0])
                        .amount(money(amountByCategory.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                        .build())
                .sorted(Comparator.comparing(CategoryTotal::getAmount).reversed())
                .toList();

        List<TopProduct> topProducts = qtyByProduct.entrySet().stream()
                .map(e -> {
                    Product p = products.get(e.getKey());
                    return TopProduct.builder()
                            .productId(e.getKey())
                            .name(p != null ? p.getName() : "Sản phẩm #" + e.getKey())
                            .quantity(e.getValue()[0])
                            .amount(money(amountByProduct.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                            .build();
                })
                .sorted(Comparator.comparing(TopProduct::getQuantity).reversed())
                .limit(10)
                .toList();

        return DailySalesSummaryResponse.builder()
                .date(day.toString())
                .grossRevenue(grossRevenue)
                .returnAmount(returnAmount)
                .netRevenue(netRevenue)
                .vatTotal(netVat)
                .cogs(netCogs)
                .grossProfit(grossProfit)
                .orderCount(paidRetailIds.size())
                .itemCount(itemCount)
                .byPaymentMethod(byMethod)
                .byCategory(byCategory)
                .topProducts(topProducts)
                .build();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
