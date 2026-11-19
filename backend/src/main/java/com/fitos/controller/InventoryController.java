package com.fitos.controller;

import com.fitos.common.security.Roles;
import com.fitos.dto.common.PageResponse;
import com.fitos.dto.retail.RetailDtos.*;
import com.fitos.service.InventoryService;
import com.fitos.purchase.PurchaseReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Nhập kho, điều chỉnh tồn và lịch sử biến động kho. */
@RestController
@RequestMapping("/api/retail/inventory")
@RequiredArgsConstructor
@Tag(name = "Retail Inventory", description = "Quản lý tồn kho hàng hoá bán lẻ")
public class InventoryController {

    private final InventoryService inventoryService;
    private final PurchaseReceiptService purchaseReceiptService;

    @PostMapping("/stock-in")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Nhập kho (cập nhật giá vốn bình quân gia quyền)")
    public ResponseEntity<InventoryTransactionResponse> stockIn(@Valid @RequestBody StockInRequest req) {
        return new ResponseEntity<>(purchaseReceiptService.legacyStockIn(req), HttpStatus.CREATED);
    }

    @PostMapping("/adjust")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Điều chỉnh tồn kho theo số kiểm kê thực tế")
    public ResponseEntity<InventoryTransactionResponse> adjust(@Valid @RequestBody StockAdjustRequest req) {
        return new ResponseEntity<>(inventoryService.adjust(req), HttpStatus.CREATED);
    }

    @GetMapping("/low-stock")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Danh sách mặt hàng sắp hết / đã hết")
    public ResponseEntity<List<LowStockItem>> lowStock() {
        return ResponseEntity.ok(inventoryService.lowStock());
    }

    @GetMapping("/movements")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Lịch sử biến động kho")
    public ResponseEntity<PageResponse<InventoryTransactionResponse>> movements(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.movements(productId, page, size));
    }
}
