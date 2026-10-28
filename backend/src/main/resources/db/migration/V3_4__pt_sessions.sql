-- V3.4: PT Sessions & Scheduling
-- Author: Member C (Operations & PT Sessions)

CREATE TABLE IF NOT EXISTS pt_sessions (
    id              BIGSERIAL PRIMARY KEY,
    trainer_id      BIGINT       NOT NULL REFERENCES trainers(id) ON DELETE CASCADE,
    customer_id     BIGINT       NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    pt_package_id   BIGINT       REFERENCES pt_packages(id) ON DELETE SET NULL,
    session_date    DATE         NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED'
                      CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    -- Spec 8.4 & Task 65: completion_ref duy nhất chống trừ trùng buổi tập (Idempotency)
    completion_ref  VARCHAR(100) UNIQUE,
    training_notes  TEXT,
    customer_rating SMALLINT     CHECK (customer_rating BETWEEN 1 AND 5),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pt_session_times CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_pt_sessions_trainer_date  ON pt_sessions(trainer_id, session_date);
CREATE INDEX IF NOT EXISTS idx_pt_sessions_customer_date ON pt_sessions(customer_id, session_date);
CREATE INDEX IF NOT EXISTS idx_pt_sessions_status        ON pt_sessions(status);
CREATE INDEX IF NOT EXISTS idx_pt_sessions_comp_ref      ON pt_sessions(completion_ref);
