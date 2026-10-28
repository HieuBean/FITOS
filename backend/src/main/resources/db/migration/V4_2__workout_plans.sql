-- V4.2: Workout plans & workout plan exercises
-- Author: Member D (Workout & AI)

CREATE TABLE IF NOT EXISTS workout_plans (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    trainer_id  BIGINT,
    title       VARCHAR(150) NOT NULL,
    goal        VARCHAR(50)  DEFAULT 'GENERAL_FITNESS'
                  CHECK (goal IN ('MUSCLE_GAIN', 'WEIGHT_LOSS', 'STRENGTH', 'GENERAL_FITNESS', 'MOBILITY', 'OTHER')),
    description TEXT,
    start_date  DATE NOT NULL,
    end_date    DATE,
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'ARCHIVED')),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    -- Validate end_date không trước start_date
    CONSTRAINT chk_wp_date_range CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Foreign key constraints added if referenced tables exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_wp_customer') THEN
            ALTER TABLE workout_plans ADD CONSTRAINT fk_wp_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
        END IF;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trainers') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_wp_trainer') THEN
            ALTER TABLE workout_plans ADD CONSTRAINT fk_wp_trainer FOREIGN KEY (trainer_id) REFERENCES trainers(id) ON DELETE SET NULL;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_workout_plans_customer ON workout_plans(customer_id);
CREATE INDEX IF NOT EXISTS idx_workout_plans_status   ON workout_plans(status);

CREATE TABLE IF NOT EXISTS workout_plan_exercises (
    id               BIGSERIAL PRIMARY KEY,
    plan_id          BIGINT     NOT NULL,
    exercise_id      BIGINT     NOT NULL,
    -- 1=Mon, 2=Tue, ..., 7=Sun
    day_of_week      SMALLINT   NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    order_index      INT        NOT NULL DEFAULT 1 CHECK (order_index > 0),
    target_sets      INT        NOT NULL DEFAULT 3 CHECK (target_sets > 0),
    -- Tách target_reps thành min/max để dễ query và validate
    target_reps_min  INT        NOT NULL DEFAULT 8  CHECK (target_reps_min > 0),
    target_reps_max  INT        NOT NULL DEFAULT 12 CHECK (target_reps_max > 0),
    target_weight_kg NUMERIC(6, 2) CHECK (target_weight_kg >= 0),
    rest_seconds     INT        DEFAULT 60 CHECK (rest_seconds >= 0),
    notes            VARCHAR(255),
    CONSTRAINT chk_wpe_reps_range CHECK (target_reps_max >= target_reps_min),
    CONSTRAINT fk_wpe_plan     FOREIGN KEY (plan_id)     REFERENCES workout_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_wpe_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id)     ON DELETE RESTRICT,
    -- Ngăn duplicate cùng bài tập trong cùng ngày của cùng plan
    CONSTRAINT uq_wpe_plan_exercise_day UNIQUE (plan_id, exercise_id, day_of_week)
);

CREATE INDEX IF NOT EXISTS idx_wpe_plan_day ON workout_plan_exercises(plan_id, day_of_week);
