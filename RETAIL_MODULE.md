# Module Bán hàng (POS) — FITOS

Tài liệu triển khai module bán lẻ được thêm vào hệ thống FITOS.

---

## 1. Tổng quan

Hệ thống trước đây **chỉ bán được gói tập** (`service_plans` → `orders`). Module này bổ sung khả năng bán lẻ thực tế tại quầy:

| Bán được gì | Ví dụ |
|---|---|
| Thực phẩm bổ sung | Whey, Creatine, BCAA, Pre-workout |
| Nước uống | Nước khoáng, isotonic, tăng lực, trà |
| Dụng cụ tập | Găng tay, đai lưng, dây kháng lực, bình lắc |
| Phụ kiện | Khăn tập, dây đeo thẻ |
| Gói tập | Vẫn bán như cũ, nay nằm chung một giỏ hàng |
| Suất lớp | Giữ chỗ lớp học ngay khi thu tiền |

Một đơn có thể **trộn nhiều loại**: 1 whey + 3 nước + 1 gói PT + 1 suất lớp.

---

## 2. Kiến trúc

### Quyết định: tái sử dụng bảng `orders`, không tạo bảng mới

Thêm `orders.order_type` (`PLAN_SALE | RETAIL | RETAIL_RETURN`) thay vì tạo `retail_orders`.

**Lý do:** thanh toán, đối soát, mã giảm giá, báo cáo doanh thu và toàn bộ FK đa studio đều neo vào `orders(id, studio_id)`. Tạo bảng riêng sẽ nhân đôi hạ tầng thanh toán và làm hỏng danh sách đơn hiện có.

### Thay đổi schema

File migration:
- `backend/src/main/resources/db/migration/V6_1__retail_pos.sql`
- `backend/src/main/resources/db/migration/V6_2__retail_seed_catalog.sql`

Bảng mới:
- `product_categories` — danh mục hàng hoá
- `products` — sản phẩm (SKU, mã vạch, giá vốn/bán, tồn, ngưỡng cảnh báo)
- `inventory_transactions` — mọi biến động kho (nhập/bán/trả/điều chỉnh/huỷ)

Mở rộng:
- `orders` + `order_type`, `client_request_id` (idempotency), `parent_order_id`, `created_by`
- `order_items` + `item_type`, `product_id`, `session_id`, `booking_id`, `item_name`, `unit_cost`, `vat_rate`, `vat_amount`
- `order_items.plan_id` **bỏ NOT NULL** — đây là blocker chính, không có nó thì không thêm được dòng sản phẩm

Ràng buộc quan trọng:
```sql
-- Mỗi dòng chỉ có đúng một "đích", khớp với item_type
CHECK (
     (item_type = 'PLAN'    AND plan_id    IS NOT NULL AND product_id IS NULL)
  OR (item_type = 'PRODUCT' AND product_id IS NOT NULL AND plan_id    IS NULL)
  OR (item_type = 'BOOKING' AND session_id IS NOT NULL AND product_id IS NULL)
)
```

---

## 3. Backend

### File mới

```
model/Product.java, ProductCategory.java, InventoryTransaction.java
repository/ProductRepository.java, ProductCategoryRepository.java,
           InventoryTransactionRepository.java
service/ProductService.java, InventoryService.java,
        RetailOrderService.java, RetailReportService.java
controller/ProductController.java, InventoryController.java,
           RetailOrderController.java, RetailReportController.java
dto/retail/RetailDtos.java
retail/RetailPricing.java
```

### API

Tất cả dưới `/api/retail/**`.

| Method | Path | Quyền | Chức năng |
|---|---|---|---|
| GET | `/categories` | STAFF | Danh sách danh mục |
| POST/PUT/PATCH | `/categories[/{id}]` | MANAGEMENT | Thêm/sửa/bật-tắt danh mục |
| GET | `/products` | STAFF | Tìm theo tên/SKU/mã vạch, lọc danh mục, phân trang |
| GET | `/products/barcode/{code}` | STAFF | Tra cứu máy quét |
| POST/PUT/PATCH | `/products[/{id}]` | MANAGEMENT | CRUD sản phẩm |
| POST | `/inventory/stock-in` | MANAGEMENT | Nhập kho (bình quân gia quyền) |
| POST | `/inventory/adjust` | MANAGEMENT | Điều chỉnh theo kiểm kê |
| GET | `/inventory/low-stock` | STAFF | Mặt hàng sắp hết |
| GET | `/inventory/movements` | STAFF | Lịch sử kho |
| POST | `/orders` | STAFF | Tạo đơn POS |
| POST | `/orders/{id}/pay` | STAFF | Thu tiền |
| GET | `/orders[/{id}]` | STAFF | Danh sách / chi tiết |
| GET | `/orders/{id}/receipt` | STAFF | Dữ liệu in hoá đơn |
| PATCH | `/orders/{id}/cancel` | STAFF | Huỷ đơn chờ (hoàn kho) |
| POST | `/orders/{id}/return` | MANAGEMENT | Trả hàng |
| POST | `/orders/quick-customer` | STAFF | Tạo nhanh khách tại quầy |
| GET | `/reports/daily` | MANAGEMENT | Tổng kết ngày |

