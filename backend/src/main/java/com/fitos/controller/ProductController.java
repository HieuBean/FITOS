package com.fitos.controller;

import com.fitos.common.security.Roles;
import com.fitos.dto.common.PageResponse;
import com.fitos.dto.retail.RetailDtos.*;
import com.fitos.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Danh mục & sản phẩm bán lẻ (thực phẩm bổ sung, nước uống, dụng cụ, phụ kiện). */
@RestController
@RequestMapping("/api/retail")
@RequiredArgsConstructor
@Tag(name = "Retail Catalog", description = "Danh mục và sản phẩm bán lẻ tại quầy")
public class ProductController {

    private final ProductService productService;

    // ------------------------------------------------------------------ category

    @GetMapping("/categories")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Danh sách danh mục sản phẩm")
    public ResponseEntity<List<CategoryResponse>> listCategories(
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(productService.listCategories(includeInactive));
    }

    @PostMapping("/categories")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Tạo danh mục sản phẩm")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest req) {
        return new ResponseEntity<>(productService.createCategory(req), HttpStatus.CREATED);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Cập nhật danh mục sản phẩm")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                           @Valid @RequestBody CategoryRequest req) {
        return ResponseEntity.ok(productService.updateCategory(id, req));
    }

    @PatchMapping("/categories/{id}/toggle")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Bật/tắt danh mục sản phẩm")
    public ResponseEntity<CategoryResponse> toggleCategory(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleCategory(id));
    }

    // ------------------------------------------------------------------- product

    @GetMapping("/products")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Tìm sản phẩm theo tên, SKU hoặc mã vạch")
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.searchProducts(search, categoryId, activeOnly, page, size));
    }

    @GetMapping("/products/barcode/{barcode}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Tra cứu sản phẩm theo mã vạch (dùng cho máy quét)")
    public ResponseEntity<ProductResponse> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.getByBarcode(barcode));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Chi tiết sản phẩm")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping("/products")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Tạo sản phẩm (tồn kho khởi tạo 0, nhập kho qua /inventory/stock-in)")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest req) {
        return new ResponseEntity<>(productService.createProduct(req), HttpStatus.CREATED);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Cập nhật sản phẩm")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                         @Valid @RequestBody ProductRequest req) {
        return ResponseEntity.ok(productService.updateProduct(id, req));
    }

    @PatchMapping("/products/{id}/toggle")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Ngừng bán / mở bán lại sản phẩm")
    public ResponseEntity<ProductResponse> toggleProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleProduct(id));
    }
}
