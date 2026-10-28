package com.fitos.report;

import com.fitos.common.security.Roles;
import com.fitos.report.dto.ReportDtos.ClassesReportResponse;
import com.fitos.report.dto.ReportDtos.MembershipReportResponse;
import com.fitos.report.dto.ReportDtos.SalesReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Task #68: GET /api/reports/sales, memberships, classes (MANAGEMENT).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales")
    @PreAuthorize(Roles.MANAGEMENT)
    public SalesReportResponse getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getSalesReport(from, to);
    }

    @GetMapping("/memberships")
    @PreAuthorize(Roles.MANAGEMENT)
    public MembershipReportResponse getMembershipsReport() {
        return reportService.getMembershipsReport();
    }

    @GetMapping("/classes")
    @PreAuthorize(Roles.MANAGEMENT)
    public ClassesReportResponse getClassesReport() {
        return reportService.getClassesReport();
    }
}
