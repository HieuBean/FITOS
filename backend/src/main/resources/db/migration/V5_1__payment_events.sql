-- V5.1: Payment gateway events (idempotent callbacks), commerce & renewal indexes
-- Author: Khanh (C-C-C-C-C-A) — Layer DB / BE

CREATE TABLE IF NOT EXISTS payment_events (
    id         BIGSERIAL PRIMARY KEY,
    studio_id  BIGINT       NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    payment_id BIGINT       REFERENCES payments(id) ON DELETE CASCADE,
    -- event_key là khóa chống trùng: một callback chỉ được xử lý đúng một lần
    event_key  VARCHAR(150) NOT NULL UNIQUE,
    event_type VARCHAR(50)  NOT NULL,
    payload    JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_events_payment ON payment_events(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_created ON payment_events(created_at DESC);

-- Tăng tốc truy vấn gia hạn / sắp hết hạn theo studio
CREATE INDEX IF NOT EXISTS idx_memberships_studio_expiry
    ON memberships(studio_id, status, end_date);
CREATE INDEX IF NOT EXISTS idx_memberships_customer_status
    ON memberships(customer_id, status);

-- Tra cứu đơn hàng của hội viên theo trạng thái (trang doanh số / thanh toán)
CREATE INDEX IF NOT EXISTS idx_orders_customer_status ON orders(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_studio_paid_at  ON orders(studio_id, status, created_at DESC);

-- Lịch tuần: lấy phiên theo studio + ngày
CREATE INDEX IF NOT EXISTS idx_class_sessions_studio_date ON class_sessions(studio_id, session_date);

-- Điểm danh theo studio/ngày cho quầy lễ tân
CREATE INDEX IF NOT EXISTS idx_attendance_studio_time ON attendance(studio_id, checkin_time DESC);
