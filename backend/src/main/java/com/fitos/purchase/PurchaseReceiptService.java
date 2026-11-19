package com.fitos.purchase;

import com.fitos.dto.retail.RetailDtos.StockInRequest;
import com.fitos.dto.retail.RetailDtos.InventoryTransactionResponse;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.purchase.PurchaseReceiptDtos.*;
import com.fitos.security.StudioContext;
import com.fitos.security.UserPrincipal;
import com.fitos.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseReceiptService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final JdbcTemplate jdbc;
    private final InventoryService inventory;

    @Transactional
    public Receipt create(SaveRequest request) {
        Long studioId = StudioContext.currentStudioId();
        BigDecimal total = validateAndTotal(studioId, request);
        String reference = "PNH-" + LocalDate.now(ZONE).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long id = jdbc.queryForObject("""
                INSERT INTO purchase_receipts
                (studio_id, receipt_no, supplier_name, note, status, total_amount, created_by)
                VALUES (?, ?, ?, ?, 'DRAFT', ?, ?) RETURNING id
                """, Long.class, studioId, reference, request.supplierName().trim(), request.note(),
                total, currentUserId());
        insertLines(studioId, id, request.items());
        return get(id);
    }

    @Transactional
    public Receipt update(Long id, SaveRequest request) {
        Long studioId = StudioContext.currentStudioId();
        String status = lockStatus(id, studioId);
        if (!"DRAFT".equals(status)) throw new BadRequestException("Chỉ sửa được phiếu nháp");
        BigDecimal total = validateAndTotal(studioId, request);
        jdbc.update("UPDATE purchase_receipts SET supplier_name=?, note=?, total_amount=?, updated_at=now() WHERE id=? AND studio_id=?",
                request.supplierName().trim(), request.note(), total, id, studioId);
        jdbc.update("DELETE FROM purchase_receipt_items WHERE receipt_id=? AND studio_id=?", id, studioId);
        insertLines(studioId, id, request.items());
        return get(id);
    }

    @Transactional
    public Receipt confirm(Long id) {
        Long studioId = StudioContext.currentStudioId();
        String status = lockStatus(id, studioId);
        if ("CONFIRMED".equals(status)) return get(id);
        List<Line> lines = lines(id, studioId);
        if (lines.isEmpty()) throw new BadRequestException("Phiếu nhập phải có sản phẩm");
        // A stable lock order prevents two receipts with overlapping products from deadlocking.
        for (Line line : lines.stream().sorted(java.util.Comparator.comparing(Line::productId)).toList()) {
            InventoryTransactionResponse movement = inventory.stockIn(StockInRequest.builder()
                    .productId(line.productId()).quantity(line.quantity()).unitCost(line.unitCost())
                    .note("Phiếu nhập " + id).build());
            jdbc.update("UPDATE inventory_transactions SET purchase_receipt_id=? WHERE id=? AND studio_id=?",
                    id, movement.getId(), studioId);
        }
        jdbc.update("UPDATE purchase_receipts SET status='CONFIRMED', confirmed_at=now(), updated_at=now() WHERE id=? AND studio_id=?",
                id, studioId);
        return get(id);
    }

    /** Keeps the existing one-product stock-in API and old callers consistent with purchase reports. */
    @Transactional
    public InventoryTransactionResponse legacyStockIn(StockInRequest request) {
        InventoryTransactionResponse movement = inventory.stockIn(request);
        Long studioId = StudioContext.currentStudioId();
        Long id = jdbc.queryForObject("""
                INSERT INTO purchase_receipts
                (studio_id, receipt_no, supplier_name, note, status, total_amount,
                 source_transaction_id, created_by, confirmed_at)
                VALUES (?, ?, 'Không xác định', ?, 'CONFIRMED', ?, ?, ?, now()) RETURNING id
                """, Long.class, studioId, "PNH-LE-" + movement.getId(), request.getNote(),
                request.getUnitCost().multiply(BigDecimal.valueOf(request.getQuantity())),
                movement.getId(), currentUserId());
        jdbc.update("""
                INSERT INTO purchase_receipt_items
                (studio_id, receipt_id, product_id, quantity, unit_cost, line_total)
                VALUES (?, ?, ?, ?, ?, ?)
                """, studioId, id, request.getProductId(), request.getQuantity(), request.getUnitCost(),
                request.getUnitCost().multiply(BigDecimal.valueOf(request.getQuantity())));
        jdbc.update("UPDATE inventory_transactions SET purchase_receipt_id=? WHERE id=? AND studio_id=?",
                id, movement.getId(), studioId);
        return movement;
    }

    @Transactional(readOnly = true)
    public Receipt get(Long id) {
        Long studioId = StudioContext.currentStudioId();
        List<Receipt> receipts = jdbc.query("""
                SELECT id, receipt_no, supplier_name, note, status, total_amount,
                       created_by, confirmed_at, created_at
                FROM purchase_receipts WHERE id=? AND studio_id=?
                """, (rs, row) -> new Receipt(rs.getLong("id"), rs.getString("receipt_no"),
                rs.getString("supplier_name"), rs.getString("note"), rs.getString("status"),
                rs.getBigDecimal("total_amount"), (Long) rs.getObject("created_by"),
                instant(rs.getTimestamp("confirmed_at")), instant(rs.getTimestamp("created_at")),
                lines(rs.getLong("id"), studioId)), id, studioId);
        if (receipts.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy phiếu nhập: " + id);
        return receipts.get(0);
    }

    @Transactional(readOnly = true)
    public Page list(int page, int size) {
        Long studioId = StudioContext.currentStudioId();
        int safePage = Math.max(0, page), safeSize = Math.min(100, Math.max(1, size));
        long count = jdbc.queryForObject("SELECT count(*) FROM purchase_receipts WHERE studio_id=?", Long.class, studioId);
        List<Long> ids = jdbc.query("""
                SELECT id FROM purchase_receipts WHERE studio_id=?
                ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> rs.getLong(1), studioId, safeSize, safePage * safeSize);
        return new Page(ids.stream().map(this::get).toList(), count, safePage, safeSize);
    }

    private String lockStatus(Long id, Long studioId) {
        List<String> statuses = jdbc.query(
                "SELECT status FROM purchase_receipts WHERE id=? AND studio_id=? FOR UPDATE",
                (rs, row) -> rs.getString(1), id, studioId);
        if (statuses.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy phiếu nhập: " + id);
        return statuses.get(0);
    }

    private BigDecimal validateAndTotal(Long studioId, SaveRequest request) {
        HashSet<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (LineInput item : request.items()) {
            if (!seen.add(item.productId())) throw new BadRequestException("Sản phẩm bị lặp trong phiếu nhập");
            Integer exists = jdbc.queryForObject(
                    "SELECT count(*) FROM products WHERE id=? AND studio_id=? AND track_inventory=true",
                    Integer.class, item.productId(), studioId);
            if (exists == null || exists == 0) throw new BadRequestException("Sản phẩm không thuộc studio hoặc không quản lý tồn kho");
            total = total.add(item.unitCost().multiply(BigDecimal.valueOf(item.quantity())));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void insertLines(Long studioId, Long receiptId, List<LineInput> items) {
        for (LineInput item : items) {
            jdbc.update("""
                    INSERT INTO purchase_receipt_items
                    (studio_id, receipt_id, product_id, quantity, unit_cost, line_total)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, studioId, receiptId, item.productId(), item.quantity(), item.unitCost(),
                    item.unitCost().multiply(BigDecimal.valueOf(item.quantity())).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private List<Line> lines(Long id, Long studioId) {
        return jdbc.query("""
                SELECT i.id, i.product_id, p.name, i.quantity, i.unit_cost, i.line_total
                FROM purchase_receipt_items i JOIN products p ON p.id=i.product_id AND p.studio_id=i.studio_id
                WHERE i.receipt_id=? AND i.studio_id=? ORDER BY i.id
                """, (rs, row) -> new Line(rs.getLong(1), rs.getLong(2), rs.getString(3),
                rs.getInt(4), rs.getBigDecimal(5), rs.getBigDecimal(6)), id, studioId);
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal principal ? principal.getId() : null;
    }

    private Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
