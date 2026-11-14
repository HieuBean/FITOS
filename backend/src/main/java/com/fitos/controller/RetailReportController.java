package com.fitos.controller;

import com.fitos.common.security.Roles;
import com.fitos.dto.retail.RetailDtos.DailySalesSummaryResponse;
import com.fitos.service.RetailReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Báo cáo bán lẻ. */
@RestController
@RequestMapping("/api/retail/reports")
@RequiredArgsConstructor
@Tag(name = "Retail Reports", description = "Tổng kết bán hàng theo ngày")
public class RetailReportController {

    private final RetailReportService retailReportService;

    @GetMapping("/daily")
    @PreAuthorize(Roles.MANAGEMENT)
    @Operation(summary = "Tổng kết bán lẻ trong ngày: doanh thu, lãi gộp, theo phương thức và danh mục")
    public ResponseEntity<DailySalesSummaryResponse> daily(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(retailReportService.daily(date));
    }
}
