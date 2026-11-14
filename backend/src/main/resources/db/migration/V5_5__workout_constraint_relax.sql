-- V5.5: Nới các ràng buộc còn sót lại sau khi đổi tên cột ở V5.3.
--
-- V5.3 đổi `order_index` -> `sort_order` nhưng ràng buộc cũ vẫn giữ điều kiện
-- `> 0`, trong khi entity WorkoutPlanExercise mặc định `sortOrder = 0` (đánh số
-- thứ tự từ 0). Hệ quả: mọi lần lưu giáo án đều vi phạm
-- workout_plan_exercises_order_index_check và trả 500.
--
-- Đồng thời `chk_wpe_reps_range` so sánh `target_reps_max` (cột không còn được
-- entity sử dụng) với `reps`, gây chặn nhầm khi người dùng đặt reps lớn hơn giá
-- trị mặc định 12 của cột cũ.

ALTER TABLE workout_plan_exercises
    DROP CONSTRAINT IF EXISTS workout_plan_exercises_order_index_check;

ALTER TABLE workout_plan_exercises
    ADD CONSTRAINT workout_plan_exercises_sort_order_check CHECK (sort_order >= 0);

ALTER TABLE workout_plan_exercises
    DROP CONSTRAINT IF EXISTS chk_wpe_reps_range;
