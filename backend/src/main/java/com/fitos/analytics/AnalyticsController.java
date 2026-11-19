package com.fitos.analytics;

import com.fitos.analytics.AnalyticsDtos.Overview;
import com.fitos.analytics.AnalyticsDtos.InvoiceRange;
import com.fitos.analytics.AnalyticsDtos.AvailableYears;
import com.fitos.analytics.AnalyticsDtos.TransactionPage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class AnalyticsController {
    private final AnalyticsService service;
    private final AnalyticsExportService exports;

    @GetMapping("/invoice-range")
    public InvoiceRange invoiceRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.invoiceRange(from, to);
    }

    @GetMapping("/available-years")
    public AvailableYears availableYears() { return service.availableYears(); }

    @GetMapping("/transactions")
    public TransactionPage transactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long productId) {
        return service.transactions(from, to, page, size, kind, employeeId, customerId, productId);
    }

    @GetMapping("/overview")
    public Overview overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear) {
        return service.overview(from, to, year, fromYear, toYear);
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fitos-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(exports.xlsx(from, to, year, fromYear, toYear));
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fitos-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(exports.pdf(from, to, year, fromYear, toYear));
    }
}
