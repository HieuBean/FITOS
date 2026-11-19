package com.fitos.invoice;

import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.invoice.InvoiceDtos.*;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final JdbcTemplate jdbc;

    private static final String BASE = """
            FROM orders o
            JOIN customers c ON c.id=o.customer_id AND c.studio_id=o.studio_id
            LEFT JOIN users u ON u.id=o.created_by AND u.studio_id=o.studio_id
            WHERE o.studio_id=? AND o.status='PAID' AND o.order_type <> 'RETAIL_RETURN'
            """;
    private static final String FIELDS = """
            SELECT o.id, o.order_reference, o.order_type, c.full_name,
                   COALESCE(u.full_name,'Chưa xác định') employee_name, o.total_amount,
                   COALESCE((SELECT SUM(r.total_amount) FROM orders r
                             WHERE r.studio_id=o.studio_id AND r.parent_order_id=o.id
                               AND r.order_type='RETAIL_RETURN' AND r.status='REFUNDED'),0) returned_amount,
                   COALESCE(o.paid_at,o.created_at) event_at
            """;

    @Transactional(readOnly = true)
    public Page search(SearchRequest request) {
        int page = request == null || request.page() == null ? 0 : Math.max(0, request.page());
        int size = request == null || request.size() == null ? 20 : Math.min(100, Math.max(1, request.size()));
        List<Object> params = new ArrayList<>();
        params.add(StudioContext.currentStudioId());
        StringBuilder condition = new StringBuilder();
        if (request != null && request.keyword() != null && !request.keyword().isBlank()) {
            String keyword = request.keyword().trim();
            if (keyword.length() > 100) throw new BadRequestException("Từ khóa tìm kiếm quá dài");
            condition.append(" AND (POSITION(LOWER(?) IN LOWER(o.order_reference)) > 0")
                    .append(" OR POSITION(LOWER(?) IN LOWER(c.full_name)) > 0)");
            params.add(keyword);
            params.add(keyword);
        }
        long total = jdbc.queryForObject("SELECT count(*) " + BASE + condition,
                Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add(page * size);
        List<Summary> rows = jdbc.query(FIELDS + BASE + condition
                        + " ORDER BY COALESCE(o.paid_at,o.created_at) DESC, o.id DESC LIMIT ? OFFSET ?",
                (rs, index) -> summary(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getBigDecimal(6),
                        rs.getBigDecimal(7), rs.getTimestamp(8)), pageParams.toArray());
        return new Page(rows, total, page, size);
    }

    @Transactional(readOnly = true)
    public Detail get(Long id) {
        Long studioId = StudioContext.currentStudioId();
        List<Summary> rows = jdbc.query(FIELDS + BASE + " AND o.id=?",
                (rs, index) -> summary(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getBigDecimal(6),
                        rs.getBigDecimal(7), rs.getTimestamp(8)), studioId, id);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy hóa đơn: " + id);
        List<Item> items = jdbc.query("""
                SELECT oi.id, oi.item_type, oi.product_id, COALESCE(oi.item_name,p.name,'Mặt hàng'),
                       oi.quantity, oi.unit_price, oi.total_price
                FROM order_items oi LEFT JOIN products p
                  ON p.id=oi.product_id AND p.studio_id=oi.studio_id
                WHERE oi.order_id=? AND oi.studio_id=? ORDER BY oi.id
                """, (rs, index) -> new Item(rs.getLong(1), rs.getString(2),
                (Long) rs.getObject(3), rs.getString(4), rs.getInt(5),
                rs.getBigDecimal(6), rs.getBigDecimal(7)), id, studioId);
        List<Summary> returns = jdbc.query("""
                SELECT r.id, r.order_reference, r.order_type, c.full_name,
                       COALESCE(u.full_name,'Chưa xác định'), r.total_amount,
                       0::numeric, r.created_at
                FROM orders r JOIN customers c ON c.id=r.customer_id AND c.studio_id=r.studio_id
                LEFT JOIN users u ON u.id=r.created_by AND u.studio_id=r.studio_id
                WHERE r.studio_id=? AND r.parent_order_id=? AND r.order_type='RETAIL_RETURN'
                  AND r.status='REFUNDED' ORDER BY r.created_at
                """, (rs, index) -> summary(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getBigDecimal(6),
                rs.getBigDecimal(7), rs.getTimestamp(8)), studioId, id);
        return new Detail(rows.get(0), items, returns);
    }

    private Summary summary(Long id, String reference, String type, String customer,
                            String employee, BigDecimal total, BigDecimal returned, Timestamp at) {
        return new Summary(id, reference, type, customer, employee, total, returned,
                total.subtract(returned), at.toInstant());
    }

}
