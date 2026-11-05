from typing import Dict, Any, List
from schemas import FitnessProfile

class FitnessContextLoader:
    """
    Nạp ngữ cảnh thể lực chuyên sâu (Fitness Context Loader) từ cơ sở dữ liệu FITOS.
    Phân tích chỉ số sinh học, phân loại giới hạn chấn thương và sinh System Prompt cá nhân hóa.
    """

    @staticmethod
    def calculate_bmi(weight_kg: float, height_cm: float) -> float | None:
        if not weight_kg or not height_cm or height_cm <= 0:
            return None
        height_m = height_cm / 100.0
        return round(weight_kg / (height_m * height_m), 1)

    @staticmethod
    def derive_biomechanical_constraints(injuries: List[str]) -> List[str]:
        constraints = []
        if not injuries:
            return ["Chưa có thông tin chấn thương; cần hỏi hội viên trước khi đề xuất bài tập nặng."]

        lower_injuries = [inj.lower() for inj in injuries]

        for inj in lower_injuries:
            if any(k in inj for k in ["lưng", "thắt lưng", "thoát vị", "cột sống"]):
                constraints.append(
                    "❌ Tránh dồn tải trục dọc cột sống nặng (Axial Loading: Barbell Back Squat nặng, Deadlift truyền thống). "
                    "Khuyến nghị thay bằng: Goblet Squat, Chest-Supported Row, Bulgarian Split Squat."
                )
            if any(k in inj for k in ["gối", "khớp gối", "dây chằng", "sụn chêm", "bánh chè"]):
                constraints.append(
                    "❌ Hạn chế góc gập gối quá 90 độ dồn lực thẳng lên xương bánh chè. "
                    "Khuyến nghị thay bằng: Romanian Deadlift (RDL), Hip Thrust, Box Squat, Leg Curl nằm."
                )
            if any(k in inj for k in ["vai", "khớp vai", "chóp xoay", "cổ tay"]):
                constraints.append(
                    "❌ Tránh các bài đẩy tạ qua đầu (Overhead Press) góc mở rộng và Bench Press cùi chỏ dang ngang 90 độ. "
                    "Khuyến nghị thay bằng: Neutral-Grip Dumbbell Press (cầm song song), Cable Face Pull, Incline Press 30 độ."
                )
            if any(k in inj for k in ["cổ chân", "mắt cá"]):
                constraints.append(
                    "❌ Tránh các bài nhảy bật cường độ cao (Plyometrics, Box Jump). "
                    "Khuyến nghị thay bằng bài tập tĩnh hoặc đạp xe kháng lực thấp."
                )

        if not constraints:
            constraints.append(f"Lưu ý các vùng tổn thương nhẹ: {', '.join(injuries)}. Cần khởi động kỹ tối thiểu 10 phút trước buổi tập.")

        return constraints

    @classmethod
    def load_context(cls, profile: FitnessProfile) -> Dict[str, Any]:
        """Tạo từ điển ngữ cảnh đầy đủ để lưu trữ hoặc inject vào metadata."""
        weight = profile.body_metrics.weight_kg if profile.body_metrics else None
        height = profile.body_metrics.height_cm if profile.body_metrics else None
        fat_pct = profile.body_metrics.body_fat_pct if profile.body_metrics and profile.body_metrics.body_fat_pct else None

        bmi = cls.calculate_bmi(weight, height)
        constraints = cls.derive_biomechanical_constraints(profile.injuries)

        goal_names = {
            "WEIGHT_LOSS": "Giảm mỡ & Săn chắc cơ thể",
            "MUSCLE_GAIN": "Tăng khối lượng cơ bắp (Hypertrophy)",
            "STRENGTH": "Phát triển sức mạnh tối đa (Max Strength)",
            "GENERAL_FITNESS": "Thể lực toàn diện & Sức bền tim mạch",
            "MOBILITY": "Tăng cường độ linh hoạt & Dẻo dai khớp"
        }

        return {
            "user_id": profile.user_id,
            "full_name": profile.full_name,
            "age": profile.age,
            "gender": profile.gender,
            "fitness_goal": goal_names.get(profile.fitness_goal, profile.fitness_goal),
            "fitness_level": profile.fitness_level,
            "weight_kg": weight,
            "height_cm": height,
            "body_fat_pct": fat_pct,
            "bmi": bmi,
            "injuries": profile.injuries,
            "biomechanical_constraints": constraints,
            "fatigue_level": profile.fatigue_level,
            "recent_workouts_count": len(profile.recent_workouts) if profile.recent_workouts else 0,
            "recent_workouts": (profile.recent_workouts or [])[:20],
        }

    @classmethod
    def build_system_prompt(cls, profile: FitnessProfile) -> str:
        """Sinh System Prompt chuyên sâu kết hợp Context Injection."""
        ctx = cls.load_context(profile)

        fat_str = f"{ctx['body_fat_pct']}%" if ctx['body_fat_pct'] else "Chưa có chỉ số"
        constraints_str = "\n  - " + "\n  - ".join(ctx['biomechanical_constraints'])
        workout_lines = []
        for entry in ctx["recent_workouts"][:10]:
            workout_lines.append(
                f"{entry.get('session_date', '?')}: {entry.get('exercise_name', 'Bài tập')} "
                f"hiệp {entry.get('set_number', '?')}, {entry.get('reps_completed', '?')} lần, "
                f"{entry.get('weight_kg', '?')} kg"
            )
        workouts_str = "; ".join(workout_lines) if workout_lines else "Chưa có nhật ký sets/reps"
        def known(value: Any, unit: str = "") -> str:
            return f"{value}{unit}" if value is not None else "Chưa cung cấp"

        system_prompt = f"""Bạn là FITOS AI Workout Coach — Huấn luyện viên thể hình & Chuyên gia khoa học vận động cá nhân tại Hệ sinh thái Phòng tập FITOS Studio.

THÔNG TIN THỂ LỰC VÀ NGỮ CẢNH CỦA HỘI VIÊN HIỆN TẠI (CONTEXT INJECTION):
- Họ tên: {known(ctx['full_name'])} | Độ tuổi: {known(ctx['age'], ' tuổi')} | Giới tính: {known(ctx['gender'])}
- Thể hình: Chiều cao {known(ctx['height_cm'], ' cm')} | Cân nặng {known(ctx['weight_kg'], ' kg')} | Chỉ số BMI: {known(ctx['bmi'])} | Mỡ cơ thể: {fat_str}
- Mục tiêu tập luyện: {known(ctx['fitness_goal'])}
- Trình độ tập luyện: {known(ctx['fitness_level'])}
- Mức độ mệt mỏi hiện tại: {known(ctx['fatigue_level'], '/10')}
- Nhật ký sets/reps gần đây: {workouts_str}
- Giới hạn chấn thương & Ràng buộc chuyển động sinh học:{constraints_str}

QUY TẮC CỐT LÕI KHI TRẢ LỜI:
1. LUÔN LUÔN bảo vệ các khớp bị chấn thương của hội viên dựa trên các ràng buộc ở trên. Tuyệt đối không gợi ý bài tập làm nặng thêm chấn thương.
2. Trả lời bằng tiếng Việt tự nhiên, nhiệt tình, chuyên môn cao nhưng dễ hiểu, mang tính động viên (chuẩn ngôn phong Huấn luyện viên chuyên nghiệp).
3. Đưa ra thông số cụ thể: Tên bài tập, Số hiệp (Sets), Số lần lặp (Reps), Thời gian nghỉ (Rest) và Mẹo kỹ thuật cảm nhận cơ (Mind-Muscle Connection).
4. Khuyến khích nghỉ ngơi và giãn cơ nếu mức độ mệt mỏi >= 7/10.
5. Tuyệt đối tuân thủ chuẩn Clean Fitness: Không bao giờ tư vấn doping hay chất cấm y tế.
"""
        return system_prompt.strip()
