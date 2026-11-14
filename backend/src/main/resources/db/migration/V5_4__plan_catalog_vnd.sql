-- V5.4: Chuẩn hoá danh mục gói về giá VND và đúng loại gói.
--
-- V2_1 seed giá theo kiểu USD (29 / 49 / 89 / 150 / 280 / 500) cho một sản phẩm
-- bán tại Việt Nam, trong khi trang bảng giá và tài liệu đều tính bằng VND. Hệ quả:
-- đơn hàng thật được tạo với giá 29đ. Đồng thời "PT Focus" bị gắn plan_type
-- MEMBERSHIP dù là gói PT 1-1.
--
-- Chỉ cập nhật bản ghi còn mang giá trị seed cũ để không ghi đè giá đã chỉnh tay.

UPDATE service_plans SET price = 290000.00, duration_days = 30
WHERE name = 'Move' AND price = 29.00;

UPDATE service_plans SET price = 490000.00, duration_days = 30
WHERE name = 'Perform' AND price = 49.00;

UPDATE service_plans
SET price = 1200000.00,
    plan_type = 'PT_PACKAGE',
    duration_days = 30,
    session_count = 4,
    total_sessions = 4
WHERE name = 'PT Focus' AND price = 89.00;

UPDATE service_plans SET price = 1500000.00, duration_days = 60
WHERE name = 'PT Starter 5' AND price = 150.00;

UPDATE service_plans SET price = 2800000.00, duration_days = 90
WHERE name = 'PT Pro 10' AND price = 280.00;

UPDATE service_plans SET price = 5000000.00, duration_days = 180
WHERE name = 'PT Elite 20' AND price = 500.00;

-- Đảm bảo mọi gói PT đều có session_count hợp lệ để kích hoạt gói hoạt động.
UPDATE service_plans
SET session_count = COALESCE(session_count, total_sessions)
WHERE plan_type = 'PT_PACKAGE' AND session_count IS NULL AND total_sessions IS NOT NULL;
