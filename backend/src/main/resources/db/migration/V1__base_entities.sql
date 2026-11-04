-- ============================================================
-- V1__base_entities.sql
-- Foundational base entities required for FITOS platform
-- Defines: studios, customers, trainers, trainer_assignments,
--          classes, class_sessions, pt_packages, pt_sessions
-- ============================================================

CREATE TABLE IF NOT EXISTS studios (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS customers (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    phone       VARCHAR(50),
    created_at  TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS trainers (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    phone       VARCHAR(50),
    specialty   VARCHAR(255),
    bio         TEXT,
    is_active   BOOLEAN      DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS trainer_assignments (
    trainer_id  BIGINT NOT NULL REFERENCES trainers(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    PRIMARY KEY (trainer_id, customer_id)
);

CREATE TABLE IF NOT EXISTS classes (
    id               BIGSERIAL    PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    class_type       VARCHAR(50),
    duration_minutes INTEGER,
    max_capacity     INTEGER,
    is_active        BOOLEAN      DEFAULT TRUE,
    created_at       TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS class_sessions (
    id               BIGSERIAL    PRIMARY KEY,
    class_id         BIGINT       NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    trainer_id       BIGINT       REFERENCES trainers(id) ON DELETE SET NULL,
    start_time       TIMESTAMP    NOT NULL,
    end_time         TIMESTAMP    NOT NULL,
    max_capacity     INTEGER,
    current_bookings INTEGER      DEFAULT 0,
    status           VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
    cancel_reason    TEXT,
    created_at       TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pt_packages (
    id                 BIGSERIAL   PRIMARY KEY,
    customer_id        BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    trainer_id         BIGINT      REFERENCES trainers(id) ON DELETE SET NULL,
    total_sessions     INTEGER     NOT NULL,
    remaining_sessions INTEGER     NOT NULL,
    status             VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    start_date         DATE,
    end_date           DATE,
    version            BIGINT      DEFAULT 0,
    created_at         TIMESTAMP   DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pt_sessions (
    id             BIGSERIAL    PRIMARY KEY,
    pt_package_id  BIGINT       REFERENCES pt_packages(id) ON DELETE SET NULL,
    customer_id    BIGINT       NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    trainer_id     BIGINT       NOT NULL REFERENCES trainers(id) ON DELETE CASCADE,
    start_time     TIMESTAMP    NOT NULL,
    end_time       TIMESTAMP    NOT NULL,
    status         VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
    completion_ref VARCHAR(255) UNIQUE,
    completed_at   TIMESTAMP,
    notes          TEXT,
    cancel_reason  TEXT,
    created_at     TIMESTAMP    DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_customers_email         ON customers(email);
CREATE INDEX IF NOT EXISTS idx_trainers_email          ON trainers(email);
CREATE INDEX IF NOT EXISTS idx_class_sessions_start    ON class_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_pt_packages_customer    ON pt_packages(customer_id);
CREATE INDEX IF NOT EXISTS idx_pt_sessions_customer    ON pt_sessions(customer_id);
CREATE INDEX IF NOT EXISTS idx_pt_sessions_completion  ON pt_sessions(completion_ref);

