# Dữ liệu mẫu local

Script `seed_demo.py` nạp dữ liệu vào studio mặc định qua backend API. Script chỉ chấp nhận API trên `localhost` hoặc `127.0.0.1`; dữ liệu demo không được thêm vào Flyway nên không xuất hiện trong môi trường triển khai.

Với backend đang chạy ở cổng `18081`:

```powershell
$env:FITOS_ADMIN_PASSWORD = '<mật khẩu admin local>'
python scripts/seed_demo.py
python scripts/test_demo_seed.py
```

Nếu backend chạy ở cổng khác, đặt thêm `FITOS_API_BASE`, ví dụ `http://localhost:8080/api`.

Script hiện tạo 1 Quản lý, 3 Lễ tân (gồm 2 tài khoản mới), 1 tài khoản HLV và 8 Hội viên. Dữ liệu mẫu gồm 17 lead và lịch sử tư vấn, 11 đơn gói đã thanh toán cùng 1 đơn đang chờ, 8 membership, 3 gói PT, 14 buổi lớp sắp tới, ít nhất 8 lượt đặt chỗ, 4 buổi PT, 3 sản phẩm có phiếu nhập kho và 8 đơn POS đã thanh toán. Hội viên Linh có thêm giáo án tập, buổi tập kèm các set, hai lần ghi chỉ số cơ thể và một lượt điểm danh. Gói dịch vụ, lớp học, 4 hồ sơ HLV và 15 sản phẩm khác có sẵn từ Flyway. Mật khẩu ngẫu nhiên dùng chung cho 13 tài khoản demo được lưu tại `.dsh/demo-accounts.json` (đã bị Git bỏ qua). Chạy lại script không tạo trùng các bản ghi mẫu cùng ngày; script không xóa dữ liệu có sẵn.

## Khôi phục dữ liệu mẫu trên môi trường local

Sau khi nạp dữ liệu demo vào database mới, chụp trạng thái ban đầu:

```powershell
python scripts/capture_demo.py
```

File `.dsh/demo-baseline.dump` nằm ngoài Git. Chỉ dùng `--replace` khi chủ động muốn thay đổi mốc khôi phục. Chạy `python scripts/reset_demo.py --yes` để thay thế toàn bộ database `fitos` bằng bản chụp này; các thay đổi kiểm thử sẽ mất. API khôi phục dành cho kiểm thử tự động chỉ chạy khi server frontend có `FITOS_DEMO_RESET_ENABLED=1`, xác thực bằng tài khoản Admin và được truy cập từ localhost. Backend cần chạy tại `http://localhost:18081/api` (hoặc đặt `FITOS_DEMO_BACKEND_URL` cho server frontend). Docker container PostgreSQL phải là `fitos-postgres` của Compose project `fitos`.

Mốc khôi phục local được cập nhật ngày 19/11/2026 sau khi tạo database mới bằng toàn bộ migration đến `V6.3` và nạp dữ liệu mẫu. Database trước đó được giữ dưới tên `fitos_backup_20261119`; bản sao trạng thái trước khi chuyển nằm ở `.dsh/fitos-before-fresh-2026-11-19.dump`, còn mốc khôi phục demo cũ nằm ở `.dsh/demo-baseline-before-fresh-2026-11-19.dump`. Các file dump đều nằm ngoài Git.

Nếu frontend chạy ở cổng `13000`, backend cũng cần cho phép origin `http://localhost:13000` và `http://127.0.0.1:13000` qua `FITOS_CORS_ALLOWED_ORIGINS` để đăng nhập trên trình duyệt.

Bản chụp chứa toàn bộ database demo, kể cả mật khẩu đã băm; không đưa file dump lên Git.
