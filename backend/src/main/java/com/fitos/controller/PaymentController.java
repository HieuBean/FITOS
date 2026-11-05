package com.fitos.controller;

import com.fitos.dto.commerce.PaymentDtos.PaymentCallbackRequest;
import com.fitos.dto.commerce.PaymentDtos.PaymentRequest;
import com.fitos.dto.commerce.PaymentDtos.PaymentResponse;
import com.fitos.security.UserPrincipal;
import com.fitos.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Task #38 và #39: thanh toán + webhook idempotent. */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Thanh toán và callback từ cổng thanh toán")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Tạo giao dịch cho một đơn hàng (CASH thanh toán ngay)")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest req) {
        return new ResponseEntity<>(paymentService.create(req), HttpStatus.CREATED);
    }

    @PostMapping("/callback")
    @Operation(summary = "Webhook từ cổng thanh toán — idempotent theo transactionRef + status")
    public ResponseEntity<PaymentResponse> callback(@Valid @RequestBody PaymentCallbackRequest req) {
        return ResponseEntity.ok(paymentService.callback(req));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Các giao dịch của một đơn hàng")
    public ResponseEntity<List<PaymentResponse>> byOrder(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(paymentService.byOrder(orderId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Lịch sử thanh toán của hội viên đang đăng nhập")
    public ResponseEntity<List<PaymentResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.mine(principal.getId()));
    }
}
