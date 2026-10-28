-- V4.3: Workout sessions, workout logs & body metrics
-- Author: Member D (Workout & AI)

CREATE TABLE IF NOT EXISTS workout_sessions (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT   NOT NULL,
    plan_id          BIGINT,                           -- NULL = tập tự do, không theo plan
    session_date     DATE     NOT NULL DEFAULT CURRENT_DATE,
    start_time       TIMESTAMP WITH TIME ZONE,
    end_time         TIMESTAMP WITH TIME ZONE,
    -- duration_minutes: tính thủ công khi complete session (không dùng GENERATED vì
    --   start/end_time có thể NULL lúc tạo, chỉ có giá trị khi complete)
    duration_minutes INT      CHECK (duration_minutes >= 0),
    rating           SMALLINT CHECK (rating BETWEEN 1 AND 5),
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    -- end_time phải sau start_time nếu cả hai đã có giá trị
    CONSTRAINT chk_ws_time_range CHECK (end_time IS NULL OR start_time IS NULL OR end_time > start_time),
    CONSTRAINT fk_ws_plan FOREIGN KEY (plan_id) REFERENCES workout_plans(id) ON DELETE SET NULL
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_ws_customer') THEN
            ALTER TABLE workout_sessions ADD CONSTRAINT fk_ws_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ws_customer_date ON workout_sessions(customer_id, session_date DESC);

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS workout_logs (
    id               BIGSERIAL PRIMARY KEY,
    session_id       BIGINT        NOT NULL,
    exercise_id      BIGINT        NOT NULL,
    -- plan_exercise_id: link về prescription gốc → so sánh "thực tế vs mục tiêu"
    -- NULL khi tập tự do không theo plan
    plan_exercise_id BIGINT        DEFAULT NULL,
    set_number       INT           NOT NULL CHECK (set_number > 0),
    weight_kg        NUMERIC(6, 2) NOT NULL DEFAULT 0 CHECK (weight_kg >= 0),
    reps_completed   INT           NOT NULL CHECK (reps_completed > 0),
    rpe              NUMERIC(3, 1) CHECK (rpe >= 1.0 AND rpe <= 10.0), -- Rate of Perceived Exertion
    notes            VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wl_session       FOREIGN KEY (session_id)       REFERENCES workout_sessions(id)      ON DELETE CASCADE,
    CONSTRAINT fk_wl_exercise      FOREIGN KEY (exercise_id)      REFERENCES exercises(id)             ON DELETE RESTRICT,
    CONSTRAINT fk_wl_plan_exercise FOREIGN KEY (plan_exercise_id) REFERENCES workout_plan_exercises(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_wl_session          ON workout_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_wl_exercise_weight  ON workout_logs(exercise_id, weight_kg DESC);
-- Index hỗ trợ query Personal Records theo exercise
CREATE INDEX IF NOT EXISTS idx_wl_exercise_reps    ON workout_logs(exercise_id, reps_completed DESC);

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS body_metrics (
    id                BIGSERIAL PRIMARY KEY,
    customer_id       BIGINT        NOT NULL,
    recorded_date     DATE          NOT NULL DEFAULT CURRENT_DATE,
    weight_kg         NUMERIC(5, 2) NOT NULL CHECK (weight_kg > 0),
    height_cm         NUMERIC(5, 2)           CHECK (height_cm > 0),
    body_fat_percent  NUMERIC(4, 2)           CHECK (body_fat_percent BETWEEN 1.0 AND 70.0),
    muscle_mass_kg    NUMERIC(5, 2)           CHECK (muscle_mass_kg > 0),
    notes             VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    -- Mỗi ngày chỉ có 1 bản ghi / customer (upsert pattern)
    CONSTRAINT uq_body_metrics_date UNIQUE (customer_id, recorded_date)
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_bm_customer') THEN
            ALTER TABLE body_metrics ADD CONSTRAINT fk_bm_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bm_customer_date ON body_metrics(customer_id, recorded_date DESC);
