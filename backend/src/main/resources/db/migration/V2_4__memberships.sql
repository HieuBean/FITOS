-- =====================================================================
-- V2_4 — Memberships & PT Packages (Task #7 — Người B)
-- Phụ thuộc: customers (V1), service_plans (V2_1), orders (V2_2)
-- Được tạo bởi ServiceActivationService (#43) sau khi payment SUCCESS.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Thẻ hội viên
-- Lifecycle: ACTIVE ⇄ PAUSED → EXPIRED / CANCELLED  (API do C làm — #49, #50)
-- ---------------------------------------------------------------------
CREATE TABLE memberships (
    id                  BIGSERIAL    PRIMARY KEY,
    customer_id         BIGINT       NOT NULL,
    plan_id             BIGINT       NOT NULL,
    order_id            BIGINT,
    start_date          DATE         NOT NULL,
    end_date            DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    paused_at           DATE,                               -- ngày bắt đầu tạm dừng hiện tại
    total_paused_days   INT          NOT NULL DEFAULT 0,    -- cộng dồn khi resume → end_date += số ngày dừng (test #151)
    cancelled_at        TIMESTAMPTZ,
    version             BIGINT       NOT NULL DEFAULT 0,    -- @Version optimistic locking
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_memberships_customer FOREIGN KEY (customer_id) REFERENCES customers (id)     ON DELETE RESTRICT,
    CONSTRAINT fk_memberships_plan     FOREIGN KEY (plan_id)     REFERENCES service_plans (id) ON DELETE RESTRICT,
    CONSTRAINT fk_memberships_order    FOREIGN KEY (order_id)    REFERENCES orders (id)        ON DELETE SET NULL,
    CONSTRAINT ck_memberships_status   CHECK (status IN ('ACTIVE', 'PAUSED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_memberships_dates    CHECK (end_date >= start_date),
    CONSTRAINT ck_memberships_paused   CHECK ((status = 'PAUSED') = (paused_at IS NOT NULL)),
    CONSTRAINT ck_memberships_pdays    CHECK (total_paused_days >= 0)
);

CREATE INDEX idx_memberships_customer_status ON memberships (customer_id, status);  -- MembershipValidator.requireActive (#44)
CREATE INDEX idx_memberships_status_end_date ON memberships (status, end_date);     -- GET /memberships/expiring, cron hết hạn
CREATE INDEX idx_memberships_order           ON memberships (order_id);
-- Webhook trùng không thể tạo 2 thẻ cho cùng 1 đơn hàng
CREATE UNIQUE INDEX uq_memberships_order     ON memberships (order_id) WHERE order_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- Gói PT cá nhân
-- remaining_sessions bị trừ bởi PATCH /pt-packages/:id/use (D — #62) dùng @Version
-- ---------------------------------------------------------------------
CREATE TABLE pt_packages (
    id                  BIGSERIAL    PRIMARY KEY,
    customer_id         BIGINT       NOT NULL,
    plan_id             BIGINT       NOT NULL,
    order_id            BIGINT,
    total_sessions      INT          NOT NULL,
    remaining_sessions  INT          NOT NULL,
    start_date          DATE         NOT NULL,
    expiry_date         DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version             BIGINT       NOT NULL DEFAULT 0,    -- @Version optimistic locking (#62)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pt_packages_customer FOREIGN KEY (customer_id) REFERENCES customers (id)     ON DELETE RESTRICT,
    CONSTRAINT fk_pt_packages_plan     FOREIGN KEY (plan_id)     REFERENCES service_plans (id) ON DELETE RESTRICT,
    CONSTRAINT fk_pt_packages_order    FOREIGN KEY (order_id)    REFERENCES orders (id)        ON DELETE SET NULL,
    CONSTRAINT ck_pt_packages_status   CHECK (status IN ('ACTIVE', 'DEPLETED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_pt_packages_total    CHECK (total_sessions > 0),
    -- Không bao giờ âm và không vượt tổng → lớp phòng thủ cuối cùng ở DB
    CONSTRAINT ck_pt_packages_remain   CHECK (remaining_sessions >= 0 AND remaining_sessions <= total_sessions),
    CONSTRAINT ck_pt_packages_dates    CHECK (expiry_date >= start_date)
);

CREATE INDEX idx_pt_packages_customer_status ON pt_packages (customer_id, status);  -- GET /pt-packages/me
CREATE INDEX idx_pt_packages_status_expiry   ON pt_packages (status, expiry_date);
CREATE INDEX idx_pt_packages_order           ON pt_packages (order_id);
CREATE UNIQUE INDEX uq_pt_packages_order     ON pt_packages (order_id) WHERE order_id IS NOT NULL;
