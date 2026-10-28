import pytest
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from schemas import FitnessProfile, BodyMetrics
from services.fitness_context_loader import FitnessContextLoader


def test_calculate_bmi():
    # 70 kg, 175 cm => 70 / (1.75^2) = 22.857 -> 22.9
    bmi = FitnessContextLoader.calculate_bmi(weight_kg=70.0, height_cm=175.0)
    assert bmi == 22.9

    # 85 kg, 170 cm => 85 / (1.7^2) = 29.41 -> 29.4
    bmi_overweight = FitnessContextLoader.calculate_bmi(weight_kg=85.0, height_cm=170.0)
    assert bmi_overweight == 29.4

    # Dữ liệu thiếu sót / lỗi
    assert FitnessContextLoader.calculate_bmi(0, 170) is None
    assert FitnessContextLoader.calculate_bmi(70, 0) is None


def test_biomechanical_constraints_back_injury():
    injuries = ["Thoát vị đĩa đệm nhẹ đốt L4-L5", "Hay mỏi thắt lưng"]
    constraints = FitnessContextLoader.derive_biomechanical_constraints(injuries)
    
    assert len(constraints) >= 1
    combined_text = " ".join(constraints)
    assert "Tránh dồn tải trục dọc cột sống" in combined_text
    assert "Goblet Squat" in combined_text
    assert "Chest-Supported Row" in combined_text


def test_biomechanical_constraints_knee_injury():
    injuries = ["Đau nhức khớp gối phải khi gập sâu"]
    constraints = FitnessContextLoader.derive_biomechanical_constraints(injuries)
    
    combined_text = " ".join(constraints)
    assert "xương bánh chè" in combined_text
    assert "Romanian Deadlift" in combined_text
    assert "Box Squat" in combined_text


def test_biomechanical_constraints_shoulder_injury():
    injuries = ["Viêm chóp xoay vai trái"]
    constraints = FitnessContextLoader.derive_biomechanical_constraints(injuries)
    
    combined_text = " ".join(constraints)
    assert "Overhead Press" in combined_text
    assert "Neutral-Grip Dumbbell Press" in combined_text


def test_biomechanical_constraints_healthy_user():
    constraints = FitnessContextLoader.derive_biomechanical_constraints([])
    assert len(constraints) == 1
    assert "Chưa có thông tin chấn thương" in constraints[0]
    assert "cần hỏi hội viên" in constraints[0]


def test_load_context_structure():
    profile = FitnessProfile(
        user_id=101,
        full_name="Nguyễn Văn A",
        age=28,
        gender="MALE",
        fitness_goal="MUSCLE_GAIN",
        fitness_level="INTERMEDIATE",
        fatigue_level=4,
        injuries=["Đau khớp gối"],
        body_metrics=BodyMetrics(weight_kg=72.0, height_cm=174.0, body_fat_pct=16.5)
    )

    ctx = FitnessContextLoader.load_context(profile)
    assert ctx["user_id"] == 101
    assert ctx["full_name"] == "Nguyễn Văn A"
    assert ctx["fitness_goal"] == "Tăng khối lượng cơ bắp (Hypertrophy)"
    assert ctx["bmi"] == 23.8
    assert ctx["body_fat_pct"] == 16.5
    assert ctx["fatigue_level"] == 4
    assert len(ctx["biomechanical_constraints"]) > 0


def test_build_system_prompt_injection():
    profile = FitnessProfile(
        user_id=202,
        full_name="Trần Thị Lan",
        age=25,
        gender="FEMALE",
        fitness_goal="WEIGHT_LOSS",
        fitness_level="BEGINNER",
        fatigue_level=8,
        injuries=["Mỏi lưng"],
        body_metrics=BodyMetrics(weight_kg=58.0, height_cm=162.0)
    )

    prompt = FitnessContextLoader.build_system_prompt(profile)
    
    assert "Trần Thị Lan" in prompt
    assert "Giảm mỡ & Săn chắc cơ thể" in prompt
    assert "8/10" in prompt  # fatigue level
    assert "Tránh dồn tải trục dọc cột sống" in prompt
    assert "Clean Fitness" in prompt


def test_recent_sets_and_reps_are_injected_without_guessing():
    profile = FitnessProfile(recent_workouts=[{
        "session_date": "2026-10-27",
        "exercise_name": "Goblet Squat",
        "set_number": 2,
        "reps_completed": 10,
        "weight_kg": 16,
    }])
    prompt = FitnessContextLoader.build_system_prompt(profile)
    assert "Goblet Squat" in prompt
    assert "hiệp 2, 10 lần, 16 kg" in prompt
    assert "Chỉ số BMI: Chưa cung cấp" in prompt
