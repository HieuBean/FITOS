-- =====================================================================
-- V2_2 — Orders & Order Items (Task #5 — Người B)
-- Phụ thuộc: customers (V1 — Người A), service_plans (V2_1)
-- Tiền tệ dùng NUMERIC(12,2) — tuyệt đối không dùng FLOAT/DOUBLE.
-- =====================================================================

CREATE TABLE orders (
    id                BIGSERIAL       PRIMARY KEY,
    order_reference   VARCHAR(64)     NOT NULL,            -- vd: ORD-20261021-9821
    customer_id       BIGINT          NOT NULL,
    subtotal_amount   NUMERIC(12, 2)  NOT NULL,            -- tổng tiền trước giảm giá
    discount_amount   NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    total_amount      NUMERIC(12, 2)  NOT NULL,            -- = subtotal - discount
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    notes             TEXT,
    created_by        BIGINT,                              -- user (sales/staff) tạo đơn
    paid_at           TIMESTAMPTZ,
    cancelled_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_orders_reference  UNIQUE (order_reference),
    CONSTRAINT fk_orders_customer   FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by)  REFERENCES users (id)     ON DELETE SET NULL,
    CONSTRAINT ck_orders_status     CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_orders_subtotal   CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_orders_discount   CHECK (discount_amount >= 0 AND discount_amount <= subtotal_amount),
    -- Chặn tổng âm & đảm bảo tổng luôn khớp (hỗ trợ test #149)
    CONSTRAINT ck_orders_total      CHECK (total_amount >= 0 AND total_amount = subtotal_amount - discount_amount)
);

CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at DESC);  -- GET /orders/me, Customer 360
CREATE INDEX idx_orders_status_created   ON orders (status, created_at DESC);       -- Sales dashboard, lọc PENDING

CREATE TABLE order_items (
    id          BIGSERIAL       PRIMARY KEY,
    order_id    BIGINT          NOT NULL,
    plan_id     BIGINT          NOT NULL,
    plan_name   VARCHAR(100)    NOT NULL,                  -- snapshot tên gói tại thời điểm mua
    unit_price  NUMERIC(12, 2)  NOT NULL,                  -- snapshot giá tại thời điểm mua
    quantity    INT             NOT NULL DEFAULT 1,
    line_total  NUMERIC(12, 2)  NOT NULL,

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)        ON DELETE CASCADE,
    CONSTRAINT fk_order_items_plan  FOREIGN KEY (plan_id)  REFERENCES service_plans (id) ON DELETE RESTRICT,
    CONSTRAINT ck_order_items_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_qty   CHECK (quantity > 0),
    CONSTRAINT ck_order_items_total CHECK (line_total = unit_price * quantity)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_plan  ON order_items (plan_id);

COMMENT ON COLUMN order_items.unit_price IS 'Snapshot giá lúc mua — đổi giá plan sau này không làm sai lịch sử đơn';
