-- V3.1: Trainers & Trainer Assignments
-- Author: Member C (Operations & Trainers)

CREATE TABLE IF NOT EXISTS trainers (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    full_name        VARCHAR(100)  NOT NULL,
    phone            VARCHAR(20)   NOT NULL,
    email            VARCHAR(100),
    specialization   VARCHAR(150),
    bio              TEXT,
    experience_years INT           DEFAULT 1 CHECK (experience_years >= 0),
    rating           NUMERIC(3, 2) DEFAULT 5.00 CHECK (rating >= 1.00 AND rating <= 5.00),
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_trainers_user_id   ON trainers(user_id);
CREATE INDEX IF NOT EXISTS idx_trainers_is_active ON trainers(is_active);

-- Seed standard coaches
INSERT INTO trainers (full_name, phone, email, specialization, experience_years, rating) VALUES
('Sarah Mitchell', '0911223344', 'sarah.mitchell@fitos.com', 'Head of HIIT & Conditioning', 8, 4.95),
('Marcus Chen',    '0922334455', 'marcus.chen@fitos.com',    'Cycling & Endurance Coach',    5, 4.88),
('Emma Walsh',     '0933445566', 'emma.walsh@fitos.com',     'Yoga & Mobility Specialist',   6, 4.92),
('Leo Torres',     '0944556677', 'leo.torres@fitos.com',     'Boxing & Combat Fitness',      4, 4.85)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS trainer_assignments (
    id            BIGSERIAL PRIMARY KEY,
    trainer_id    BIGINT      NOT NULL REFERENCES trainers(id) ON DELETE CASCADE,
    customer_id   BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    status        VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'COMPLETED', 'TERMINATED')),
    assigned_date DATE        NOT NULL DEFAULT CURRENT_DATE,
    notes         TEXT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_trainer_customer_active UNIQUE (trainer_id, customer_id, status)
);

CREATE INDEX IF NOT EXISTS idx_trainer_assignments_trainer  ON trainer_assignments(trainer_id);
CREATE INDEX IF NOT EXISTS idx_trainer_assignments_customer ON trainer_assignments(customer_id);
