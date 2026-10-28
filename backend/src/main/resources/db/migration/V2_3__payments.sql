-- =====================================================================
-- V2_3 — Payments (Task #6 — Người B)
-- Phụ thuộc: orders (V2_2)
-- UNIQUE(transaction_ref) là chốt chặn idempotency cho webhook (#42):
-- callback trùng → duplicate key / đã SUCCESS → trả 200, KHÔNG kích hoạt lần 2.
-- =====================================================================

CREATE TABLE payments (
    id               BIGSERIAL       PRIMARY KEY,
    order_id         BIGINT          NOT NULL,
    transaction_ref  VARCHAR(128)    NOT NULL,             -- mã giao dịch từ cổng thanh toán / mã nội bộ
    amount           NUMERIC(12, 2)  NOT NULL,
    payment_method   VARCHAR(30)     NOT NULL,
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    paid_at          TIMESTAMPTZ,
    failure_reason   TEXT,
    raw_payload      JSONB,                                -- payload callback gốc để đối soát
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- UNIQUE tự tạo index → không cần CREATE INDEX riêng cho transaction_ref
    CONSTRAINT uq_payments_transaction_ref UNIQUE (transaction_ref),
    CONSTRAINT fk_payments_order   FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT ck_payments_amount  CHECK (amount > 0),
    CONSTRAINT ck_payments_method  CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CARD', 'VNPAY', 'MOMO')),
    CONSTRAINT ck_payments_status  CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT ck_payments_paid_at CHECK (status <> 'SUCCESS' OR paid_at IS NOT NULL)
);

CREATE INDEX idx_payments_order          ON payments (order_id);
CREATE INDEX idx_payments_status_paid_at ON payments (status, paid_at DESC);  -- revenue chart / sales KPI

-- Mỗi đơn chỉ có tối đa 1 payment SUCCESS → chặn thanh toán 2 lần cho cùng 1 đơn
CREATE UNIQUE INDEX uq_payments_one_success_per_order
    ON payments (order_id)
    WHERE status = 'SUCCESS';