### Quy tắc nghiệp vụ

**Chống bán vượt tồn (3 lớp):**
1. `PESSIMISTIC_WRITE` trên dòng `products` khi bán
2. Kiểm tra `stock >= quantity` trong cùng transaction
3. `CHECK (stock_quantity >= 0)` + unique index `uq_inventory_sale_once`

**Idempotency:** client sinh `clientRequestId` khi mở giỏ. Retry cùng ID trả lại đơn cũ (`idempotentReplay: true`), không tạo trùng. Bảo vệ lớp 2 ở DB: unique index `(studio_id, client_request_id)`.

**Giá:** tính hoàn toàn server-side, DTO **không có** trường giá. `sell_price` đã bao gồm VAT nên `total = subtotal - discount`, không cộng thêm thuế.

**Trả hàng:** tạo đơn mới `RETAIL_RETURN` (tổng dương, `parent_order_id` trỏ đơn gốc), hoàn kho, **không** tạo payment row (vì `payments.amount >= 0`).

---

## 4. Frontend

### Trang mới

**`/dashboard/pos`** — Màn hình bán hàng
- Lưới sản phẩm + tab danh mục + tìm kiếm/mã vạch
- Giỏ hàng bên phải: stepper số lượng, chọn khách, giảm giá, phương thức thanh toán, tiền thối
- Phím tắt: `F2` focus tìm kiếm, `F4` thanh toán, `Esc` đóng
- In hoá đơn khổ 80mm (`@media print`)
- Giỏ lưu `localStorage` — F5 không mất đơn

**`/dashboard/retail-orders`** — Quản lý đơn bán lẻ
- KPI hôm nay: doanh thu, đã trả hàng, lãi gộp, số đơn/món
- Lọc theo trạng thái: đã thanh toán / chờ / đã huỷ / đã trả hàng
- Chi tiết đơn + in hoá đơn
- **Thu tiền** đơn đang treo (PENDING)
- **Huỷ đơn** PENDING → hoàn kho + trả suất lớp
- **Trả hàng** từng phần: chọn số lượng theo từng mặt hàng, hiện tiền hoàn dự kiến, tự nhập lại kho

**`/dashboard/retail`** — Quản trị hàng hoá (ADMIN/MANAGER)
- 5 tab: Sản phẩm · Danh mục · Nhập kho · Lịch sử kho · Tồn thấp
- Tạo sản phẩm có tồn khởi tạo (tự gọi nhập kho để có lịch sử)
- Điều chỉnh tồn bắt buộc nhập lý do

### File

```
app/dashboard/pos/page.tsx
app/dashboard/retail-orders/page.tsx
app/dashboard/retail/page.tsx
lib/retailApi.ts
styles/globals.css  (+ block @media print)
components/Sidebar.tsx  (+ nhóm "BÁN HÀNG & KHO")
```

---

## 5. Danh mục mẫu

15 sản phẩm, giá thị trường Việt Nam:

| Sản phẩm | Giá bán | Tồn |
|---|---|---|
| Whey Protein Concentrate 2kg | 1.450.000 | 12 |
| Whey Isolate 1.8kg | 1.890.000 | 8 |
| Creatine Monohydrate 300g | 550.000 | 20 |
| BCAA 2:1:1 300g | 690.000 | 10 |
| Pre-Workout 300g | 750.000 | 6 |
| Nước khoáng Aquafina 500ml | 10.000 | 120 |
| Nước tăng lực Sting 330ml | 12.000 | 96 |
| Nước isotonic Revive 500ml | 15.000 | 80 |
| Trà xanh C2 500ml | 12.000 | 60 |
| Găng tay tập gym | 250.000 | 15 |
| Đai lưng tập tạ 4 inch | 420.000 | 10 |
| Bộ dây kháng lực 5 mức | 350.000 | 12 |
| Bình lắc Shaker 600ml | 120.000 | 30 |
| Khăn tập microfiber | 90.000 | 40 |
| Dây đeo thẻ hội viên | 50.000 | 50 |

Biên lãi gộp: TPBV 26–33%, nước 40–50%, dụng cụ 33–55%, phụ kiện 50–60%.

---

## 6. Kết quả kiểm thử

