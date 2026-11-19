package com.fitos.invoice;

import com.fitos.invoice.InvoiceDtos.Detail;
import com.fitos.invoice.InvoiceDtos.Page;
import com.fitos.invoice.InvoiceDtos.SearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST')")
public class InvoiceController {
    private final InvoiceService service;

    @PostMapping("/search")
    public Page search(@RequestBody(required = false) SearchRequest request) { return service.search(request); }

    @GetMapping("/{id}")
    public Detail get(@PathVariable Long id) { return service.get(id); }
}
