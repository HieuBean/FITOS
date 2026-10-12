-- V4.3: Workout sessions, workout logs & body metrics
-- Author: Member D (Workout & AI)

CREATE TABLE IF NOT EXISTS workout_sessions (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    plan_id BIGINT,
    session_date DATE NOT NULL DEFAULT CURRENT_DATE,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_minutes INT,
    rating SMALLINT CHECK (rating BETWEEN 1 AND 5),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
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

CREATE TABLE IF NOT EXISTS workout_logs (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    set_number INT NOT NULL,
    weight_kg NUMERIC(6, 2) NOT NULL DEFAULT 0,
    reps_completed INT NOT NULL,
    rpe NUMERIC(3, 1) CHECK (rpe >= 1.0 AND rpe <= 10.0), -- Rate of Perceived Exertion
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wl_session FOREIGN KEY (session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_wl_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_wl_session ON workout_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_wl_exercise_weight ON workout_logs(exercise_id, weight_kg DESC);

CREATE TABLE IF NOT EXISTS body_metrics (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    recorded_date DATE NOT NULL DEFAULT CURRENT_DATE,
    weight_kg NUMERIC(5, 2) NOT NULL,
    height_cm NUMERIC(5, 2),
    body_fat_percent NUMERIC(4, 2),
    muscle_mass_kg NUMERIC(5, 2),
    notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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
