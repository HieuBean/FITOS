-- V3.2: Classes & Class Sessions
-- Author: Member C (Operations & Classes)

CREATE TABLE IF NOT EXISTS classes (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    category         VARCHAR(50)  NOT NULL,
    default_capacity INT          NOT NULL DEFAULT 20 CHECK (default_capacity > 0),
    color_code       VARCHAR(20)  DEFAULT '#D7FF3F',
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed standard class types
INSERT INTO classes (name, category, default_capacity, color_code, description) VALUES
('HIIT & Strength',  'HIIT',    16, '#FF5733', 'High-intensity interval training for explosive calorie burn.'),
('Yoga Flow',        'Yoga',    12, '#33FF57', 'Mindful movement and breathwork to increase mobility.'),
('Cycling Power',    'Cycling', 15, '#3380FF', 'Rhythm-based indoor cycling endurance session.'),
('Box & Burn',       'Boxing',  14, '#FF33A8', 'Boxing conditioning with pad work and heavy bags.'),
('Pilates Core',     'Pilates', 10, '#A833FF', 'Low-impact core strengthening and posture alignment.'),
('Evening Run Club', 'Running', 25, '#FFD700', 'Group outdoor interval and tempo running.')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS class_sessions (
    id           BIGSERIAL PRIMARY KEY,
    class_id     BIGINT      NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    trainer_id   BIGINT      REFERENCES trainers(id) ON DELETE SET NULL,
    session_date DATE        NOT NULL,
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    room         VARCHAR(50) NOT NULL DEFAULT 'Studio 1',
    capacity     INT         NOT NULL CHECK (capacity > 0),
    booked_count INT         NOT NULL DEFAULT 0 CHECK (booked_count >= 0 AND booked_count <= capacity),
    status       VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED'
                   CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_session_times CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_class_sessions_date    ON class_sessions(session_date);
CREATE INDEX IF NOT EXISTS idx_class_sessions_trainer ON class_sessions(trainer_id);
CREATE INDEX IF NOT EXISTS idx_class_sessions_class   ON class_sessions(class_id);
CREATE INDEX IF NOT EXISTS idx_class_sessions_status  ON class_sessions(status);
