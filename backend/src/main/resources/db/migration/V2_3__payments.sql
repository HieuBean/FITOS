-- V2.3: Payments & Financial Transactions
-- Author: Member B (Commerce & Payments) & Team Merged

CREATE TABLE IF NOT EXISTS payments (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT         NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    customer_id      BIGINT         NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    transaction_ref  VARCHAR(128)   NOT NULL UNIQUE,
    payment_method   VARCHAR(50)    NOT NULL
                       CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CARD', 'VNPAY', 'MOMO', 'STRIPE', 'OTHER')),
    amount           NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    status           VARCHAR(50)    NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    paid_at          TIMESTAMP WITH TIME ZONE,
    failure_reason   TEXT,
    raw_payload      JSONB,
    payload_response JSONB,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payments_order      ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_customer   ON payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_tx_ref     ON payments(transaction_ref);
CREATE INDEX IF NOT EXISTS idx_payments_status     ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);
