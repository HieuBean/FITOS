package com.fitos.purchase;

import com.fitos.purchase.PurchaseReceiptDtos.Page;
import com.fitos.purchase.PurchaseReceiptDtos.Receipt;
import com.fitos.purchase.PurchaseReceiptDtos.SaveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-receipts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class PurchaseReceiptController {
    private final PurchaseReceiptService service;

    @PostMapping
    public ResponseEntity<Receipt> create(@Valid @RequestBody SaveRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Receipt update(@PathVariable Long id, @Valid @RequestBody SaveRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/confirm")
    public Receipt confirm(@PathVariable Long id) { return service.confirm(id); }

    @GetMapping("/{id}")
    public Receipt get(@PathVariable Long id) { return service.get(id); }

    @GetMapping
    public Page list(@RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }
}
