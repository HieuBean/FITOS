# Hóa đơn, phiếu nhập và báo cáo

## Quyền và màn hình

| Vai trò | Màn hình | API |
| --- | --- | --- |
| Quản lý | Sản phẩm, phiếu nhập, hóa đơn, báo cáo | `/api/retail/products/workbook`, `/api/purchase-receipts`, `/api/invoices`, `/api/analytics` |
| Lễ tân | Danh sách và chi tiết hóa đơn | `/api/invoices` |

Trang sản phẩm tải danh sách một lần, sau đó tìm theo tên, SKU, mã vạch không phân biệt chữ hoa, chữ thường hoặc dấu tiếng Việt. Bộ lọc danh mục và khoảng giá bán đã gồm VAT (hai mốc được tính vào kết quả) cũng chạy trên danh sách trong bộ nhớ trình duyệt. Dữ liệu được tải lại sau khi thêm, sửa hoặc nhập Excel.

Khi tạo sản phẩm, quản lý nhập giá bán chưa VAT, VAT và ngưỡng cảnh báo tồn; giao diện tự hiển thị giá bán đã gồm VAT. Giá vốn và tồn khởi tạo không nhập tại đây mà được ghi nhận khi xác nhận phiếu nhập. API từ chối bật bán sản phẩm có giá bán bằng 0.

## Phiếu nhập hàng

Quản lý lập phiếu gồm nhà cung cấp và nhiều dòng sản phẩm. Phiếu `DRAFT` có thể sửa và chưa ảnh hưởng tồn kho hoặc báo cáo. `POST /api/purchase-receipts/{id}/confirm` xác nhận phiếu trong một giao dịch: mỗi dòng tạo biến động nhập kho, tổng phiếu được tính vào chi phí ngày xác nhận. Gọi xác nhận lại không nhập kho lần hai. Chỉ nhận sản phẩm cùng studio có quản lý tồn kho.

Các lần nhập kho `STOCK_IN` trước đây được chuyển thành phiếu lịch sử qua migration `V6_3`; thao tác này không cộng tồn kho lần nữa. API nhập kho một dòng cũ cũng tạo một phiếu đã xác nhận để báo cáo không bỏ sót.

## Hóa đơn và tìm kiếm

`POST /api/invoices/search` trả về các đơn đã thu tiền, có phân trang. Trường `keyword` tùy chọn tìm đồng thời trong mã hóa đơn và tên khách hàng. Truy vấn dùng tham số SQL và luôn giới hạn theo studio.

`GET /api/invoices/{id}` trả chi tiết hóa đơn đang chọn gồm dòng hàng và phiếu trả hàng. Giá trị thuần của hóa đơn trừ các phiếu trả đã hoàn tiền. Màn hình hiển thị danh sách bên trái và chi tiết bên phải.

## Quy tắc báo cáo

Trang `/dashboard/reports` có bốn tab: Theo ngày, Thu–Chi–LN, Theo NV / KH và Theo Sản phẩm. Mỗi tab đặt bộ lọc phía trên, kết quả ở giữa và nút xuất Excel/PDF ở dưới. Tab Theo ngày cho chọn hai ngày, hiển thị hai dòng bán hàng (tách POS và gói) / nhập hàng, số chứng từ, danh sách có phân trang và chi tiết chứng từ đang chọn. Tổng giá trị hóa đơn cộng hai chiều bán và nhập; đây là tổng giao dịch, không phải lợi nhuận.

`GET /api/analytics/invoice-range?from=YYYY-MM-DD&to=YYYY-MM-DD` chỉ dành cho quản lý và chỉ lấy dữ liệu thuộc studio hiện tại. Hai ngày đều được tính trọn theo múi giờ Việt Nam. POS và gói lấy hóa đơn đã thanh toán theo ngày thanh toán; hoàn tiền POS được trừ theo ngày trả hàng. Nhập hàng lấy phiếu đã xác nhận theo ngày xác nhận. API trả `posSales`, `packageSales`, `salesTotal`, `purchaseTotal`, `invoiceTotal`.

`GET /api/analytics/transactions` nhận `from`, `to`, `page`, `size` và tùy chọn `kind=SALE|PURCHASE`, `employeeId`, `customerId`, `productId` để liệt kê các chứng từ tạo nên ô thống kê. Phiếu hoàn tiền POS được liệt kê riêng với giá trị âm. Bấm đúp vào ô số liệu của bảng theo kỳ, nhân viên/khách hàng hoặc sản phẩm để xem danh sách phù hợp. `GET /api/analytics/available-years` trả dải năm từ năm có dữ liệu sớm nhất đến năm hiện tại hoặc năm dữ liệu muộn nhất; bảng theo NV/KH sinh cột từ dải này.

`GET /api/analytics/overview` dùng `from`, `to`, `year`, `fromYear`, `toYear`. Khoảng ngày bao gồm cả hai đầu, tính theo múi giờ Việt Nam. Tổng thu gồm đơn gói dịch vụ và bán lẻ đã thanh toán; khoản trả hàng đã hoàn tiền được trừ tại ngày trả. Tổng chi là giá trị phiếu nhập đã xác nhận theo ngày xác nhận. `Thu - chi` chỉ là chênh lệch của hai khoản này, chưa phải lợi nhuận kế toán vì chưa tính lương, thuê mặt bằng, chi phí khác và giá vốn hàng đã bán.

Bảng tháng, quý và năm lấy cùng quy tắc trên. Ma trận nhân viên và khách hàng phân bổ doanh thu theo năm; đơn cũ không có người tạo nằm trong dòng “Chưa xác định”. Ma trận sản phẩm theo quý chỉ gồm các dòng sản phẩm trong đơn, phân bổ giảm giá của đơn theo tỷ lệ giá trị dòng. Đơn gói và suất lớp không nằm trong ma trận sản phẩm.

`GET /api/analytics/export.xlsx` và `/export.pdf` dùng cùng tham số và cùng dữ liệu với trang báo cáo.

## Excel sản phẩm

`GET /api/retail/products/workbook` xuất `.xlsx`. Tệp nhập cần các cột đúng thứ tự: `SKU`, `Barcode`, `Name`, `Category`, `Unit`, `CostPrice`, `NetSellPrice`, `VatRate`, `LowStockThreshold`, `TrackInventory`, `Active`.

`POST /api/retail/products/workbook` nhận multipart `file`, tối đa 5 MB và 5000 dòng. SKU là khóa cập nhật: SKU đã có được sửa, SKU mới được thêm. `NetSellPrice` là giá trước VAT; giá bán lưu trong hệ thống được tính từ giá này và `VatRate`. Việc nhập Excel không thay đổi số lượng tồn kho; dùng phiếu nhập để ghi nhận tồn. Tệp có công thức, dữ liệu sai hoặc SKU trùng bị từ chối toàn bộ.

Mốc khôi phục demo local đã được chụp lại sau migration `V6_3`; bản trước khi có phiếu nhập được lưu tại `.dsh/demo-baseline-before-purchase-2026-11-18.dump` ngoài Git.
