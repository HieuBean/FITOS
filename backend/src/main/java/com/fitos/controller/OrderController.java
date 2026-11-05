package com.fitos.controller;

import com.fitos.dto.commerce.OrderDtos.OrderCreateRequest;
import com.fitos.dto.commerce.OrderDtos.OrderResponse;
import com.fitos.dto.common.PageResponse;
import com.fitos.security.UserPrincipal;
import com.fitos.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Task #35, #36, #37: đơn hàng — giá luôn do server tính. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Đơn mua gói dịch vụ")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Tạo đơn hàng (tính tiền ở server)")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest req) {
        return new ResponseEntity<>(orderService.create(req), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Danh sách đơn hàng có phân trang, lọc theo trạng thái")
    public ResponseEntity<PageResponse<OrderResponse>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(orderService.list(status,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Đơn hàng của hội viên đang đăng nhập")
    public ResponseEntity<List<OrderResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.mine(principal.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'MEMBER')")
    @Operation(summary = "Chi tiết đơn hàng")
    public ResponseEntity<OrderResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.get(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Huỷ đơn đang PENDING")
    public ResponseEntity<OrderResponse> cancel(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}
