-- =====================================================================
-- V2_1 — Service Plans (Task #4 — Người B)
-- Danh mục gói dịch vụ: MEMBERSHIP (thẻ hội viên) và PT_PACKAGE (gói PT).
-- Giá niêm yết ở đây là NGUỒN SỰ THẬT DUY NHẤT cho server-side pricing (#38).
-- =====================================================================

CREATE TABLE service_plans (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    plan_type       VARCHAR(20)     NOT NULL,
    price           NUMERIC(12, 2)  NOT NULL,
    duration_days   INT             NOT NULL,
    total_sessions  INT,                                   -- chỉ dùng cho PT_PACKAGE
    description     TEXT,
    features        JSONB           NOT NULL DEFAULT '[]'::jsonb,  -- danh sách quyền lợi hiển thị ở /plans (#80)
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_service_plans_name      UNIQUE (name),
    CONSTRAINT ck_service_plans_type      CHECK (plan_type IN ('MEMBERSHIP', 'PT_PACKAGE')),
    CONSTRAINT ck_service_plans_price     CHECK (price >= 0),
    CONSTRAINT ck_service_plans_duration  CHECK (duration_days > 0),
    -- PT_PACKAGE bắt buộc có số buổi > 0; MEMBERSHIP không được có số buổi
    CONSTRAINT ck_service_plans_sessions  CHECK (
        (plan_type = 'PT_PACKAGE' AND total_sessions IS NOT NULL AND total_sessions > 0)
     OR (plan_type = 'MEMBERSHIP' AND total_sessions IS NULL)
    )
);

CREATE INDEX idx_service_plans_type_active ON service_plans (plan_type, is_active);

COMMENT ON TABLE  service_plans                IS 'Danh mục gói dịch vụ (MEMBERSHIP / PT_PACKAGE)';
COMMENT ON COLUMN service_plans.price          IS 'Giá niêm yết (VND). Backend luôn tính tiền từ cột này, không tin giá từ client';
COMMENT ON COLUMN service_plans.duration_days  IS 'Thời hạn hiệu lực tính từ ngày kích hoạt';
COMMENT ON COLUMN service_plans.total_sessions IS 'Số buổi PT (chỉ PT_PACKAGE)';
