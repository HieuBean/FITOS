import pytest
import sys
import os

# Đảm bảo import được module từ thư mục gốc ai-workout
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from safety_filter import check_safety, remove_accents


def test_remove_accents():
    assert remove_accents("Thể dục thể thao") == "the duc the thao"
    assert remove_accents("Đầu gối đau nhức") == "dau goi dau nhuc"
    assert remove_accents("Thoát vị đĩa đệm") == "thoat vi dia dem"


def test_banned_substances_detected():
    # Kiểm tra chặn các chất cấm phổ biến
    bad_prompts = [
        ("Em muốn dùng tren để siết cơ nhanh", "tren"),
        ("Có nên uống anavar trước khi tập không?", "anavar"),
        ("Tư vấn dùng dbol và dianabol", "dbol"),
        ("Sử dụng sarm rad140 có an toàn không?", "sarm"),
        ("Anh có bán thuốc tiêm steroid không?", "tiem steroid"),
        ("Có nên chích steroid tăng cơ cấp tốc?", "chich steroid"),
        ("Thuốc tăng cơ clenbuterol đốt mỡ", "clenbuterol"),
    ]

    for prompt, expected_term in bad_prompts:
        result = check_safety(prompt)
        assert not result.is_safe, f"Failed to block: {prompt}"
        assert result.violation_type == "BANNED_SUBSTANCE"
        assert any(expected_term in term for term in result.flagged_terms)
        assert "⚠️ **CẢNH BÁO AN TOÀN Y TẾ FITOS**" in result.warning_message


def test_medical_contraindications_detected():
    # Kiểm tra chặn các triệu chứng cấp cứu hoặc bệnh lý nguy cấp
    medical_prompts = [
        ("Tôi bị nhồi máu cơ tim có tập ngực được không?", "nhoi mau co tim"),
        ("Vừa bị đau thắt ngực khi đẩy tạ nặng", "dau that nguc"),
        ("Mới bị gãy xương bàn chân có chạy bộ được không?", "gay xuong"),
        ("Bác sĩ bảo vỡ sụn chêm", "vo sun"),
        ("Thoát vị đĩa đệm chèn ép tủy sống", "thoat vi dia dem chen ep tuy"),
    ]

    for prompt, expected_term in medical_prompts:
        result = check_safety(prompt)
        assert not result.is_safe, f"Failed to block medical risk: {prompt}"
        assert result.violation_type == "MEDICAL_CONTRAINDICATION"
        assert expected_term in result.flagged_terms
        assert "🚨 **KHUYẾN CÁO Y KHOA KHẨN CẤP**" in result.warning_message


def test_unsafe_practices_detected():
    # Kiểm tra chặn các kỹ thuật nguy hại
    unsafe_prompts = [
        "Khi tập leg press có nên khóa khớp gối không?",
        "Phương pháp nhịn uống nước cắt nước cực đoan để lên cơ nét",
    ]

    for prompt in unsafe_prompts:
        result = check_safety(prompt)
        assert not result.is_safe, f"Failed to block unsafe habit: {prompt}"
        assert result.violation_type == "UNSAFE_EXERCISE"
        assert "⚠️ **CẢNH BÁO KỸ THUẬT NGUY HIỂM**" in result.warning_message


def test_safe_queries_allowed():
    # Kiểm tra các câu hỏi thể hình lành mạnh phải được thông qua 100%
    safe_prompts = [
        "Hôm nay tôi muốn tập ngực và tay sau, gợi ý giáo án giúp tôi.",
        "Làm sao để squat đúng kỹ thuật không bị đau lưng?",
        "Chỉ số BMI của tôi là 23, nên tăng cơ hay giảm mỡ trước?",
        "Tôi bị mỏi cơ sau buổi tập chân hôm qua, có nên đi bộ nhẹ không?",
        "Tập deadlift với tạ ấm 16kg thì cần chú ý điều gì?",
        "Thực đơn giàu protein tự nhiên từ ức gà và trứng",
    ]

    for prompt in safe_prompts:
        result = check_safety(prompt)
        assert result.is_safe is True, f"Legitimate query was wrongly blocked: {prompt}"
        assert result.violation_type is None
        assert result.warning_message is None


def test_empty_message():
    assert check_safety("").is_safe is True
    assert check_safety("   ").is_safe is True
