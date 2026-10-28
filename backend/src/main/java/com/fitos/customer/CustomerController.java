package com.fitos.customer;

import com.fitos.common.dto.PageResponse;
import com.fitos.common.security.Roles;
import com.fitos.customer.dto.CustomerDtos.CustomerDetailResponse;
import com.fitos.customer.dto.CustomerDtos.CustomerListItem;
import com.fitos.customer.dto.CustomerDtos.CustomerProfile;
import com.fitos.customer.dto.CustomerDtos.UpdateCustomerRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Task #30 – #32: /api/customers (nhân viên). */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * @param search           tên / email / SĐT
     * @param membershipStatus ACTIVE | PAUSED | EXPIRED | CANCELLED | NONE
     */
    @GetMapping
    @PreAuthorize(Roles.STAFF_OR_TRAINER)
    public PageResponse<CustomerListItem> list(@RequestParam(required = false) String search,
                                               @RequestParam(required = false) String membershipStatus,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return customerService.list(search, membershipStatus, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF_OR_TRAINER)
    public CustomerDetailResponse get(@PathVariable Long id) {
        return customerService.getDetail(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    public CustomerProfile update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest req) {
        return customerService.update(id, req);
    }
}
