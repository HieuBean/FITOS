-- ============================================================
-- V4.1__workout_plans.sql
-- Layer 1 - Task #13 (Người D)
-- Bảng kế hoạch tập luyện: workout_plans + workout_plan_exercises
-- ============================================================

CREATE TABLE IF NOT EXISTS workout_plans (
    id          BIGSERIAL    PRIMARY KEY,
    customer_id BIGINT       NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    trainer_id  BIGINT       REFERENCES trainers(id) ON DELETE SET NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    goal        VARCHAR(50),
    -- WEIGHT_LOSS | MUSCLE_GAIN | ENDURANCE | FLEXIBILITY | GENERAL_FITNESS
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | ACTIVE | COMPLETED | CANCELLED
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Các bài tập trong kế hoạch, chia theo ngày trong tuần
CREATE TABLE IF NOT EXISTS workout_plan_exercises (
    id              BIGSERIAL PRIMARY KEY,
    workout_plan_id BIGINT    NOT NULL REFERENCES workout_plans(id) ON DELETE CASCADE,
    exercise_id     BIGINT    NOT NULL REFERENCES exercises(id),
    day_of_week     INTEGER   NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    -- 1=Thứ 2, 2=Thứ 3, ..., 7=Chủ Nhật
    sets            INTEGER   CHECK (sets > 0),
    reps            INTEGER   CHECK (reps > 0),
    duration_secs   INTEGER   CHECK (duration_secs > 0),
    -- dùng khi bài tập tính theo giây (plank, cardio...)
    rest_secs       INTEGER   NOT NULL DEFAULT 60,
    weight_suggest  NUMERIC(6,2),
    -- gợi ý kg, null = tự chọn
    notes           TEXT,
    sort_order      INTEGER   NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_workout_plans_customer ON workout_plans(customer_id);
CREATE INDEX IF NOT EXISTS idx_workout_plans_trainer  ON workout_plans(trainer_id);
CREATE INDEX IF NOT EXISTS idx_workout_plans_status   ON workout_plans(status);
CREATE INDEX IF NOT EXISTS idx_wpe_plan_day           ON workout_plan_exercises(workout_plan_id, day_of_week);
