package com.fitos.analytics;

import com.fitos.analytics.AnalyticsDtos.*;
import com.fitos.exception.BadRequestException;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final JdbcTemplate jdbc;

    private record Sale(Long id, Instant at, BigDecimal amount, Long customerId,
                        String customerName, Long employeeId, String employeeName) {}
    private record Purchase(Instant at, BigDecimal amount) {}
    private record ProductLine(Long orderId, String orderType, Instant at,
                               BigDecimal orderTotal, BigDecimal subtotal, Long productId,
                               String productName, BigDecimal lineTotal) {}
    private record AmountRow(Long id, String name, BigDecimal[] values) {}

    private static final String TRANSACTION_EVENTS = """
            WITH events AS (
                SELECT o.id, o.id detail_id, CASE WHEN o.order_type='RETAIL' THEN 'POS' ELSE 'PACKAGE' END type,
                       o.order_reference reference, COALESCE(c.full_name,'Khách vãng lai') party_name,
                       COALESCE(u.full_name,'Chưa xác định') employee_name,
                       COALESCE(o.paid_at,o.created_at) event_at, o.total_amount amount,
                       o.created_by employee_id, o.customer_id customer_id
                FROM orders o
                LEFT JOIN customers c ON c.id=o.customer_id AND c.studio_id=o.studio_id
                LEFT JOIN users u ON u.id=o.created_by AND u.studio_id=o.studio_id
                WHERE o.studio_id=? AND o.status='PAID'
                  AND o.order_type IN ('RETAIL','PLAN_SALE')
                  AND COALESCE(o.paid_at,o.created_at)>=?
                  AND COALESCE(o.paid_at,o.created_at)<?
                UNION ALL
                SELECT r.id, r.parent_order_id, 'POS_RETURN', r.order_reference,
                       COALESCE(c.full_name,'Khách vãng lai'),
                       COALESCE(u.full_name,'Chưa xác định'), r.created_at, -r.total_amount,
                       original.created_by, original.customer_id
                FROM orders r JOIN orders original
                  ON original.id=r.parent_order_id AND original.studio_id=r.studio_id
                LEFT JOIN customers c ON c.id=original.customer_id AND c.studio_id=original.studio_id
                LEFT JOIN users u ON u.id=original.created_by AND u.studio_id=original.studio_id
                WHERE r.studio_id=? AND r.status='REFUNDED' AND r.order_type='RETAIL_RETURN'
                  AND r.created_at>=? AND r.created_at<?
                UNION ALL
                SELECT p.id, p.id, 'PURCHASE', p.receipt_no, p.supplier_name,
                       COALESCE(u.full_name,'Chưa xác định'), p.confirmed_at, p.total_amount,
                       NULL::bigint, NULL::bigint
                FROM purchase_receipts p
                LEFT JOIN users u ON u.id=p.created_by AND u.studio_id=p.studio_id
                WHERE p.studio_id=? AND p.status='CONFIRMED'
                  AND p.confirmed_at>=? AND p.confirmed_at<?
            )
            """;

    @Transactional(readOnly = true)
    public AvailableYears availableYears() {
        Long studioId = StudioContext.currentStudioId();
        List<Integer> found = jdbc.query("""
                SELECT DISTINCT EXTRACT(YEAR FROM event_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::int AS year
                FROM (
                    SELECT COALESCE(paid_at,created_at) event_at FROM orders
                    WHERE studio_id=? AND status='PAID' AND order_type IN ('RETAIL','PLAN_SALE')
                    UNION ALL
                    SELECT confirmed_at FROM purchase_receipts
                    WHERE studio_id=? AND status='CONFIRMED'
                ) events ORDER BY year
                """, (rs, row) -> rs.getInt(1), studioId, studioId);
        int current = LocalDate.now(ZONE).getYear();
        int first = found.isEmpty() ? current : Math.min(current, found.get(0));
        int last = found.isEmpty() ? current : Math.max(current, found.get(found.size() - 1));
        return new AvailableYears(java.util.stream.IntStream.rangeClosed(first, last).boxed().toList());
    }

    @Transactional(readOnly = true)
    public TransactionPage transactions(LocalDate from, LocalDate to, int page, int size,
                                        String kind, Long employeeId, Long customerId, Long productId) {
        if (from == null || to == null || from.isAfter(to) || page < 0 || size < 1 || size > 100)
            throw new BadRequestException("Khoảng ngày hoặc phân trang không hợp lệ");
        if (kind != null && !kind.equals("SALE") && !kind.equals("PURCHASE"))
            throw new BadRequestException("Loại chứng từ không hợp lệ");
        Long studioId = StudioContext.currentStudioId();
        Timestamp start = Timestamp.from(from.atStartOfDay(ZONE).toInstant());
        Timestamp end = Timestamp.from(to.plusDays(1).atStartOfDay(ZONE).toInstant());
        Object[] params = {studioId, start, end, studioId, start, end, studioId, start, end};
        String filtered = TRANSACTION_EVENTS + """
                SELECT * FROM events
                WHERE (CAST(? AS varchar) IS NULL OR (?='SALE' AND type<>'PURCHASE') OR (?='PURCHASE' AND type='PURCHASE'))
                  AND (CAST(? AS bigint) IS NULL OR COALESCE(employee_id,-1)=?)
                  AND (CAST(? AS bigint) IS NULL OR COALESCE(customer_id,-1)=?)
                  AND (CAST(? AS bigint) IS NULL OR (type<>'PURCHASE' AND EXISTS (
                      SELECT 1 FROM order_items oi
                      WHERE oi.studio_id=? AND oi.order_id=events.id AND oi.product_id=?)))
                """;
        List<Object> filters = new ArrayList<>(List.of(params));
        filters.add(kind); filters.add(kind); filters.add(kind);
        filters.add(employeeId); filters.add(employeeId);
        filters.add(customerId); filters.add(customerId);
        filters.add(productId); filters.add(studioId); filters.add(productId);
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM (" + filtered + ") filtered",
                Long.class, filters.toArray());
        List<Object> pageParams = new ArrayList<>(filters);
        pageParams.add(size);
        pageParams.add((long) page * size);
        List<TransactionRow> content = jdbc.query("SELECT id, detail_id, type, reference, party_name, employee_name, event_at, amount FROM (" + filtered + ") filtered " + """
                ORDER BY event_at DESC, type, id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> new TransactionRow(rs.getLong(1), (Long) rs.getObject(2),
                rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                rs.getTimestamp(7).toInstant(), rs.getBigDecimal(8)), pageParams.toArray());
        return new TransactionPage(content, count == null ? 0 : count, page, size);
    }

    @Transactional(readOnly = true)
    public InvoiceRange invoiceRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to))
            throw new BadRequestException("Khoảng ngày không hợp lệ");
        Long studioId = StudioContext.currentStudioId();
        Timestamp start = Timestamp.from(from.atStartOfDay(ZONE).toInstant());
        Timestamp endExclusive = Timestamp.from(to.plusDays(1).atStartOfDay(ZONE).toInstant());

        BigDecimal pos = ZERO;
        BigDecimal packages = ZERO;
        List<Map.Entry<String, BigDecimal>> saleAmounts = jdbc.query("""
                SELECT sale_type, SUM(amount) AS total
                FROM (
                    SELECT o.order_type AS sale_type, o.total_amount AS amount
                    FROM orders o
                    WHERE o.studio_id=? AND o.status='PAID'
                      AND o.order_type IN ('RETAIL', 'PLAN_SALE')
                      AND COALESCE(o.paid_at, o.created_at)>=?
                      AND COALESCE(o.paid_at, o.created_at)<?
                    UNION ALL
                    SELECT original.order_type AS sale_type, -r.total_amount AS amount
                    FROM orders r JOIN orders original
                      ON original.id=r.parent_order_id AND original.studio_id=r.studio_id
                    WHERE r.studio_id=? AND r.order_type='RETAIL_RETURN'
                      AND r.status='REFUNDED' AND original.order_type='RETAIL'
                      AND r.created_at>=? AND r.created_at<?
                ) events
                GROUP BY sale_type
                """, (rs, row) -> Map.entry(rs.getString("sale_type"), rs.getBigDecimal("total")),
                studioId, start, endExclusive, studioId, start, endExclusive);
        for (Map.Entry<String, BigDecimal> amount : saleAmounts) {
            if ("RETAIL".equals(amount.getKey())) pos = amount.getValue();
            if ("PLAN_SALE".equals(amount.getKey())) packages = amount.getValue();
        }
        BigDecimal purchases = jdbc.queryForObject("""
                SELECT COALESCE(SUM(total_amount), 0) FROM purchase_receipts
                WHERE studio_id=? AND status='CONFIRMED'
                  AND confirmed_at>=? AND confirmed_at<?
                """, BigDecimal.class, studioId, start, endExclusive);
        if (purchases == null) purchases = ZERO;
        BigDecimal salesTotal = pos.add(packages);
        return new InvoiceRange(from, to, pos, packages, salesTotal,
                purchases, salesTotal.add(purchases));
    }

    @Transactional(readOnly = true)
    public Overview overview(LocalDate from, LocalDate to, Integer selectedYear,
                             Integer firstYear, Integer lastYear) {
        LocalDate today = LocalDate.now(ZONE);
        int year = selectedYear == null ? today.getYear() : selectedYear;
        int startYear = firstYear == null ? year - 2 : firstYear;
        int endYear = lastYear == null ? year : lastYear;
        LocalDate start = from == null ? LocalDate.of(year, 1, 1) : from;
        LocalDate end = to == null ? today : to;
        if (year < 2000 || year > 2100 || startYear < 2000 || endYear > 2100
                || startYear > endYear || start.isAfter(end)) {
            throw new BadRequestException("Khoảng ngày hoặc năm không hợp lệ");
        }
        LocalDate queryStart = min(start, LocalDate.of(startYear, 1, 1), LocalDate.of(year, 1, 1));
        LocalDate queryEnd = max(end.plusDays(1), LocalDate.of(endYear + 1, 1, 1), LocalDate.of(year + 1, 1, 1));
        Timestamp fromTs = Timestamp.from(queryStart.atStartOfDay(ZONE).toInstant());
        Timestamp toTs = Timestamp.from(queryEnd.atStartOfDay(ZONE).toInstant());
        Long studioId = StudioContext.currentStudioId();

        List<Sale> sales = jdbc.query("""
                SELECT event.id, event.event_at, event.amount, event.customer_id,
                       c.full_name, event.employee_id, u.full_name
                FROM (
                    SELECT o.id, o.studio_id, COALESCE(o.paid_at, o.created_at) event_at,
                           o.total_amount amount, o.customer_id, o.created_by employee_id
                    FROM orders o WHERE o.status='PAID' AND o.order_type <> 'RETAIL_RETURN'
                    UNION ALL
                    SELECT r.id, r.studio_id, r.created_at, -r.total_amount,
                           original.customer_id, original.created_by
                    FROM orders r JOIN orders original
                      ON original.id=r.parent_order_id AND original.studio_id=r.studio_id
                    WHERE r.order_type='RETAIL_RETURN' AND r.status='REFUNDED'
                ) event
                LEFT JOIN customers c ON c.id=event.customer_id AND c.studio_id=event.studio_id
                LEFT JOIN users u ON u.id=event.employee_id AND u.studio_id=event.studio_id
                WHERE event.studio_id=? AND event.event_at>=? AND event.event_at<?
                """, (rs, row) -> new Sale(rs.getLong(1), rs.getTimestamp(2).toInstant(),
                rs.getBigDecimal(3), (Long) rs.getObject(4), rs.getString(5),
                (Long) rs.getObject(6), rs.getString(7)), studioId, fromTs, toTs);
        List<Purchase> purchases = jdbc.query("""
                SELECT confirmed_at, total_amount FROM purchase_receipts
                WHERE studio_id=? AND status='CONFIRMED' AND confirmed_at>=? AND confirmed_at<?
                """, (rs, row) -> new Purchase(rs.getTimestamp(1).toInstant(), rs.getBigDecimal(2)),
                studioId, fromTs, toTs);

        BigDecimal rangeIncome = sumSales(sales, start, end);
        BigDecimal rangeExpense = sumPurchases(purchases, start, end);
        List<Period> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            final int m = month;
            months.add(period(String.format("%02d/%d", month, year), sales, purchases,
                    date -> date.getYear() == year && date.getMonthValue() == m));
        }
        List<Period> quarters = new ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            final int q = quarter;
            quarters.add(period("Q" + quarter, sales, purchases,
                    date -> date.getYear() == year && quarter(date) == q));
        }
        List<Period> years = new ArrayList<>();
        for (int y = startYear; y <= endYear; y++) {
            final int current = y;
            years.add(period(String.valueOf(y), sales, purchases,
                    date -> date.getYear() == current));
        }
        Matrix employees = yearMatrix(sales, startYear, endYear, true);
        Matrix customers = yearMatrix(sales, startYear, endYear, false);
        Matrix products = productMatrix(studioId, year, fromTs, toTs);
        return new Overview(start, end, year, rangeIncome, rangeExpense,
                rangeIncome.subtract(rangeExpense), months, quarters, years,
                employees, customers, products);
    }

    private Period period(String label, List<Sale> sales, List<Purchase> purchases,
                          java.util.function.Predicate<LocalDate> matches) {
        BigDecimal income = sales.stream().filter(s -> matches.test(date(s.at())))
                .map(Sale::amount).reduce(ZERO, BigDecimal::add);
        BigDecimal expense = purchases.stream().filter(p -> matches.test(date(p.at())))
                .map(Purchase::amount).reduce(ZERO, BigDecimal::add);
        return new Period(label, income, expense, income.subtract(expense));
    }

    private BigDecimal sumSales(List<Sale> rows, LocalDate from, LocalDate to) {
        return rows.stream().filter(row -> inRange(date(row.at()), from, to))
                .map(Sale::amount).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumPurchases(List<Purchase> rows, LocalDate from, LocalDate to) {
        return rows.stream().filter(row -> inRange(date(row.at()), from, to))
                .map(Purchase::amount).reduce(ZERO, BigDecimal::add);
    }

    private Matrix yearMatrix(List<Sale> sales, int firstYear, int lastYear, boolean employee) {
        int length = lastYear - firstYear + 1;
        Map<Long, AmountRow> rows = new LinkedHashMap<>();
        for (Sale sale : sales) {
            int y = date(sale.at()).getYear();
            if (y < firstYear || y > lastYear) continue;
            Long id = employee ? sale.employeeId() : sale.customerId();
            String name = employee ? sale.employeeName() : sale.customerName();
            long key = id == null ? -1L : id;
            AmountRow row = rows.computeIfAbsent(key, ignored -> new AmountRow(id,
                    name == null ? "Chưa xác định" : name, zeroArray(length)));
            row.values()[y - firstYear] = row.values()[y - firstYear].add(sale.amount());
        }
        return matrix(java.util.stream.IntStream.rangeClosed(firstYear, lastYear)
                .mapToObj(String::valueOf).toList(), rows.values());
    }

    private Matrix productMatrix(Long studioId, int year, Timestamp from, Timestamp to) {
        List<ProductLine> lines = jdbc.query("""
                SELECT o.id, o.order_type,
                       CASE WHEN o.order_type='RETAIL_RETURN' THEN o.created_at
                            ELSE COALESCE(o.paid_at,o.created_at) END event_at,
                       o.total_amount, o.subtotal_amount, oi.product_id,
                       COALESCE(oi.item_name,p.name,'Sản phẩm chưa xác định'), oi.total_price
                FROM orders o JOIN order_items oi ON oi.order_id=o.id AND oi.studio_id=o.studio_id
                LEFT JOIN products p ON p.id=oi.product_id AND p.studio_id=oi.studio_id
                WHERE o.studio_id=? AND ((o.status='PAID' AND o.order_type<>'RETAIL_RETURN')
                    OR (o.status='REFUNDED' AND o.order_type='RETAIL_RETURN'))
                  AND (CASE WHEN o.order_type='RETAIL_RETURN' THEN o.created_at
                            ELSE COALESCE(o.paid_at,o.created_at) END)>=?
                  AND (CASE WHEN o.order_type='RETAIL_RETURN' THEN o.created_at
                            ELSE COALESCE(o.paid_at,o.created_at) END)<?
                ORDER BY o.id, oi.id
                """, (rs, row) -> new ProductLine(rs.getLong(1), rs.getString(2),
                rs.getTimestamp(3).toInstant(), rs.getBigDecimal(4), rs.getBigDecimal(5),
                (Long) rs.getObject(6), rs.getString(7), rs.getBigDecimal(8)),
                studioId, from, to);
        Map<Long, List<ProductLine>> byOrder = new LinkedHashMap<>();
        for (ProductLine line : lines) byOrder.computeIfAbsent(line.orderId(), ignored -> new ArrayList<>()).add(line);
        Map<Long, AmountRow> rows = new LinkedHashMap<>();
        for (List<ProductLine> orderLines : byOrder.values()) {
            ProductLine first = orderLines.get(0);
            if (date(first.at()).getYear() != year) continue;
            int column = quarter(date(first.at())) - 1;
            BigDecimal remaining = first.orderTotal();
            for (int index = 0; index < orderLines.size(); index++) {
                ProductLine line = orderLines.get(index);
                BigDecimal allocated;
                if ("RETAIL_RETURN".equals(line.orderType())) {
                    allocated = line.lineTotal().negate();
                } else if (index == orderLines.size() - 1) {
                    allocated = remaining;
                } else if (first.subtotal().signum() == 0) {
                    allocated = ZERO;
                } else {
                    allocated = line.lineTotal().multiply(first.orderTotal())
                            .divide(first.subtotal(), 2, RoundingMode.HALF_UP);
                    remaining = remaining.subtract(allocated);
                }
                if (line.productId() == null) continue;
                AmountRow row = rows.computeIfAbsent(line.productId(), ignored ->
                        new AmountRow(line.productId(), line.productName(), zeroArray(4)));
                row.values()[column] = row.values()[column].add(allocated);
            }
        }
        return matrix(List.of("Q1", "Q2", "Q3", "Q4"), rows.values());
    }

    private Matrix matrix(List<String> columns, java.util.Collection<AmountRow> source) {
        BigDecimal[] totals = zeroArray(columns.size());
        List<MatrixRow> rows = source.stream().map(row -> {
            for (int i = 0; i < totals.length; i++) totals[i] = totals[i].add(row.values()[i]);
            List<BigDecimal> values = List.of(row.values());
            return new MatrixRow(row.id(), row.name(), values,
                    values.stream().reduce(ZERO, BigDecimal::add));
        }).sorted(Comparator.comparing(MatrixRow::total).reversed()).toList();
        return new Matrix(columns, rows, List.of(totals),
                List.of(totals).stream().reduce(ZERO, BigDecimal::add));
    }

    private BigDecimal[] zeroArray(int length) {
        BigDecimal[] values = new BigDecimal[length];
        java.util.Arrays.fill(values, ZERO);
        return values;
    }

    private LocalDate date(Instant instant) { return instant.atZone(ZONE).toLocalDate(); }
    private int quarter(LocalDate date) { return (date.getMonthValue() - 1) / 3 + 1; }
    private boolean inRange(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
    private LocalDate min(LocalDate... dates) { return java.util.Arrays.stream(dates).min(LocalDate::compareTo).orElseThrow(); }
    private LocalDate max(LocalDate... dates) { return java.util.Arrays.stream(dates).max(LocalDate::compareTo).orElseThrow(); }
}