| Hạng mục | Kết quả |
|---|---|
| Backend unit test | 103/103 pass (6 skipped) |
| Trang render × 5 vai trò | 28/28 trang OK mỗi vai trò |
| POS end-to-end qua UI | Bán hàng, in hoá đơn, reset giỏ — OK |
| Đơn bán lẻ end-to-end qua UI | Chi tiết, thu tiền, huỷ, trả hàng, in hoá đơn — OK |
| Quản trị kho qua UI | 5 tab render OK, modal mở OK |
| UI audit 28 trang × 3 kích thước | 0 lỗi tương phản, 0 a11y, 0 tràn ngang |
| Touch target WCAG 2.2 (24px) | 0 vi phạm trên toàn bộ 28 trang |
| Lỗi JS | Không |
| Lỗi API (ngoài 403 phân quyền) | Không |
| Nhất quán sổ kho | 16/16 sản phẩm khớp `stock_quantity` = Σ`quantity_delta` |

### Luồng đã kiểm chứng

- Bán 1 whey + 3 nước → trừ kho đúng (12→11, 120→117)
- Gửi lại cùng `clientRequestId` → trả đơn cũ, không tạo trùng
- Bán 9999 whey khi còn 12 → bị chặn 400
- Nhập 5 whey → tồn 12→17, giá vốn bình quân cập nhật
- Huỷ đơn pending → hoàn kho
- Trả 1 whey → tạo đơn `RET-...`, tồn tăng lại
- Thu tiền đơn treo PENDING → chuyển PAID, ghi `paid_at`
- Lễ tân xem sản phẩm OK, tạo sản phẩm bị 403 (đúng)
- MEMBER bị chặn ở cả 3 trang bán hàng
- Báo cáo ngày: doanh thu 4,14tr, trả 1,45tr, thuần 2,69tr, **lãi gộp 641.364đ**

---

## 7. Lỗi đã phát hiện và sửa trong quá trình xây

| # | Lỗi | Nguyên nhân | Sửa |
|---|---|---|---|
| 1 | Tạo đơn POS trả 500 | `ServiceActivationService` gọi `planRepository.findById(null)` cho dòng sản phẩm | Guard `itemType != 'PLAN'` và `planId != null` |
| 2 | `OrderService.toResponse` sập | Cùng nguyên nhân trên | Chỉ tra `planRepository` khi `planId != null` |
| 3 | Thanh toán thẻ luôn 400 | `PaymentDtos` regexp thiếu `CARD` | Thêm `CARD` |
| 4 | **Lãi gộp báo âm** (-592.390đ) | Không trừ giá vốn của hàng trả lại khỏi COGS | Trừ `returnCogs` và `returnVat` |
| 5 | **`orders.paid_at` luôn NULL** | `OrderService.markPaid` chỉ đổi status, không ghi thời điểm | Ghi `paidAt = Instant.now()` + backfill 7 đơn cũ |
| 6 | Trang Sales hiện 5 đơn giả | Fallback mock (Sophie Turner, James Hargreaves…) khi API lỗi | Bỏ mock, hiện banner lỗi + nút thử lại |
| 7 | Trang Sales lẫn tiếng Anh | Chưa Việt hoá | Dịch toàn bộ nhãn |
| 8 | Trang Tiến độ hiện thành tích giả | Hardcode streak 12, PR 85/120/140kg | Dùng dữ liệu thật, 0 khi chưa có |
| 9 | `getProgressSummary` ném lỗi 403 | Không catch | Trả số 0 khi 403/404 |
| 10 | Test booking hỏng | Bản vá chặn đặt lớp quá giờ | Test dùng ngày mai |
| 11 | Nhiều nút nhỏ hơn 24px | Sidebar 40px, nút X 19px, ô nhập bảng | Nâng lên 44px toàn cục |

---

## 8. Cách dùng

```
http://localhost:3000/dashboard/pos             # bán hàng
http://localhost:3000/dashboard/retail-orders   # xem/thu tiền/huỷ/trả hàng
http://localhost:3000/dashboard/retail          # quản lý kho
```

Quyền:
- **ADMIN, MANAGER**: bán hàng + toàn bộ quản trị kho + trả hàng + báo cáo
- **RECEPTIONIST**: bán hàng, xem đơn, thu tiền, huỷ đơn; **không** nhập kho, **không** trả hàng, **không** xem báo cáo
- **TRAINER, MEMBER**: không truy cập (bị chặn ở cả 3 trang)

---

## 9. Việc có thể làm tiếp

- Quản lý nhà cung cấp + phiếu nhập hàng nhiều dòng
- Mã vạch: tích hợp máy quét USB (hiện đã hỗ trợ qua bàn phím + Enter)
- Báo cáo lãi lỗ theo tháng, theo nhân viên (hiện chỉ có theo ngày)
- Xuất Excel hoá đơn / bảng kê
- Tích hợp cổng thanh toán thật (VNPay/MoMo) — hiện xác nhận tại quầy
- Chương trình tích điểm / thẻ thành viên cho khách mua lẻ
- Hoàn trả cho dòng gói tập/suất lớp (hiện chỉ trả hàng hoá)
- Phân trang cho danh sách đơn (hiện tải 50 đơn gần nhất)
