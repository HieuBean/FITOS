-- ============================================================
-- V4.2__workout_logs.sql
-- Layer 1 - Task #14 (Người D)
-- Bảng ghi nhật ký tập: workout_sessions + workout_logs + body_metrics
-- ============================================================

-- Mỗi lần ra phòng gym = 1 workout_session
CREATE TABLE IF NOT EXISTS workout_sessions (
    id              BIGSERIAL   PRIMARY KEY,
    customer_id     BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    workout_plan_id BIGINT      REFERENCES workout_plans(id) ON DELETE SET NULL,
    started_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    -- IN_PROGRESS | COMPLETED | CANCELLED
    notes           TEXT,
    total_duration_secs INTEGER,
    -- tổng thời gian buổi tập (tính khi complete)
    calories_burned INTEGER
    -- ước tính calories (có thể null)
);

-- Chi tiết từng set trong buổi tập
CREATE TABLE IF NOT EXISTS workout_logs (
    id                  BIGSERIAL      PRIMARY KEY,
    workout_session_id  BIGINT         NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id         BIGINT         NOT NULL REFERENCES exercises(id),
    set_number          INTEGER        NOT NULL CHECK (set_number > 0),
    reps_actual         INTEGER        CHECK (reps_actual >= 0),
    weight_kg           NUMERIC(6,2)   CHECK (weight_kg >= 0),
    duration_secs       INTEGER        CHECK (duration_secs >= 0),
    -- dùng cho bài tập cardio / plank
    rpe                 INTEGER        CHECK (rpe BETWEEN 1 AND 10),
    -- Rate of Perceived Exertion: mức độ mệt 1-10
    notes               TEXT,
    logged_at           TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Chỉ số cơ thể theo thời gian
CREATE TABLE IF NOT EXISTS body_metrics (
    id              BIGSERIAL    PRIMARY KEY,
    customer_id     BIGINT       NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    weight_kg       NUMERIC(5,2) CHECK (weight_kg > 0),
    height_cm       NUMERIC(5,2) CHECK (height_cm > 0),
    body_fat_pct    NUMERIC(4,2) CHECK (body_fat_pct BETWEEN 0 AND 100),
    muscle_mass_kg  NUMERIC(5,2) CHECK (muscle_mass_kg >= 0),
    bmi             NUMERIC(4,2),
    -- tự động tính: weight / (height/100)^2
    waist_cm        NUMERIC(5,2),
    chest_cm        NUMERIC(5,2),
    recorded_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    notes           TEXT
);

CREATE INDEX IF NOT EXISTS idx_workout_sessions_customer   ON workout_sessions(customer_id);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_plan       ON workout_sessions(workout_plan_id);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_status     ON workout_sessions(status);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_started    ON workout_sessions(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_workout_logs_session        ON workout_logs(workout_session_id);
CREATE INDEX IF NOT EXISTS idx_workout_logs_exercise       ON workout_logs(exercise_id);
CREATE INDEX IF NOT EXISTS idx_body_metrics_customer_time  ON body_metrics(customer_id, recorded_at DESC);
