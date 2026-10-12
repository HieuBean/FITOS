-- V4.2: Workout plans & workout plan exercises
-- Author: Member D (Workout & AI)

CREATE TABLE IF NOT EXISTS workout_plans (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    trainer_id BIGINT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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
CREATE INDEX IF NOT EXISTS idx_workout_plans_status ON workout_plans(status);

CREATE TABLE IF NOT EXISTS workout_plan_exercises (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1: Mon, 2: Tue, ..., 7: Sun
    order_index INT NOT NULL DEFAULT 1,
    target_sets INT NOT NULL DEFAULT 3,
    target_reps VARCHAR(50) NOT NULL DEFAULT '8-12',
    target_weight_kg NUMERIC(6, 2),
    notes VARCHAR(255),
    CONSTRAINT fk_wpe_plan FOREIGN KEY (plan_id) REFERENCES workout_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_wpe_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_wpe_plan_day ON workout_plan_exercises(plan_id, day_of_week);
