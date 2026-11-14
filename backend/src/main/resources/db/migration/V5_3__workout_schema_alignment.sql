-- V5.3: Chuẩn hoá schema module workout về đúng tên cột mà JPA entity đang map.
--
-- Trước đây tồn tại HAI bộ tên cột song song:
--   * Flyway (V4_2/V4_3) tạo: plan_id, start_time, end_time, duration_minutes,
--     session_id, reps_completed, title, target_sets, target_reps_min, ...
--   * Entity JPA lại map: workout_plan_id, started_at, completed_at, session_id,
--     reps_actual, name, sets, reps, rest_secs, ...
-- Với `spring.jpa.hibernate.ddl-auto=validate`, Hibernate dừng khởi động vì thiếu cột.
-- Migration này đổi tên cột theo entity (nguồn ghi chính qua ORM) và bổ sung các cột
-- mà schema cũ chưa có (trạng thái buổi tập, calo, thời lượng).

-- ---------------------------------------------------------------------------
-- workout_plans: title -> name
-- ---------------------------------------------------------------------------
ALTER TABLE workout_plans RENAME COLUMN title TO name;

-- ---------------------------------------------------------------------------
-- workout_plan_exercises
-- ---------------------------------------------------------------------------
ALTER TABLE workout_plan_exercises RENAME COLUMN plan_id           TO workout_plan_id;
ALTER TABLE workout_plan_exercises RENAME COLUMN target_sets       TO sets;
ALTER TABLE workout_plan_exercises RENAME COLUMN target_reps_min   TO reps;
ALTER TABLE workout_plan_exercises RENAME COLUMN rest_seconds      TO rest_secs;
ALTER TABLE workout_plan_exercises RENAME COLUMN target_weight_kg  TO weight_suggest;
ALTER TABLE workout_plan_exercises RENAME COLUMN order_index       TO sort_order;
ALTER TABLE workout_plan_exercises ADD COLUMN IF NOT EXISTS duration_secs INT;

-- ---------------------------------------------------------------------------
-- workout_sessions
-- ---------------------------------------------------------------------------
ALTER TABLE workout_sessions RENAME COLUMN plan_id    TO workout_plan_id;
ALTER TABLE workout_sessions RENAME COLUMN start_time TO started_at;
ALTER TABLE workout_sessions RENAME COLUMN end_time   TO completed_at;

ALTER TABLE workout_sessions
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    ADD COLUMN IF NOT EXISTS total_duration_secs INT,
    ADD COLUMN IF NOT EXISTS calories_burned INT;

-- LocalDateTime của entity không mang múi giờ -> đổi sang timestamp không TZ.
ALTER TABLE workout_sessions
    ALTER COLUMN started_at   TYPE TIMESTAMP USING started_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN completed_at TYPE TIMESTAMP USING completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN created_at   TYPE TIMESTAMP USING created_at AT TIME ZONE 'Asia/Ho_Chi_Minh';

ALTER TABLE workout_sessions
    DROP CONSTRAINT IF EXISTS chk_ws_time_range;
ALTER TABLE workout_sessions
    ADD CONSTRAINT chk_ws_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

CREATE INDEX IF NOT EXISTS idx_ws_customer_status ON workout_sessions (studio_id, customer_id, status);

-- ---------------------------------------------------------------------------
-- workout_logs
-- ---------------------------------------------------------------------------
ALTER TABLE workout_logs RENAME COLUMN session_id     TO workout_session_id;
ALTER TABLE workout_logs RENAME COLUMN reps_completed TO reps_actual;
ALTER TABLE workout_logs RENAME COLUMN created_at     TO logged_at;
ALTER TABLE workout_logs ADD COLUMN IF NOT EXISTS duration_secs INT;

-- Entity WorkoutLog.rpe là Integer, schema cũ là NUMERIC(3,1) -> ép về INT.
ALTER TABLE workout_logs
    ALTER COLUMN rpe TYPE INT USING CASE WHEN rpe IS NULL THEN NULL ELSE ROUND(rpe)::INT END;

ALTER TABLE workout_logs
    ALTER COLUMN logged_at TYPE TIMESTAMP USING logged_at AT TIME ZONE 'Asia/Ho_Chi_Minh';

CREATE INDEX IF NOT EXISTS idx_wl_session_set ON workout_logs (workout_session_id, set_number);

-- ---------------------------------------------------------------------------
-- day_of_week: entity khai báo Integer, schema cũ là SMALLINT.
-- ---------------------------------------------------------------------------
ALTER TABLE workout_plan_exercises
    ALTER COLUMN day_of_week TYPE INT USING day_of_week::INT;

-- ---------------------------------------------------------------------------
-- Các entity dùng java.time.LocalDateTime (không mang múi giờ) nhưng schema cũ
-- tạo TIMESTAMP WITH TIME ZONE. Hibernate validate từ chối, nên đổi về timestamp.
-- Instant (có múi giờ) thì giữ nguyên, ví dụ users.created_at.
-- ---------------------------------------------------------------------------
ALTER TABLE ai_conversations
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'Asia/Ho_Chi_Minh';

ALTER TABLE ai_messages
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'Asia/Ho_Chi_Minh';

ALTER TABLE attendance
    ALTER COLUMN checkin_time TYPE TIMESTAMP USING checkin_time AT TIME ZONE 'Asia/Ho_Chi_Minh';

ALTER TABLE bookings
    ALTER COLUMN booked_at    TYPE TIMESTAMP USING booked_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN cancelled_at TYPE TIMESTAMP USING cancelled_at AT TIME ZONE 'Asia/Ho_Chi_Minh';

ALTER TABLE pt_packages
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'Asia/Ho_Chi_Minh';

ALTER TABLE pt_sessions
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'Asia/Ho_Chi_Minh';

ALTER TABLE workout_plans
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'Asia/Ho_Chi_Minh';
