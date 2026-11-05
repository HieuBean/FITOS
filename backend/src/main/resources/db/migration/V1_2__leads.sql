-- V1.2: Leads, Lead Activities & Audit Logs
-- Author: Member A (CRM & Leads)

CREATE TABLE IF NOT EXISTS leads (
    id                    BIGSERIAL PRIMARY KEY,
    full_name             VARCHAR(100) NOT NULL,
    phone                 VARCHAR(20)  NOT NULL,
    email                 VARCHAR(100),
    dob                   DATE,
    goal                  VARCHAR(50)
                            CHECK (goal IN ('WEIGHT_LOSS', 'MUSCLE_GAIN', 'GENERAL_FITNESS', 'STRENGTH', 'MOBILITY', 'OTHER')),
    source                VARCHAR(50)  NOT NULL DEFAULT 'WALK_IN'
                            CHECK (source IN ('WALK_IN', 'WEBSITE', 'FACEBOOK', 'INSTAGRAM', 'REFERRAL', 'ADVERTISEMENT', 'OTHER')),
    status                VARCHAR(50)  NOT NULL DEFAULT 'NEW'
                            CHECK (status IN ('NEW', 'CONTACTED', 'INTERESTED', 'TRIAL', 'NEGOTIATING', 'CONVERTED', 'LOST')),
    assigned_to           BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    notes                 TEXT,
    lost_reason           TEXT,
    converted_customer_id BIGINT,      -- FK added in V1.3 after customers table is created
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_leads_status      ON leads(status);
CREATE INDEX IF NOT EXISTS idx_leads_source      ON leads(source);
CREATE INDEX IF NOT EXISTS idx_leads_phone       ON leads(phone);
CREATE INDEX IF NOT EXISTS idx_leads_assigned_to ON leads(assigned_to);
CREATE INDEX IF NOT EXISTS idx_leads_created_at  ON leads(created_at DESC);

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS lead_activities (
    id            BIGSERIAL PRIMARY KEY,
    lead_id       BIGINT       NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    user_id       BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    activity_type VARCHAR(50)  NOT NULL
                    CHECK (activity_type IN ('CALL', 'MESSAGE', 'CONSULTATION', 'TRIAL', 'NOTE', 'FOLLOW_UP', 'OTHER')),
    notes         TEXT         NOT NULL,
    activity_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lead_activities_lead_time ON lead_activities(lead_id, activity_time DESC);

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    entity_name VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    details     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user   ON audit_logs(user_id, created_at DESC);
