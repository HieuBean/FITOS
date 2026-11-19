package com.fitos.workbook;

import com.fitos.dto.retail.RetailDtos.ProductRequest;
import com.fitos.exception.BadRequestException;
import com.fitos.model.Product;
import com.fitos.model.ProductCategory;
import com.fitos.repository.ProductCategoryRepository;
import com.fitos.repository.ProductRepository;
import com.fitos.security.StudioContext;
import com.fitos.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductWorkbookService {
    public record ImportResult(int created, int updated, int total) {}
    private record Entry(int row, ProductRequest request) {}
    private static final String[] HEADERS = { "SKU", "Barcode", "Name", "Category", "Unit",
            "CostPrice", "NetSellPrice", "VatRate", "LowStockThreshold", "TrackInventory", "Active" };
    private final ProductRepository products;
    private final ProductCategoryRepository categories;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public byte[] exportProducts() {
        Long studioId = StudioContext.currentStudioId();
        Map<Long, String> categoryNames = new HashMap<>();
        for (ProductCategory category : categories.findByStudioIdOrderBySortOrderAscNameAsc(studioId))
            categoryNames.put(category.getId(), category.getName());
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("San pham");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) header.createCell(i).setCellValue(HEADERS[i]);
            int index = 1;
            for (Product p : products.findByStudioIdOrderByNameAsc(studioId)) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(p.getSku());
                row.createCell(1).setCellValue(p.getBarcode() == null ? "" : p.getBarcode());
                row.createCell(2).setCellValue(p.getName());
                row.createCell(3).setCellValue(categoryNames.getOrDefault(p.getCategoryId(), ""));
                row.createCell(4).setCellValue(p.getUnit());
                row.createCell(5).setCellValue(p.getCostPrice().doubleValue());
                BigDecimal net = p.getSellPrice().divide(BigDecimal.ONE.add(p.getVatRate().movePointLeft(2)),
                        2, RoundingMode.HALF_UP);
                row.createCell(6).setCellValue(net.doubleValue());
                row.createCell(7).setCellValue(p.getVatRate().doubleValue());
                row.createCell(8).setCellValue(p.getLowStockThreshold());
                row.createCell(9).setCellValue(Boolean.TRUE.equals(p.getTrackInventory()));
                row.createCell(10).setCellValue(Boolean.TRUE.equals(p.getIsActive()));
            }
            book.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Không tạo được tệp Excel", ex);
        }
    }

    @Transactional
    public ImportResult importProducts(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5_000_000
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new BadRequestException("Chọn tệp .xlsx không quá 5 MB");
        Long studioId = StudioContext.currentStudioId();
        Map<String, Long> categoryIds = new HashMap<>();
        for (ProductCategory category : categories.findByStudioIdOrderBySortOrderAscNameAsc(studioId))
            categoryIds.put(category.getName().trim().toLowerCase(Locale.ROOT), category.getId());
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (XSSFWorkbook book = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = book.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() > 5000) throw new BadRequestException("Tệp tối đa 5000 sản phẩm");
            Row header = sheet.getRow(0);
            if (header == null) throw new BadRequestException("Thiếu dòng tiêu đề Excel");
            for (int i = 0; i < HEADERS.length; i++) {
                if (!HEADERS[i].equals(cell(header, i))) throw new BadRequestException("Sai cột Excel: " + HEADERS[i]);
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || cell(row, 0).isBlank()) continue;
                String sku = cell(row, 0).trim().toUpperCase(Locale.ROOT);
                if (!sku.matches("[A-Z0-9._-]{2,50}") || !seen.add(sku))
                    throw new BadRequestException("SKU không hợp lệ hoặc trùng ở dòng " + (i + 1));
                String name = cell(row, 2).trim();
                String category = cell(row, 3).trim();
                String unit = cell(row, 4).trim().toUpperCase(Locale.ROOT);
                if (name.isEmpty() || name.length() > 150 || !Set.of("VIEN","CHAI","HOP","CAI","DOI","BO","GOI","KG").contains(unit))
                    throw new BadRequestException("Tên hoặc đơn vị không hợp lệ ở dòng " + (i + 1));
                if (!category.isEmpty() && !categoryIds.containsKey(category.toLowerCase(Locale.ROOT)))
                    throw new BadRequestException("Danh mục không tồn tại ở dòng " + (i + 1));
                BigDecimal cost = money(cell(row, 5), i), net = money(cell(row, 6), i);
                BigDecimal vat = money(cell(row, 7), i);
                if (vat.compareTo(BigDecimal.valueOf(100)) > 0)
                    throw new BadRequestException("VAT vượt 100% ở dòng " + (i + 1));
                int threshold;
                try { threshold = Integer.parseInt(cell(row, 8)); }
                catch (NumberFormatException ex) { throw new BadRequestException("Ngưỡng tồn không hợp lệ ở dòng " + (i + 1)); }
                if (threshold < 0) throw new BadRequestException("Ngưỡng tồn âm ở dòng " + (i + 1));
                Product existing = products.findByStudioIdAndSkuIgnoreCase(studioId, sku).orElse(null);
                BigDecimal gross = net.multiply(BigDecimal.ONE.add(vat.movePointLeft(2))).setScale(2, RoundingMode.HALF_UP);
                if (existing != null && existing.getVatRate().compareTo(vat) == 0) {
                    BigDecimal oldNet = existing.getSellPrice().divide(BigDecimal.ONE.add(vat.movePointLeft(2)),
                            2, RoundingMode.HALF_UP);
                    if (oldNet.compareTo(net) == 0) gross = existing.getSellPrice();
                }
                String barcode = cell(row, 1).trim();
                ProductRequest request = ProductRequest.builder().sku(sku)
                        .barcode(barcode.isEmpty() ? null : barcode).name(name)
                        .description(existing == null ? null : existing.getDescription())
                        .imageUrl(existing == null ? null : existing.getImageUrl())
                        .categoryId(category.isEmpty() ? null : categoryIds.get(category.toLowerCase(Locale.ROOT)))
                        .unit(unit).costPrice(cost).sellPrice(gross).vatRate(vat)
                        .lowStockThreshold(threshold)
                        .trackInventory(bool(cell(row, 9), i))
                        .isActive(bool(cell(row, 10), i)).build();
                entries.add(new Entry(i + 1, request));
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Không đọc được tệp .xlsx: " + ex.getMessage());
        }
        int created = 0, updated = 0;
        for (Entry entry : entries) {
            Product existing = products.findByStudioIdAndSkuIgnoreCase(studioId, entry.request().getSku()).orElse(null);
            try {
                if (existing == null) {
                    productService.createProduct(entry.request()); created++;
                } else {
                    productService.updateProduct(existing.getId(), entry.request()); updated++;
                }
            } catch (RuntimeException ex) {
                throw new BadRequestException("Dòng " + entry.row() + ": " + ex.getMessage());
            }
        }
        return new ImportResult(created, updated, entries.size());
    }

    private String cell(Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        if (cell.getCellType() == CellType.FORMULA) throw new BadRequestException("Tệp Excel không được chứa công thức");
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> throw new BadRequestException("Kiểu ô Excel không hợp lệ");
        };
    }

    private BigDecimal money(String value, int row) {
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.signum() < 0 || number.scale() > 2) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Giá hoặc VAT không hợp lệ ở dòng " + (row + 1));
        }
    }

    private Boolean bool(String value, int row) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new BadRequestException("Giá trị TRUE/FALSE không hợp lệ ở dòng " + (row + 1));
    }
}
