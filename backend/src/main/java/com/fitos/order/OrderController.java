package com.fitos.order;

import com.fitos.common.dto.PageResponse;
import com.fitos.common.security.Roles;
import com.fitos.order.dto.OrderDtos.CreateOrderRequest;
import com.fitos.order.dto.OrderDtos.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Task #35 – #37: /api/orders */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.STAFF_OR_MEMBER)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.create(req);
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.search(status, customerId, from, to, q, page, size);
    }

    @GetMapping("/me")
    @PreAuthorize(Roles.MEMBER)
    public PageResponse<OrderResponse> myOrders(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return orderService.myOrders(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF_OR_MEMBER)
    public OrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize(Roles.STAFF_OR_MEMBER)
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
