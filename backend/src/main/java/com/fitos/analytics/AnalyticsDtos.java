package com.fitos.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record Period(String label, BigDecimal income, BigDecimal expense, BigDecimal balance) {}
    public record InvoiceRange(LocalDate from, LocalDate to, BigDecimal posSales,
                               BigDecimal packageSales, BigDecimal salesTotal,
                               BigDecimal purchaseTotal, BigDecimal invoiceTotal) {}
    public record AvailableYears(List<Integer> years) {}
    public record TransactionRow(Long id, Long detailId, String type, String reference, String partyName,
                                 String employeeName, Instant at, BigDecimal amount) {}
    public record TransactionPage(List<TransactionRow> content, long totalElements,
                                  int page, int size) {}
    public record MatrixRow(Long id, String name, List<BigDecimal> values, BigDecimal total) {}
    public record Matrix(List<String> columns, List<MatrixRow> rows,
                         List<BigDecimal> totals, BigDecimal grandTotal) {}
    public record Overview(LocalDate from, LocalDate to, int year,
                           BigDecimal rangeIncome, BigDecimal rangeExpense, BigDecimal rangeBalance,
                           List<Period> months, List<Period> quarters, List<Period> years,
                           Matrix employees, Matrix customers, Matrix products) {}
}
