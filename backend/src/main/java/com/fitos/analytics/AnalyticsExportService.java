package com.fitos.analytics;

import com.fitos.analytics.AnalyticsDtos.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnalyticsExportService {
    private final AnalyticsService analytics;

    public byte[] xlsx(java.time.LocalDate from, java.time.LocalDate to,
                       Integer year, Integer fromYear, Integer toYear) {
        Overview report = analytics.overview(from, to, year, fromYear, toYear);
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet summary = book.createSheet("Tong quan");
            stringRow(summary.createRow(0), "FITOS - Báo cáo thống kê", "Từ", report.from().toString(), "Đến", report.to().toString());
            stringRow(summary.createRow(1), "Chỉ tiêu", "Số tiền");
            moneyRow(summary.createRow(2), "Tổng thu hóa đơn", report.rangeIncome());
            moneyRow(summary.createRow(3), "Tổng chi phiếu nhập", report.rangeExpense());
            moneyRow(summary.createRow(4), "Chênh lệch thu - chi", report.rangeBalance());
            periods(book.createSheet("Theo thang"), report.months());
            periods(book.createSheet("Theo quy"), report.quarters());
            periods(book.createSheet("Theo nam"), report.years());
            matrix(book.createSheet("Theo nhan vien"), report.employees());
            matrix(book.createSheet("Theo khach hang"), report.customers());
            matrix(book.createSheet("Theo san pham"), report.products());
            book.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Không xuất được Excel", ex);
        }
    }

    public byte[] pdf(java.time.LocalDate from, java.time.LocalDate to,
                      Integer year, Integer fromYear, Integer toYear) {
        Overview report = analytics.overview(from, to, year, fromYear, toYear);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            File fontFile = locateFont();
            PDType0Font font = PDType0Font.load(document, fontFile);
            PdfText writer = new PdfText(document, font);
            writer.heading("FITOS - BÁO CÁO THỐNG KÊ");
            writer.line("Từ " + report.from() + " đến " + report.to() + " | Năm: " + report.year());
            writer.line("Tổng thu hóa đơn: " + money(report.rangeIncome()));
            writer.line("Tổng chi phiếu nhập: " + money(report.rangeExpense()));
            writer.line("Chênh lệch thu - chi: " + money(report.rangeBalance()));
            pdfPeriods(writer, "THEO THÁNG", report.months());
            pdfPeriods(writer, "THEO QUÝ", report.quarters());
            pdfPeriods(writer, "THEO NĂM", report.years());
            pdfMatrix(writer, "DOANH THU THEO NHÂN VIÊN", report.employees());
            pdfMatrix(writer, "DOANH THU THEO KHÁCH HÀNG", report.customers());
            pdfMatrix(writer, "DOANH THU THEO SẢN PHẨM", report.products());
            writer.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Không xuất được PDF", ex);
        }
    }

    private void periods(Sheet sheet, List<Period> periods) {
        stringRow(sheet.createRow(0), "Kỳ", "Tổng thu hóa đơn", "Tổng chi PNH", "Chênh lệch thu - chi");
        for (int i = 0; i < periods.size(); i++) {
            Period p = periods.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(p.label());
            row.createCell(1).setCellValue(p.income().doubleValue());
            row.createCell(2).setCellValue(p.expense().doubleValue());
            row.createCell(3).setCellValue(p.balance().doubleValue());
        }
    }

    private void matrix(Sheet sheet, Matrix matrix) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Tên");
        for (int i = 0; i < matrix.columns().size(); i++)
            header.createCell(i + 1).setCellValue(matrix.columns().get(i));
        header.createCell(matrix.columns().size() + 1).setCellValue("Tổng cộng");
        int index = 1;
        for (MatrixRow data : matrix.rows()) {
            Row row = sheet.createRow(index++);
            row.createCell(0).setCellValue(data.name());
            for (int i = 0; i < data.values().size(); i++)
                row.createCell(i + 1).setCellValue(data.values().get(i).doubleValue());
            row.createCell(matrix.columns().size() + 1).setCellValue(data.total().doubleValue());
        }
        Row total = sheet.createRow(index);
        total.createCell(0).setCellValue("Tổng cộng");
        for (int i = 0; i < matrix.totals().size(); i++)
            total.createCell(i + 1).setCellValue(matrix.totals().get(i).doubleValue());
        total.createCell(matrix.columns().size() + 1).setCellValue(matrix.grandTotal().doubleValue());
    }

    private void stringRow(Row row, String... values) {
        for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
    }
    private void moneyRow(Row row, String label, BigDecimal value) {
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value.doubleValue());
    }
    private void pdfPeriods(PdfText writer, String title, List<Period> periods) throws IOException {
        writer.heading(title);
        writer.line("Kỳ  |  Tổng thu  |  Tổng chi PNH  |  Thu - chi");
        for (Period p : periods)
            writer.line(p.label() + "  |  " + money(p.income()) + "  |  "
                    + money(p.expense()) + "  |  " + money(p.balance()));
    }
    private void pdfMatrix(PdfText writer, String title, Matrix matrix) throws IOException {
        writer.heading(title);
        for (MatrixRow row : matrix.rows()) {
            StringBuilder line = new StringBuilder(row.name()).append("  |  ");
            for (int i = 0; i < matrix.columns().size(); i++)
                line.append(matrix.columns().get(i)).append(": ").append(money(row.values().get(i))).append("  ");
            line.append("TC: ").append(money(row.total()));
            writer.wrapped(line.toString());
        }
        writer.line("TỔNG CỘNG: " + money(matrix.grandTotal()));
    }
    private String money(BigDecimal value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + " đ";
    }
    private File locateFont() {
        for (String path : List.of(
                "C:/Windows/Fonts/arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf")) {
            File file = new File(path);
            if (file.isFile()) return file;
        }
        throw new IllegalStateException("Máy chủ thiếu font Unicode để xuất PDF");
    }

    private static final class PdfText {
        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y;
        PdfText(PDDocument document, PDType0Font font) throws IOException {
            this.document = document; this.font = font; newPage();
        }
        void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = 795;
        }
        void heading(String text) throws IOException {
            y -= 8;
            draw(text, 12, 18);
        }
        void line(String text) throws IOException { draw(text, 9, 14); }
        void wrapped(String text) throws IOException {
            String remaining = text;
            while (remaining.length() > 95) {
                int split = remaining.lastIndexOf(' ', 95);
                if (split < 20) split = 95;
                line(remaining.substring(0, split));
                remaining = remaining.substring(split).trim();
            }
            line(remaining);
        }
        void draw(String text, int size, int advance) throws IOException {
            if (y < 45) newPage();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(36, y);
            stream.showText(text.replace('\n', ' '));
            stream.endText();
            y -= advance;
        }
        void close() throws IOException { if (stream != null) stream.close(); }
    }
}
