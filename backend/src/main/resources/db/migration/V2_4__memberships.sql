-- V2.4: Active Memberships & PT Packages
-- Author: Member B (Commerce & Memberships) & Team Merged

CREATE TABLE IF NOT EXISTS memberships (
    id                BIGSERIAL PRIMARY KEY,
    customer_id       BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    plan_id           BIGINT      NOT NULL REFERENCES service_plans(id) ON DELETE RESTRICT,
    order_id          BIGINT      REFERENCES orders(id) ON DELETE SET NULL,
    start_date        DATE        NOT NULL,
    end_date          DATE        NOT NULL,
    status            VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('PENDING', 'ACTIVE', 'PAUSED', 'EXPIRED', 'CANCELLED')),
    paused_at         DATE,
    pause_days_used   INT         NOT NULL DEFAULT 0 CHECK (pause_days_used >= 0),
    total_paused_days INT         NOT NULL DEFAULT 0 CHECK (total_paused_days >= 0),
    cancelled_at      TIMESTAMP WITH TIME ZONE,
    auto_renew        BOOLEAN     NOT NULL DEFAULT FALSE,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_membership_dates CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_memberships_customer      ON memberships(customer_id);
CREATE INDEX IF NOT EXISTS idx_memberships_status_expiry ON memberships(status, end_date);

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pt_packages (
    id                 BIGSERIAL PRIMARY KEY,
    customer_id        BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    plan_id            BIGINT      NOT NULL REFERENCES service_plans(id) ON DELETE RESTRICT,
    order_id           BIGINT      REFERENCES orders(id) ON DELETE SET NULL,
    total_sessions     INT         NOT NULL CHECK (total_sessions > 0),
    remaining_sessions INT         NOT NULL CHECK (remaining_sessions >= 0),
    start_date         DATE,
    expires_at         DATE        NOT NULL,
    expiry_date        DATE,
    version            BIGINT      NOT NULL DEFAULT 0,
    status             VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('ACTIVE', 'COMPLETED', 'DEPLETED', 'EXPIRED', 'CANCELLED')),
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pt_packages_customer      ON pt_packages(customer_id);
CREATE INDEX IF NOT EXISTS idx_pt_packages_status_expiry ON pt_packages(status, expires_at);
