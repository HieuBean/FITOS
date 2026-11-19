package com.fitos.workbook;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/retail/products/workbook")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ProductWorkbookController {
    private final ProductWorkbookService service;

    @GetMapping
    public ResponseEntity<byte[]> exportProducts() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fitos-products.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportProducts());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductWorkbookService.ImportResult importProducts(@RequestParam("file") MultipartFile file) {
        return service.importProducts(file);
    }
}
