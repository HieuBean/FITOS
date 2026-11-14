package com.fitos.controller;

import com.fitos.common.security.Roles;
import com.fitos.dto.common.PageResponse;
import com.fitos.dto.retail.RetailDtos.*;
import com.fitos.exception.BadRequestException;
import com.fitos.model.Customer;
import com.fitos.repository.CustomerRepository;
import com.fitos.security.StudioContext;
import com.fitos.service.RetailOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Bán lẻ tại quầy (POS): tạo đơn, thu tiền, huỷ, trả hàng, in hoá đơn. */
@RestController
@RequestMapping("/api/retail/orders")
@RequiredArgsConstructor
@Tag(name = "Retail POS", description = "Bán hàng tại quầy")
public class RetailOrderController {

    private final RetailOrderService retailOrderService;
    private final CustomerRepository customerRepository;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Tạo đơn bán lẻ (giỏ hàng hỗn hợp: sản phẩm, gói, suất lớp)")
    public ResponseEntity<RetailOrderResponse> create(@Valid @RequestBody RetailOrderCreateRequest req) {
        RetailOrderResponse response = retailOrderService.createOrder(req);
        return new ResponseEntity<>(response,
                response.isIdempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Thu tiền cho đơn đang chờ thanh toán")
    public ResponseEntity<RetailOrderResponse> pay(@PathVariable Long id,
                                                   @Valid @RequestBody PayOrderRequest req) {
        return ResponseEntity.ok(retailOrderService.pay(id, req));
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Danh sách đơn bán lẻ")
    public ResponseEntity<PageResponse<RetailOrderResponse>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(retailOrderService.list(null, status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Chi tiết đơn bán lẻ")
    public ResponseEntity<RetailOrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(retailOrderService.get(id));
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Dữ liệu in hoá đơn")
    public ResponseEntity<ReceiptResponse> receipt(@PathVariable Long id) {
        return ResponseEntity.ok(retailOrderService.receipt(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Huỷ đơn chờ thanh toán (hoàn kho, trả suất lớp)")
    public ResponseEntity<RetailOrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(retailOrderService.cancel(id));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Trả hàng (tạo đơn hoàn, nhập lại kho)")
    public ResponseEntity<RetailOrderResponse> createReturn(@PathVariable Long id,
                                                            @Valid @RequestBody RetailReturnRequest req) {
        return new ResponseEntity<>(retailOrderService.createReturn(id, req), HttpStatus.CREATED);
    }

    /** Tạo nhanh hồ sơ khách tại quầy (khách mua nước/chưa là hội viên). */
    @PostMapping("/quick-customer")
    @PreAuthorize(Roles.STAFF)
    @Operation(summary = "Tạo nhanh khách hàng tại quầy bằng tên và số điện thoại")
    public ResponseEntity<QuickCustomerResponse> quickCustomer(@Valid @RequestBody QuickCustomerRequest req) {
        Long studioId = StudioContext.currentStudioId();
        String phone = req.getPhone().trim();

        var existing = customerRepository.findByPhoneAndStudioId(phone, studioId);
        if (existing.isPresent()) {
            Customer c = existing.get();
            return ResponseEntity.ok(QuickCustomerResponse.builder()
                    .id(c.getId()).fullName(c.getFullName()).phone(c.getPhone()).created(false).build());
        }

        Customer customer = customerRepository.save(Customer.builder()
                .studioId(studioId)
                .fullName(req.getFullName().trim())
                .phone(phone)
                .status("ACTIVE")
                .build());
        return new ResponseEntity<>(QuickCustomerResponse.builder()
                .id(customer.getId()).fullName(customer.getFullName())
                .phone(customer.getPhone()).created(true).build(), HttpStatus.CREATED);
    }
}
