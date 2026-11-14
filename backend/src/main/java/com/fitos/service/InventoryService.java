package com.fitos.service;

import com.fitos.dto.common.PageResponse;
import com.fitos.dto.retail.RetailDtos.*;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.InventoryTransaction;
import com.fitos.model.Product;
import com.fitos.model.ProductCategory;
import com.fitos.repository.InventoryTransactionRepository;
import com.fitos.repository.OrderRepository;
import com.fitos.repository.ProductCategoryRepository;
import com.fitos.repository.ProductRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.StudioContext;
import com.fitos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ kho: nhập hàng, điều chỉnh tồn, trừ kho khi bán, hoàn kho khi trả.
 *
 * <p>Mọi thay đổi {@code products.stock_quantity} đều ghi một dòng
 * {@code inventory_transactions} để có lịch sử kiểm toán.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    // ---------------------------------------------------------------- stock in
    @Transactional
    public InventoryTransactionResponse stockIn(StockInRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Product product = productRepository.findByIdWithLock(req.getProductId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + req.getProductId()));

        int before = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        int qty = req.getQuantity();
        BigDecimal newCost = req.getUnitCost().setScale(2, RoundingMode.HALF_UP);

        // Giá vốn bình quân gia quyền
        BigDecimal oldCost = product.getCostPrice() == null ? BigDecimal.ZERO : product.getCostPrice();
        BigDecimal weighted = before <= 0
                ? newCost
                : oldCost.multiply(BigDecimal.valueOf(before))
                        .add(newCost.multiply(BigDecimal.valueOf(qty)))
                        .divide(BigDecimal.valueOf((long) before + qty), 2, RoundingMode.HALF_UP);

        product.setStockQuantity(before + qty);
        product.setCostPrice(weighted);
        productRepository.save(product);

        InventoryTransaction tx = transactionRepository.save(InventoryTransaction.of(
                studioId, product.getId(), InventoryTransaction.Type.STOCK_IN,
                before, qty, newCost, null, currentUserId(),
                req.getNote() == null || req.getNote().isBlank() ? "Nhập kho" : req.getNote().trim()));

        log.info("Nhập kho {} {} {} -> {}", qty, Product.unitLabel(product.getUnit()), product.getName(),
                product.getStockQuantity());
        return toResponse(tx, product);
    }

    // ----------------------------------------------------------------- adjust
    @Transactional
    public InventoryTransactionResponse adjust(StockAdjustRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Product product = productRepository.findByIdWithLock(req.getProductId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + req.getProductId()));

        int before = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        int delta = req.getNewQuantity() - before;
        if (delta == 0) {
            throw new BadRequestException("Tồn kho không thay đổi");
        }

        product.setStockQuantity(req.getNewQuantity());
        productRepository.save(product);

        InventoryTransaction tx = transactionRepository.save(InventoryTransaction.of(
                studioId, product.getId(), InventoryTransaction.Type.ADJUSTMENT,
                before, delta, product.getCostPrice(), null, currentUserId(),
                "Điều chỉnh: " + req.getReason().trim()));

        return toResponse(tx, product);
    }

    /**
     * Trừ kho khi bán. Phải gọi trong transaction của đơn hàng, sau khi đã khoá
     * sản phẩm bằng {@code findByIdWithLock}.
     */
    @Transactional
    public void deductForSale(Long studioId, Product product, int quantity, Long orderId, Long actorId) {
        int before = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        if (Boolean.TRUE.equals(product.getTrackInventory()) && before < quantity) {
            throw new BadRequestException("Không đủ tồn: " + product.getName()
                    + " (còn " + before + " " + Product.unitLabel(product.getUnit()) + ")");
        }
        if (Boolean.TRUE.equals(product.getTrackInventory())) {
            product.setStockQuantity(before - quantity);
            productRepository.save(product);
            transactionRepository.save(InventoryTransaction.of(
                    studioId, product.getId(), InventoryTransaction.Type.SALE_OUT,
                    before, -quantity, product.getCostPrice(), orderId, actorId, null));
        }
    }

    /** Hoàn kho khi huỷ đơn hoặc trả hàng. */
    @Transactional
    public void restock(Long studioId, Product product, int quantity, Long orderId, Long actorId,
                        InventoryTransaction.Type type, String note) {
        if (!Boolean.TRUE.equals(product.getTrackInventory())) return;
        int before = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setStockQuantity(before + quantity);
        productRepository.save(product);
        transactionRepository.save(InventoryTransaction.of(
                studioId, product.getId(), type, before, quantity,
                product.getCostPrice(), orderId, actorId, note));
    }

    // ------------------------------------------------------------------ query
    @Transactional(readOnly = true)
    public List<LowStockItem> lowStock() {
        Long studioId = StudioContext.currentStudioId();
        Map<Long, String> categoryNames = categoryRepository
                .findByStudioIdOrderBySortOrderAscNameAsc(studioId).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));

        return productRepository.findLowStock(studioId).stream()
                .map(p -> {
                    int shortage = Math.max(0,
                            (p.getLowStockThreshold() == null ? 0 : p.getLowStockThreshold())
                                    - (p.getStockQuantity() == null ? 0 : p.getStockQuantity()));
                    return LowStockItem.builder()
                            .id(p.getId())
                            .sku(p.getSku())
                            .name(p.getName())
                            .categoryName(p.getCategoryId() == null ? null : categoryNames.get(p.getCategoryId()))
                            .unit(p.getUnit())
                            .stockQuantity(p.getStockQuantity())
                            .lowStockThreshold(p.getLowStockThreshold())
                            .shortage(shortage)
                            .severity(p.isOutOfStock() ? "OUT_OF_STOCK" : "LOW")
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> movements(Long productId, int page, int size) {
        Long studioId = StudioContext.currentStudioId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Page<InventoryTransaction> result = productId == null
                ? transactionRepository.findByStudioIdOrderByCreatedAtDesc(studioId, pageable)
                : transactionRepository.findByStudioIdAndProductIdOrderByCreatedAtDesc(studioId, productId, pageable);

        // Nạp tên sản phẩm + tên người thực hiện một lần (tránh N+1)
        Map<Long, Product> products = productRepository
                .findByStudioIdAndIsActiveTrueOrderByNameAsc(studioId).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return PageResponse.of(result, tx -> toResponse(tx, products.get(tx.getProductId())));
    }

    // ----------------------------------------------------------------- helper
    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private InventoryTransactionResponse toResponse(InventoryTransaction tx, Product product) {
        String actorName = null;
        if (tx.getCreatedBy() != null) {
            actorName = userRepository.findById(tx.getCreatedBy())
                    .map(u -> u.getFullName()).orElse(null);
        }
        String orderRef = tx.getOrderId() == null ? null
                : orderRepository.findById(tx.getOrderId())
                        .map(o -> o.getOrderReference()).orElse(null);

        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .transactionType(tx.getTransactionType())
                .productId(tx.getProductId())
                .productName(product != null ? product.getName() : null)
                .unit(product != null ? product.getUnit() : null)
                .quantityDelta(tx.getQuantityDelta())
                .stockBefore(tx.getStockBefore())
                .stockAfter(tx.getStockAfter())
                .unitCost(tx.getUnitCost())
                .orderId(tx.getOrderId())
                .orderReference(orderRef)
                .note(tx.getNote())
                .createdBy(tx.getCreatedBy())
                .createdByName(actorName)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
