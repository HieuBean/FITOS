-- V2.2: Orders & Order Items
-- Author: Member B (Commerce & Orders) & Team Merged

CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT         NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    order_reference VARCHAR(64)    NOT NULL UNIQUE,
    status          VARCHAR(50)    NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'REFUNDED', 'EXPIRED')),
    subtotal_amount NUMERIC(12, 2) NOT NULL CHECK (subtotal_amount >= 0),
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (discount_amount >= 0),
    tax_amount      NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (tax_amount >= 0),
    total_amount    NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    discount_code   VARCHAR(50),
    notes           TEXT,
    created_by      BIGINT         REFERENCES users(id) ON DELETE SET NULL,
    paid_at         TIMESTAMP WITH TIME ZONE,
    cancelled_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_customer   ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_reference  ON orders(order_reference);
CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    plan_id     BIGINT         NOT NULL REFERENCES service_plans(id) ON DELETE RESTRICT,
    plan_name   VARCHAR(100),
    quantity    INT            NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price  NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    total_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (total_price >= 0),
    line_total  NUMERIC(12, 2) DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
