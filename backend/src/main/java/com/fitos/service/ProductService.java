package com.fitos.service;

import com.fitos.dto.common.PageResponse;
import com.fitos.dto.retail.RetailDtos.*;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Product;
import com.fitos.model.ProductCategory;
import com.fitos.repository.ProductCategoryRepository;
import com.fitos.repository.ProductRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Quản lý danh mục và sản phẩm bán lẻ. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    // ------------------------------------------------------------------ category
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(boolean includeInactive) {
        Long studioId = StudioContext.currentStudioId();
        List<ProductCategory> categories = includeInactive
                ? categoryRepository.findByStudioIdOrderBySortOrderAscNameAsc(studioId)
                : categoryRepository.findByStudioIdAndIsActiveTrueOrderBySortOrderAscNameAsc(studioId);
        return categories.stream().map(this::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        Long studioId = StudioContext.currentStudioId();
        String name = req.getName().trim();
        if (categoryRepository.existsByStudioIdAndNameIgnoreCase(studioId, name)) {
            throw new BadRequestException("Danh mục đã tồn tại: " + name);
        }
        ProductCategory category = ProductCategory.builder()
                .studioId(studioId)
                .name(name)
                .description(req.getDescription())
                .sortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder())
                .isActive(req.getIsActive() == null || req.getIsActive())
                .build();
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest req) {
        Long studioId = StudioContext.currentStudioId();
        ProductCategory category = categoryRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục: " + id));
        String name = req.getName().trim();
        if (categoryRepository.existsByStudioIdAndNameIgnoreCaseAndIdNot(studioId, name, id)) {
            throw new BadRequestException("Danh mục đã tồn tại: " + name);
        }
        category.setName(name);
        category.setDescription(req.getDescription());
        if (req.getSortOrder() != null) category.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) category.setIsActive(req.getIsActive());
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse toggleCategory(Long id) {
        Long studioId = StudioContext.currentStudioId();
        ProductCategory category = categoryRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục: " + id));
        category.setIsActive(!Boolean.TRUE.equals(category.getIsActive()));
        return toCategoryResponse(categoryRepository.save(category));
    }

    // ------------------------------------------------------------------- product
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String search, Long categoryId,
                                                        boolean activeOnly, int page, int size) {
        Long studioId = StudioContext.currentStudioId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        String q = (search == null || search.isBlank()) ? null : search.trim();
        Page<Product> result = productRepository.search(studioId, q, categoryId, activeOnly, pageable);

        // Nạp tên danh mục một lần, tránh N+1
        Map<Long, String> categoryNames = categoryRepository
                .findByStudioIdOrderBySortOrderAscNameAsc(studioId).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));

        return PageResponse.of(result, p -> toProductResponse(p, categoryNames.get(p.getCategoryId())));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Long studioId = StudioContext.currentStudioId();
        Product product = productRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
        return toProductResponse(product, categoryName(studioId, product.getCategoryId()));
    }

    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        Long studioId = StudioContext.currentStudioId();
        Product product = productRepository.findByStudioIdAndBarcode(studioId, barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã vạch: " + barcode));
        return toProductResponse(product, categoryName(studioId, product.getCategoryId()));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        Long studioId = StudioContext.currentStudioId();
        String sku = req.getSku().trim().toUpperCase();
        if (productRepository.existsByStudioIdAndSkuIgnoreCase(studioId, sku)) {
            throw new BadRequestException("SKU đã tồn tại: " + sku);
        }
        String barcode = normalizeBarcode(req.getBarcode());
        if (barcode != null && productRepository.existsByStudioIdAndBarcode(studioId, barcode)) {
            throw new BadRequestException("Mã vạch đã tồn tại: " + barcode);
        }
        validateCategory(studioId, req.getCategoryId());
        validatePrices(req.getSellPrice(), req.getCostPrice());
        requireSalePriceIfActive(req.getIsActive() == null || req.getIsActive(), req.getSellPrice());

        Product product = Product.builder()
                .studioId(studioId)
                .categoryId(req.getCategoryId())
                .sku(sku)
                .barcode(barcode)
                .name(req.getName().trim())
                .description(req.getDescription())
                .unit(req.getUnit() == null ? "CAI" : req.getUnit())
                .costPrice(money(req.getCostPrice()))
                .sellPrice(money(req.getSellPrice()))
                .vatRate(req.getVatRate() == null ? BigDecimal.ZERO : req.getVatRate())
                .stockQuantity(0)
                .lowStockThreshold(req.getLowStockThreshold() == null ? 5 : req.getLowStockThreshold())
                .trackInventory(req.getTrackInventory() == null || req.getTrackInventory())
                .imageUrl(req.getImageUrl())
                .isActive(req.getIsActive() == null || req.getIsActive())
                .build();
        return toProductResponse(productRepository.save(product), categoryName(studioId, product.getCategoryId()));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Long studioId = StudioContext.currentStudioId();
        Product product = productRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));

        String sku = req.getSku().trim().toUpperCase();
        if (productRepository.existsByStudioIdAndSkuIgnoreCaseAndIdNot(studioId, sku, id)) {
            throw new BadRequestException("SKU đã tồn tại: " + sku);
        }
        String barcode = normalizeBarcode(req.getBarcode());
        if (barcode != null && productRepository.existsByStudioIdAndBarcodeAndIdNot(studioId, barcode, id)) {
            throw new BadRequestException("Mã vạch đã tồn tại: " + barcode);
        }
        validateCategory(studioId, req.getCategoryId());
        validatePrices(req.getSellPrice(), req.getCostPrice());
        requireSalePriceIfActive(req.getIsActive() == null ? Boolean.TRUE.equals(product.getIsActive())
                : req.getIsActive(), req.getSellPrice());

        product.setSku(sku);
        product.setBarcode(barcode);
        product.setName(req.getName().trim());
        product.setDescription(req.getDescription());
        product.setCategoryId(req.getCategoryId());
        if (req.getUnit() != null) product.setUnit(req.getUnit());
        product.setCostPrice(money(req.getCostPrice()));
        product.setSellPrice(money(req.getSellPrice()));
        if (req.getVatRate() != null) product.setVatRate(req.getVatRate());
        if (req.getLowStockThreshold() != null) product.setLowStockThreshold(req.getLowStockThreshold());
        if (req.getTrackInventory() != null) product.setTrackInventory(req.getTrackInventory());
        product.setImageUrl(req.getImageUrl());
        if (req.getIsActive() != null) product.setIsActive(req.getIsActive());

        return toProductResponse(productRepository.save(product), categoryName(studioId, product.getCategoryId()));
    }

    @Transactional
    public ProductResponse toggleProduct(Long id) {
        Long studioId = StudioContext.currentStudioId();
        Product product = productRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
        requireSalePriceIfActive(!Boolean.TRUE.equals(product.getIsActive()), product.getSellPrice());
        product.setIsActive(!Boolean.TRUE.equals(product.getIsActive()));
        return toProductResponse(productRepository.save(product), categoryName(studioId, product.getCategoryId()));
    }

    // -------------------------------------------------------------------- helper
    private String categoryName(Long studioId, Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findByIdAndStudioId(categoryId, studioId)
                .map(ProductCategory::getName).orElse(null);
    }

    private void validateCategory(Long studioId, Long categoryId) {
        if (categoryId == null) return;
        categoryRepository.findByIdAndStudioId(categoryId, studioId)
                .orElseThrow(() -> new BadRequestException("Danh mục không tồn tại trong studio này"));
    }

    private void validatePrices(BigDecimal sellPrice, BigDecimal costPrice) {
        if (sellPrice == null || sellPrice.signum() < 0) {
            throw new BadRequestException("Giá bán không hợp lệ");
        }
        if (costPrice != null && costPrice.signum() < 0) {
            throw new BadRequestException("Giá vốn không hợp lệ");
        }
    }

    private void requireSalePriceIfActive(boolean active, BigDecimal sellPrice) {
        if (active && (sellPrice == null || sellPrice.signum() <= 0)) {
            throw new BadRequestException("Cần đặt giá bán lớn hơn 0 trước khi mở bán sản phẩm");
        }
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null) return null;
        String trimmed = barcode.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private CategoryResponse toCategoryResponse(ProductCategory category) {
        long count = category.getId() == null ? 0
                : productRepository.countByStudioIdAndCategoryId(category.getStudioId(), category.getId());
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .productCount(count)
                .build();
    }

    public ProductResponse toProductResponse(Product product, String categoryName) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .unit(product.getUnit())
                .unitLabel(Product.unitLabel(product.getUnit()))
                .costPrice(product.getCostPrice())
                .sellPrice(product.getSellPrice())
                .vatRate(product.getVatRate())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .trackInventory(product.getTrackInventory())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .lowStock(product.isLowStock())
                .outOfStock(product.isOutOfStock())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
